/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.businessrules.ReleaseClearingStateSummaryComputer;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.ektorp.http.HttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.eclipse.sw360.datahandler.common.SW360Utils.*;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 * @author daniele.fognini@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */
public class ProjectDatabaseHandler extends AttachmentAwareDatabaseHandler {

    private static final Logger log = Logger.getLogger(ProjectDatabaseHandler.class);
    private static final int DELETION_SANITY_CHECK_THRESHOLD = 5;
    private static final String DUMMY_NEW_PROJECT_ID = "newproject";
    public static final int SVMML_JSON_LOG_CUTOFF_LENGTH = 3000;

    private final ProjectRepository repository;
    private final ProjectVulnerabilityRatingRepository pvrRepository;
    private final ProjectModerator moderator;
    private final AttachmentConnector attachmentConnector;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private static final User EMPTY_USER = new User().setId("").setEmail("").setExternalid("").setDepartment("").setLastname("").setGivenname("");

    private static final Pattern PLAUSIBLE_GID_REGEXP = Pattern.compile("^[zZ].{7}$");
    private final MailUtil mailUtil = new MailUtil();

    // this caching structure is only used for filling clearing state summaries and
    // should be avoided anywhere else so that no other outdated information will be
    // provided
    // for the clearing state it is
    // 1. necessary because they take a long time to get calculated which might be a
    // good reason for service clients to query not for all projects at once, but to
    // divide the queries in subsets of project (and we do not want to load the
    // whole project map for each of this subset queries)
    // 2. okay because the data normally "only moves forward", meaning that already
    // approved clearing reports will not be revoked. So worst case of caching is
    // that outdated data is provided, not wrong one (as it would be okay to see a
    // cleared project that is displayed as not yet cleared - but it won't be okay
    // to see a uncleared project that is displayed as cleared but isn't anymore)
    private static final java.time.Duration ALL_PROJECTS_ID_MAP_CACHE_LIFETIME = java.time.Duration.ofMinutes(2);
    private Map<String, Project> cachedAllProjectsIdMap;
    private Instant cachedAllProjectsIdMapLoadingInstant;

    public ProjectDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        this(httpClient, dbName, attachmentDbName, new ProjectModerator(),
                new ComponentDatabaseHandler(httpClient,dbName,attachmentDbName),
                new AttachmentDatabaseHandler(httpClient, dbName, attachmentDbName));
    }

    @VisibleForTesting
    public ProjectDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName, ProjectModerator moderator,
                                  ComponentDatabaseHandler componentDatabaseHandler,
                                  AttachmentDatabaseHandler attachmentDatabaseHandler) throws MalformedURLException {
        super(attachmentDatabaseHandler);
        DatabaseConnector db = new DatabaseConnector(httpClient, dbName);

        // Create the repositories
        repository = new ProjectRepository(db);
        pvrRepository = new ProjectVulnerabilityRatingRepository(db);

        // Create the moderator
        this.moderator = moderator;

        // Create the attachment connector
        attachmentConnector = new AttachmentConnector(httpClient, attachmentDbName, Duration.durationOf(30, TimeUnit.SECONDS));

        this.componentDatabaseHandler = componentDatabaseHandler;
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    public List<Project> getMyProjectsSummary(String user) {
        return repository.getMyProjectsSummary(user);
    }

    public List<Project> getBUProjectsSummary(String organisation) {
        return repository.getBUProjectsSummary(organisation);
    }

    public List<Project> getAccessibleProjectsSummary(User user) {
        return repository.getAccessibleProjectsSummary(user);
    }

    public List<Project> searchByName(String name, User user) {
        return repository.searchByName(name, user);
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    public Project getProjectById(String id, User user) throws SW360Exception {
        Project project = repository.get(id);
        assertNotNull(project);

        if(!makePermission(project, user).isActionAllowed(RequestedAction.READ)) {
            throw fail("User " + user + " is not allowed to view the requested project " + project + "!");
        }

        return project;
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////

    public AddDocumentRequestSummary addProject(Project project, User user) throws SW360Exception {
        // Prepare project for database
        prepareProject(project);
        if(isDuplicate(project)) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
        }

        // Save creating user
        project.createdBy = user.getEmail();
        project.createdOn = getCreatedOn();
        project.businessUnit = getBUFromOrganisation(user.getDepartment());

        // Add project to database and return ID
        repository.add(project);
        sendMailNotificationsForNewProject(project, user.getEmail());
        return new AddDocumentRequestSummary().setId(project.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    private boolean isDuplicate(Project project){
        List<Project> duplicates = repository.searchByNameAndVersion(project.getName(), project.getVersion());
        return duplicates.size()>0;
    }
    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    public RequestStatus updateProject(Project project, User user) throws SW360Exception {
        // Prepare project for database
        prepareProject(project);

        Project actual = repository.get(project.getId());

        assertNotNull(project);

        if (!changePassesSanityCheck(project, actual)){
            return RequestStatus.FAILED_SANITY_CHECK;
        } else if (makePermission(actual, user).isActionAllowed(RequestedAction.WRITE)) {
            copyImmutableFields(project,actual);
            project.setAttachments( getAllAttachmentsToKeep(toSource(actual), actual.getAttachments(), project.getAttachments()) );
            deleteAttachmentUsagesOfUnlinkedReleases(project, actual);
            repository.update(project);

            //clean up attachments in database
            attachmentConnector.deleteAttachmentDifference(actual.getAttachments(), project.getAttachments());
            sendMailNotificationsForProjectUpdate(project, user.getEmail());
            return RequestStatus.SUCCESS;
        } else {
            return moderator.updateProject(project, user);
        }
    }

    private void deleteAttachmentUsagesOfUnlinkedReleases(Project updated, Project actual) throws SW360Exception {
        Source usedBy = Source.projectId(updated.getId());
        Set<String> updatedLinkedReleaseIds = nullToEmptyMap(updated.getReleaseIdToUsage()).keySet();
        Set<String> actualLinkedReleaseIds = nullToEmptyMap(actual.getReleaseIdToUsage()).keySet();
        deleteAttachmentUsagesOfUnlinkedReleases(usedBy, updatedLinkedReleaseIds, actualLinkedReleaseIds);
    }

    private boolean changePassesSanityCheck(Project updated, Project current) {
        return !(nullToEmptyMap(current.getReleaseIdToUsage()).size() > DELETION_SANITY_CHECK_THRESHOLD &&
                nullToEmptyMap(updated.getReleaseIdToUsage()).size() == 0 ||
                nullToEmptyMap(current.getLinkedProjects()).size() > DELETION_SANITY_CHECK_THRESHOLD &&
                nullToEmptyMap(updated.getLinkedProjects()).size() == 0);
    }

    private void prepareProject(Project project) throws SW360Exception {
        // Prepare project for database
        ThriftValidate.prepareProject(project);

        //add sha1 to attachments if necessary
        if(project.isSetAttachments()) {
            attachmentConnector.setSha1ForAttachments(project.getAttachments());
        }
    }

    public RequestStatus updateProjectFromAdditionsAndDeletions(Project projectAdditions, Project projectDeletions, User user){

        try {
            Project project = getProjectById(projectAdditions.getId(), user);
            project = moderator.updateProjectFromModerationRequest(project, projectAdditions, projectDeletions);
            return updateProject(project, user);
        } catch (SW360Exception e) {
            log.error("Could not get original project when updating from moderation request.");
            return RequestStatus.FAILURE;
        }

    }

    private void copyImmutableFields(Project destination, Project source) {
        ThriftUtils.copyField(source, destination, Project._Fields.CREATED_ON);
        ThriftUtils.copyField(source, destination, Project._Fields.CREATED_BY);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////

    public RequestStatus deleteProject(String id, User user) throws SW360Exception {
        Project project = repository.get(id);
        assertNotNull(project);

        if (checkIfInUse(id)) {
            return RequestStatus.IN_USE;
        }

        // Remove the project if the user is allowed to do it by himself
        if (makePermission(project, user).isActionAllowed(RequestedAction.DELETE)) {
            removeProjectAndCleanUp(project);
            return RequestStatus.SUCCESS;
        } else {
            return moderator.deleteProject(project, user);
        }
    }

    public boolean checkIfInUse(String projectId) {
        final Set<Project> usingProjects = repository.searchByLinkingProjectId(projectId);
       return !usingProjects.isEmpty();
    }

    private void removeProjectAndCleanUp(Project project) throws SW360Exception {
        attachmentConnector.deleteAttachments(project.getAttachments());
        attachmentDatabaseHandler.deleteUsagesBy(Source.projectId(project.getId()));
        repository.remove(project);
        moderator.notifyModeratorOnDelete(project.getId());
    }

    //////////////////////
    // HELPER FUNCTIONS //
    //////////////////////

    public List<ProjectLink> getLinkedProjects(Project project, boolean deep, User user) {

        final Map<String, Project> dbProjectMap;
        if (deep){
            dbProjectMap = ThriftUtils.getIdMap(repository.getAll());
        } else {
            dbProjectMap = preloadLinkedProjects(project, user);
        }
        final Map<String, Project> projectMap;
        projectMap = project.isSetId() ? dbProjectMap : ImmutableMap.<String, Project>builder().putAll(dbProjectMap).put(DUMMY_NEW_PROJECT_ID, project).build();

        final Map<String, Release> releaseMap = preloadLinkedReleases(projectMap);

        Deque<String> visitedIds = new ArrayDeque<>();

        Map<String, ProjectRelationship> fakeRelations = new HashMap<>();
        fakeRelations.put(project.isSetId() ? project.getId() : DUMMY_NEW_PROJECT_ID, ProjectRelationship.UNKNOWN);
        List<ProjectLink> out = iterateProjectRelationShips(fakeRelations, null, visitedIds, projectMap, releaseMap, deep ? -1 : 2);
        return out;
    }

    private Map<String, Project> preloadLinkedProjects(Project project, User user) {
        List<String> projectIdsToLoad = new ArrayList<>(nullToEmptyMap(project.getLinkedProjects()).keySet());
        if (project.isSetId()) {
            projectIdsToLoad.add(project.getId());
        }
        return ThriftUtils.getIdMap(getProjectsById(projectIdsToLoad, user));
    }

    private Map<String, Release> preloadLinkedReleases(Map<String, Project> projectMap) {
        Set<String> releaseIdsToLoad = projectMap
                .values()
                .stream()
                .map(Project::getReleaseIdToUsage)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        return ThriftUtils.getIdMap(componentDatabaseHandler.getFullReleases(releaseIdsToLoad));
    }

    public List<ProjectLink> getLinkedProjects(Map<String, ProjectRelationship> relations) {
        List<ProjectLink> out;
        final Map<String, Project> projectMap = ThriftUtils.getIdMap(repository.getAll());
        final Map<String, Release> releaseMap = preloadLinkedReleases(projectMap);

        Deque<String> visitedIds = new ArrayDeque<>();
        out = iterateProjectRelationShips(relations, null, visitedIds, projectMap, releaseMap, -1);

        return out;
    }


    private List<ProjectLink> iterateProjectRelationShips(Map<String, ProjectRelationship> relations, String parentNodeId, Deque<String> visitedIds, Map<String, Project> projectMap, Map<String, Release> releaseMap, int maxDepth) {
        List<ProjectLink> out = new ArrayList<>();
        for (Map.Entry<String, ProjectRelationship> entry : relations.entrySet()) {
            Optional<ProjectLink> projectLinkOptional = createProjectLink(entry.getKey(), entry.getValue(), parentNodeId, visitedIds, projectMap, releaseMap, maxDepth);
            projectLinkOptional.ifPresent(out::add);
        }
        out.sort(Comparator.comparing(ProjectLink::getName).thenComparing(ProjectLink::getVersion));
        return out;
    }

    private Optional<ProjectLink> createProjectLink(String id, ProjectRelationship relationship, String parentNodeId, Deque<String> visitedIds, Map<String, Project> projectMap, Map<String, Release> releaseMap, int maxDepth) {
        ProjectLink projectLink = null;
        if (!visitedIds.contains(id) && (maxDepth < 0 || visitedIds.size() < maxDepth)) {
            visitedIds.push(id);
            Project project = projectMap.get(id);
            if (project != null) {
                projectLink = new ProjectLink(id, project.name);
                if (project.isSetReleaseIdToUsage() && (maxDepth < 0 || visitedIds.size() < maxDepth)){ // ProjectLink on the last level does not get children added
                    List<ReleaseLink> linkedReleases = componentDatabaseHandler.getLinkedReleases(project, releaseMap, visitedIds);
                    fillMainlineStates(linkedReleases, project.getReleaseIdToUsage());
                    projectLink.setLinkedReleases(nullToEmptyList(linkedReleases));
                }

                projectLink
                        .setNodeId(generateNodeId(id))
                        .setParentNodeId(parentNodeId)
                        .setRelation(relationship)
                        .setVersion(project.getVersion())
                        .setState(project.getState())
                        .setProjectType(project.getProjectType())
                        .setClearingState(project.getClearingState())
                        .setTreeLevel(visitedIds.size() - 1);
                if (project.isSetLinkedProjects()) {
                    List<ProjectLink> subprojectLinks = iterateProjectRelationShips(project.getLinkedProjects(),
                            projectLink.getNodeId(), visitedIds, projectMap, releaseMap, maxDepth);
                    projectLink.setSubprojects(subprojectLinks);
                }
            } else {
                log.error("Broken ProjectLink in project with id: " + parentNodeId + ". Linked project with id " + id + " was not in the project cache");
            }
            visitedIds.pop();
        }
        return Optional.ofNullable(projectLink);
    }

    private void fillMainlineStates(List<ReleaseLink> linkedReleases, Map<String, ProjectReleaseRelationship> releaseIdToUsage) {
        for (ReleaseLink releaseLink : linkedReleases) {
            releaseLink.setMainlineState(releaseIdToUsage.get(releaseLink.getId()).getMainlineState());
        }
    }

    private String generateNodeId(String id) {
        return id == null ? null : id + "_" + UUID.randomUUID();
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return repository.searchByReleaseId(id, user);
    }

    public Set<Project> searchByReleaseId(Set<String> ids, User user) {
        return repository.searchByReleaseId(ids, user);
    }

    public Set<Project> searchLinkingProjects(String id, User user) {
        return repository.searchByLinkingProjectId(id, user);
    }

    public Project getProjectForEdit(String id, User user) throws SW360Exception {

        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        Project project = getProjectById(id,user);
        DocumentState documentState;
        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();
                project = moderator.updateProjectFromModerationRequest(project,
                        moderationRequest.getProjectAdditions(),
                        moderationRequest.getProjectDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        project.setPermissions(makePermission(project, user).getPermissionMap());
        project.setDocumentState(documentState);
        return project;
    }

    public List<Project> getProjectsById(List<String> id, User user) {

        List<Project> projects = repository.makeSummaryFromFullDocs(SummaryType.SUMMARY, repository.get(id));

        List<Project> output = new ArrayList<>();
        for (Project project : projects) {
            if (makePermission(project, user).isActionAllowed(RequestedAction.READ)) {
                output.add(project);
            } else {
                log.error("User " + user.getEmail() + " requested not accessible project " + printName(project));
            }
        }

        return output;
    }

    public int getCountByReleaseIds(Set<String> ids) {
        return repository.getCountByReleaseIds(ids);
    }

    public int getCountByProjectId(String id) {
        return repository.getCountByProjectId(id);
    }

    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) {
        return repository.searchByExternalIds(externalIds, user);
    }

    public Set<Project> getAccessibleProjects(User user) {
        return repository.getAccessibleProjects(user);
    }

    public Map<String, List<String>> getDuplicateProjects() {
        ListMultimap<String, String> projectIdentifierToReleaseId = ArrayListMultimap.create();

        for (Project project : repository.getAll()) {
            projectIdentifierToReleaseId.put(SW360Utils.printName(project), project.getId());
        }

        return CommonUtils.getIdentifierToListOfDuplicates(projectIdentifierToReleaseId);
    }

    public List<ProjectVulnerabilityRating> getProjectVulnerabilityRatingByProjectId(String projectId){
        return pvrRepository.getProjectVulnerabilityRating(projectId);
    }

    public RequestStatus updateProjectVulnerabilityRating(ProjectVulnerabilityRating link) {
        if( ! link.isSetId()){
            link.setId(SW360Constants.PROJECT_VULNERABILITY_RATING_ID_PREFIX + link.getProjectId());
            pvrRepository.add(link);
        } else {
            pvrRepository.update(link);
        }
        return RequestStatus.SUCCESS;
    }

    public List<Project> fillClearingStateSummary(List<Project> projects, User user) {
        Function<Project, Set<String>> extractReleaseIds = project -> CommonUtils.nullToEmptyMap(project.getReleaseIdToUsage()).keySet();

        Set<String> allReleaseIds = projects.stream().map(extractReleaseIds).reduce(Sets.newHashSet(), Sets::union);
        if (!allReleaseIds.isEmpty()) {
            Map<String, Release> releasesById = ThriftUtils.getIdMap(componentDatabaseHandler.getReleasesForClearingStateSummary(allReleaseIds));
            for (Project project : projects) {
                final Set<String> releaseIds = extractReleaseIds.apply(project);
                List<Release> releases = releaseIds.stream().map(releasesById::get).collect(Collectors.toList());
                final ReleaseClearingStateSummary releaseClearingStateSummary = ReleaseClearingStateSummaryComputer.computeReleaseClearingStateSummary(releases, project
                        .getClearingTeam());
                project.setReleaseClearingStateSummary(releaseClearingStateSummary);
            }
        }
        return projects;
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user) throws SW360Exception {
        Project project = getProjectById(projectId, user);
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdsToProject = releaseIdToProjects(project, user);
        List<Release> releasesById = componentDatabaseHandler.getReleasesForClearingStateSummary(releaseIdsToProject.keySet());
        Map<String, Component> componentsById = ThriftUtils.getIdMap(
                componentDatabaseHandler.getComponentsShort(
                        releasesById.stream().map(Release::getComponentId).collect(Collectors.toSet())));

        List<ReleaseClearingStatusData> releaseClearingStatuses = new ArrayList<>();
        for (Release release : releasesById) {
            List<String> projectNames = new ArrayList<>();
            List<String> mainlineStates = new ArrayList<>();

            for (ProjectWithReleaseRelationTuple projectWithReleaseRelation : releaseIdsToProject.get(release.getId())) {
                projectNames.add(printName(projectWithReleaseRelation.getProject()));
                mainlineStates.add(ThriftEnumUtils.enumToString(projectWithReleaseRelation.getRelation().getMainlineState()));
                if (projectNames.size() > 3) {
                    projectNames.add("...");
                    mainlineStates.add("...");
                    break;
                }

            }
            releaseClearingStatuses.add(new ReleaseClearingStatusData(release)
                    .setProjectNames(joinStrings(projectNames))
                    .setMainlineStates(joinStrings(mainlineStates))
                    .setComponentType(componentsById.get(release.getComponentId()).getComponentType()));
        }
        return releaseClearingStatuses;
    }

    SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects(Project project, User user) throws SW360Exception {
        Set<String> visitedProjectIds = new HashSet<>();
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = HashMultimap.create();

        releaseIdToProjects(project, user, visitedProjectIds, releaseIdToProjects);
        return releaseIdToProjects;
    }

    private void releaseIdToProjects(Project project, User user, Set<String> visitedProjectIds, Multimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects) throws SW360Exception {

        if (nothingTodo(project, visitedProjectIds)) return;

        nullToEmptyMap(project.getReleaseIdToUsage()).forEach((releaseId, relation) -> {
            releaseIdToProjects.put(releaseId, new ProjectWithReleaseRelationTuple(project, relation));
        });

        Map<String, ProjectRelationship> linkedProjects = project.getLinkedProjects();
        if (linkedProjects != null) {

                for (String projectId : linkedProjects.keySet()) {
                    if (visitedProjectIds.contains(projectId)) continue;

                    Project linkedProject = getProjectById(projectId, user);
                    releaseIdToProjects(linkedProject, user, visitedProjectIds, releaseIdToProjects);
                }
        }
    }

    private boolean nothingTodo(Project project, Set<String> visitedProjectIds) {
        if (project == null) {
            return true;
        }
        return alreadyBeenHere(project.getId(), visitedProjectIds);
    }

    private boolean alreadyBeenHere(String id, Set<String> visitedProjectIds) {
        if (visitedProjectIds.contains(id)) {
            return true;
        }
        visitedProjectIds.add(id);
        return false;
    }

    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user) {
        final Map<String, Project> allProjectsIdMap = getRefreshedAllProjectsIdMap();

        projects.stream().forEach(project -> {
            // build project tree, get all linked release ids and fetch the releases
            // current decision is to not check any permissions for subproject visibility
            Set<String> releaseIdsOfProjectTree = getReleaseIdsOfProjectTree(project, Sets.newHashSet(),
                    allProjectsIdMap, user, null);
            List<Release> releasesForClearingStateSummary = componentDatabaseHandler
                    .getReleasesForClearingStateSummary(releaseIdsOfProjectTree);
            // compute the summaries
            final ReleaseClearingStateSummary releaseClearingStateSummary = ReleaseClearingStateSummaryComputer
                    .computeReleaseClearingStateSummary(releasesForClearingStateSummary, project.getClearingTeam());

            project.setReleaseClearingStateSummary(releaseClearingStateSummary);
        });

        return projects;
    }

    /**
     * Synchronization is not really necessary, we could also remove it. Worst case
     * would then be that the projects map would be loaded twice if one thread just
     * loaded it but has not yet updated the loadingInstant field while another
     * thread queries already (if we change the order and set the instant first and
     * then load the new map, worst case would be that the other thread would get an
     * older map as the new one is not yet set).
     */
    private synchronized Map<String, Project> getRefreshedAllProjectsIdMap() {
        if (cachedAllProjectsIdMap != null && Instant.now()
                .isBefore(cachedAllProjectsIdMapLoadingInstant.plus(ALL_PROJECTS_ID_MAP_CACHE_LIFETIME))) {
            return cachedAllProjectsIdMap;
        }

        cachedAllProjectsIdMap = ThriftUtils.getIdMap(repository.getAll());
        cachedAllProjectsIdMapLoadingInstant = Instant.now();

        return cachedAllProjectsIdMap;
    }

    private Set<String> getReleaseIdsOfProjectTree(Project project, Set<String> visitedProjectIds,
            Map<String, Project> allProjectsIdMap, User user, List<RequestedAction> permissionsFilter) {
        // no need to visit a project twice
        if (visitedProjectIds.contains(project.getId())) {
            return Collections.emptySet();
        }

        // we are now checking this project so no need for further examination in
        // recursion
        visitedProjectIds.add(project.getId());

        // container to aggregate results
        Set<String> releaseIds = Sets.newHashSet();

        // traverse linked projects with relation type other than "REFERRED" and
        // "DUPLICATE" and add the result to this result
        if (project.isSetLinkedProjects()) {
            project.getLinkedProjects().entrySet().stream().forEach(e -> {
                if (!ProjectRelationship.REFERRED.equals(e.getValue())
                        && !ProjectRelationship.DUPLICATE.equals(e.getValue())) {
                    Project childProject = allProjectsIdMap.get(e.getKey());
                    if (childProject != null) {
                        // since we fetched all project up front we did not yet check any permission -
                        // so do it now
                        if (permissionsFilter == null || permissionsFilter.isEmpty()
                                || makePermission(childProject, user).areActionsAllowed(permissionsFilter)) {
                            // recursion inside :-)
                            releaseIds.addAll(getReleaseIdsOfProjectTree(childProject, visitedProjectIds,
                                    allProjectsIdMap, user, permissionsFilter));
                        }
                    }
                }
            });
        }

        // add own releases to result if they are not just "REFERRED"
        if (project.isSetReleaseIdToUsage()) {
            project.getReleaseIdToUsage().entrySet().stream().forEach(e -> {
                if (!ReleaseRelationship.REFERRED.equals(e.getValue().getReleaseRelation())) {
                    releaseIds.add(e.getKey());
                }
            });
        }

        return releaseIds;
    }

    private void sendMailNotificationsForNewProject(Project project, String user) {
        mailUtil.sendMail(project.getProjectResponsible(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_RESPONSIBLE.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getProjectOwner(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_OWNER.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getLeadArchitect(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.LEAD_ARCHITECT.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.MODERATORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getContributors(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.CONTRIBUTORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getSecurityResponsibles(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.SECURITY_RESPONSIBLES.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(project.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.ROLES.toString(),
                project.getName(), project.getVersion());
    }

    private void sendMailNotificationsForProjectUpdate(Project project, String user) {
        mailUtil.sendMail(project.getProjectResponsible(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_RESPONSIBLE.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getProjectOwner(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_OWNER.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getLeadArchitect(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.LEAD_ARCHITECT.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getModerators(), user,
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.MODERATORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getContributors(), user,
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.CONTRIBUTORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getSecurityResponsibles(), user,
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.SECURITY_RESPONSIBLES.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(project.getRoles()), user,
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.ROLES.toString(),
                project.getName(), project.getVersion());
    }


    /**
     * CAUTION!
     * You should most probably use {@link #getAccessibleProjects(User)} instead of this method.
     * This method reads all the projects without checking for visibility constraints. It is intended to provide
     * all projects for export from a scheduled service, where a valid sw360 user is not available.
     * This is a less than optimal situation, but that's the way it is at the moment.
     * @return list of all projects in the database
     */
    private List<Project> getAllProjects() {
        return nullToEmptyList(repository.getAll());
    }

    @NotNull
    private JSONArray serializeProjectsToJson(List<Project> projects) {
        JSONArray serializedProjects = JSONFactoryUtil.createJSONArray();
        Map<String, Release> releaseMap = componentDatabaseHandler.getAllReleasesIdMap();
        componentDatabaseHandler.fillVendors(releaseMap.values());
        Map<String, Component> componentMap = componentDatabaseHandler.getAllComponentsIdMap();
        for (Project p : projects) {
            JSONObject json = JSONFactoryUtil.createJSONObject();
            json.put("application_id", p.getId());
            json.put("application_name", p.getName());
            json.put("application_version", p.getVersion());
            json.put("business_unit", p.getBusinessUnit());
            json.put("user_gids", stringCollectionToJsonArray(nullToEmptySet(p.getSecurityResponsibles())));
            json.put("children_application_ids", stringCollectionToJsonArray(nullToEmptyMap(p.getLinkedProjects()).keySet()));
            json.put("components", serializeReleasesToJson(
                    nullToEmptyMap(p.getReleaseIdToUsage())
                    .keySet()
                    .stream()
                    .map(releaseMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()), componentMap));

            serializedProjects.put(json);
        }
        return serializedProjects;
    }

    private JSONArray stringCollectionToJsonArray(Collection<String> strings) {
        JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
        strings.forEach(jsonArray::put);
        return jsonArray;
    }

    private JSONArray serializeReleasesToJson(List<Release> releases, Map<String, Component> componentMap) {
        JSONArray serializedReleases = JSONFactoryUtil.createJSONArray();
        for (Release r: releases){
            Component c = componentMap.get(r.getComponentId());
            JSONObject json = JSONFactoryUtil.createJSONObject();
            Map<String, String> externalIds = CommonUtils.nullToEmptyMap(r.getExternalIds());
            json.put("external_component_id", r.getId());
            putExternalIdToJsonAsInteger(json, "svm_component_id", externalIds.get(SW360Constants.SIEMENS_SVM_COMPONENT_ID));
            putExternalIdToJsonAsInteger(json, "swml_component_id", externalIds.get(SW360Constants.SIEMENS_MAINLINE_COMPONENT_ID));
            json.put("vendor", Optional.ofNullable(r.getVendor()).map(Vendor::getShortname).orElse(""));
            json.put("name", r.getName());
            json.put("version", r.getVersion());
            if (c == null) {
                log.warn(String.format("Parent component of release %s (%s) with id %s was not found", r.getName(), r.getId(), r.getComponentId()));
            } else {
                json.put("urls", getUrlsJson(r, c));
                json.put("description", c.getDescription());
            }
            json.put("cpe_items", getReleaseCpeIdsJson(r));
            serializedReleases.put(json);
        }
        return serializedReleases;
    }

    private void putExternalIdToJsonAsInteger(JSONObject json, String jsonKey, String extId){
        if (extId == null){
            json.put(jsonKey, (JSONObject) null);
        }
        try {
            json.put(jsonKey, Integer.parseInt(extId));
        } catch (NumberFormatException e){
            json.put(jsonKey, (JSONObject) null);
        }
    }

    private JSONArray putIfNotEmpty(JSONArray array, String value){
        if (!isNullOrEmpty(value)){
            array.put(value);
        }
        return array;
    }
    private JSONArray getReleaseCpeIdsJson(Release r) {
        JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
        putIfNotEmpty(jsonArray, r.getCpeid());
        return jsonArray;
    }

    private JSONArray getUrlsJson(Release r, Component c) {
        JSONArray jsonArray = JSONFactoryUtil.createJSONArray();
        putIfNotEmpty(jsonArray, c.getHomepage());
        putIfNotEmpty(jsonArray, c.getBlog());
        putIfNotEmpty(jsonArray, c.getWiki());
        putIfNotEmpty(jsonArray, r.getDownloadurl());
        return jsonArray;
    }

    public RequestStatus exportForMonitoringList() throws TException {
        // load all projects
        log.info("SVMML: Starting export of projects to SVM monitoring lists");
        List<Project> allProjects = getAllProjects();
        log.info("SVMML: successfully loaded " + allProjects.size() + " projects");
        List<Project> projects = prepareProjectsForSVM(allProjects);
        log.info("SVMML: after filtering out projects with missing security responsibles, " + projects.size() + " projects are left");
        // serialize projects to json
        JSONArray jsonResult = serializeProjectsToJson(projects);
        String jsonString = jsonResult.toString();
        log.info("SVMML: projects serialized to JSON string. String length: " + jsonString.length());
        log.info("SVMML: JSON starts with: " + StringUtils.abbreviate(jsonString, SVMML_JSON_LOG_CUTOFF_LENGTH));

        // send json to svm
        try {
            new SvmConnector().sendProjectExportForMonitoringLists(jsonString);
            log.info("SVMML: sent JSON to SVM");
        } catch (IOException | SW360Exception e) {
            log.error(e);
            return RequestStatus.FAILURE;
        }

        return RequestStatus.SUCCESS;
    }

    private List<Project> prepareProjectsForSVM(List<Project> projects) throws TException {
        Map<String, String> gidsByEmail = getGidsByEmail();

        // convert all security responsibles to gids
        projects.forEach(p -> {
            Set<String> emails = nullToEmptySet(p.getSecurityResponsibles());
            Set<String> gids = emails
                    .stream()
                    .map(gidsByEmail::get)
                    .filter(Objects::nonNull)
                    .filter(s -> PLAUSIBLE_GID_REGEXP.matcher(s).matches())
                    .collect(Collectors.toSet());
            if (emails.size() != gids.size()){
                log.warn("SVMML: couldn't find gids for some of the emails from project " + SW360Utils.printName(p) + " " + p.getId());
            }
            p.setSecurityResponsibles(gids);

            // Here comes the tricky part.
            // If SVM is disabled, clear security responsibles, but enable SVM.
            // This way, the project will be sent to SVM only if it gets some propagated secreps
            // from parent projects and only the propagated secreps will get the notifications.
            // At the same time, only projects that had enableSVM from the start or subprojects of such projects
            // will be sent to SVM.
            if (!p.isEnableSvm()){
                p.setSecurityResponsibles(Collections.emptySet());
                p.setEnableSvm(true);
            }
        });

        // create copies for propagating responsibles to
        // propagating directly to originals will cause unnecessary propagation when the iteration comes to
        // the subprojects
        List<Project> projectCopies = projects.stream().map(Project::new).collect(Collectors.toList());
        Map<String, Project> projectsById = ThriftUtils.getIdMap(projectCopies);

        // propagate security responsibles' gids to subprojects
        // take the responsibles from the original projects, not the copies
        projects.forEach(p -> {
            Set<String> responsibles = nullToEmptySet(p.getSecurityResponsibles());
            Set<String> linkedProjectIds = nullToEmptyMap(p.getLinkedProjects()).keySet();
            if (!responsibles.isEmpty() && !linkedProjectIds.isEmpty()) {
                propagateSecurityResponsiblesToLinkedProjects(responsibles, linkedProjectIds, projectsById, new HashSet<>());
            }
        });

        // return the copies, not the originals
        return projects
                .stream()
                .map(p -> projectsById.get(p.getId()))
                .filter(p -> p.isSetSecurityResponsibles() && !p.getSecurityResponsibles().isEmpty())
                .filter(Project::isEnableSvm)
                .collect(Collectors.toList());
    }

    private void propagateSecurityResponsiblesToLinkedProjects(Set<String> responsibles, Set<String> linkedProjectIds, Map<String, Project> projectsById, HashSet<String> visitedIds) {
        linkedProjectIds.stream().map(projectsById::get).filter(Objects::nonNull).forEach(p -> {
            if (!visitedIds.contains(p.getId())) {
                Set<String> currentResponsibles = nullToEmptySet(p.getSecurityResponsibles());
                currentResponsibles.addAll(responsibles);
                p.setSecurityResponsibles(currentResponsibles);
                visitedIds.add(p.getId());
                propagateSecurityResponsiblesToLinkedProjects(responsibles, nullToEmptyMap(p.getLinkedProjects()).keySet(), projectsById, visitedIds);
            }
        });
    }

    private Map<String, String> getGidsByEmail() throws TException {
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        return userClient
                .getAllUsers()
                .stream()
                .filter(User::isSetExternalid)
                .collect(Collectors.toMap(User::getEmail, User::getExternalid, (s1, s2) -> s1));
    }
}
