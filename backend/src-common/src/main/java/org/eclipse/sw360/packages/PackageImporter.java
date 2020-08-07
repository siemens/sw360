/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.packages;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManagerType;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.packages.model.OrtModel;
import org.eclipse.sw360.packages.model.PackageModel;
import org.eclipse.sw360.packages.model.Packages;
import org.eclipse.sw360.packages.model.ProjectModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PackageImporter {

    private static final Logger log = Logger.getLogger(PackageImporter.class);

    private final PackageDatabaseHandler packageDatabaseHandler;
    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final User user;

    public PackageImporter(PackageDatabaseHandler packageDatabaseHandler,
            ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler,
            User user) {
        this.packageDatabaseHandler = packageDatabaseHandler;
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.user = user;
    }

    public RequestSummary importFromBOM(InputStream inputStream, AttachmentContent attachmentContent) {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        RequestSummary requestSummary = new RequestSummary();
        requestSummary.setTotalAffectedElements(0);
        requestSummary.setTotalElements(0);
        try {
            mapper.findAndRegisterModules();
            OrtModel modelObject = mapper.readValue(inputStream, OrtModel.class);
            List<ProjectModel> projectModels = CommonUtils.nullToEmptyList(modelObject.getAnalyzer().getResult().getProjects());
            Set<String> projectNames = projectModels.stream().map(ProjectModel::getId).map(s -> s.replaceAll(".+::", "")).collect(Collectors.toSet());
            int projectCount = projectNames.size();
            if (projectCount < 1) {
                requestSummary.setMessage("The provided Yaml did not contain any Project tag.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            }
            List<Packages> packages = modelObject.getAnalyzer().getResult().getPackages();
            Set<String> componentNames = packages.stream().map(Packages::getPckg).map(PackageModel::getId).map(s -> s.replaceAll(".+::", "")).collect(Collectors.toSet());
            int compCount = componentNames.size();
            if (compCount < 1) {
                requestSummary.setTotalAffectedElements(projectCount);
                requestSummary.setTotalElements(projectCount);
                requestSummary.setMessage("The provided Yaml did not contain any Package tag.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            }
            return importProjectWithComponentReleasesAndPackages(attachmentContent, packages, projectModels);
        } catch (IOException e) {
            log.error("IOException while parsing Packages Yaml!", e);
        } catch (Exception e) {
            log.error("Exception while parsing Packages Yaml!", e);
        }
        return null;
    }

    private RequestSummary importProjectWithComponentReleasesAndPackages(AttachmentContent attachmentContent, List<Packages> packages, List<ProjectModel> projectModels) throws SW360Exception {
        int total = 0;
        int affectedCount = 0;
        final RequestSummary requestSummary = new RequestSummary();
        Map<String, List<PackageModel>> vcsToPackageMap = new HashMap<String, List<PackageModel>>();
        for (Packages pkgs : packages) {
            PackageModel pkg = pkgs.getPckg();
            if (null != pkg.getVcsProcessed() && null != pkg.getVcsProcessed().getUrl()) {
                vcsToPackageMap.computeIfAbsent(pkg.getVcsProcessed().getUrl(), e -> new ArrayList<PackageModel>()).add(pkg);
            }
        }

        ProjectReleaseRelationship prRelation = new ProjectReleaseRelationship(ReleaseRelationship.UNKNOWN, MainlineState.OPEN);
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = Maps.newHashMap();
        List<String> projectIds = Lists.newArrayList();
        AddDocumentRequestSummary addDocumentRequestSummary;

        for (Map.Entry<String, List<PackageModel>> entry : vcsToPackageMap.entrySet()) {
            List<PackageModel> pkgs = entry.getValue();

            // let's get first package and create component/release associated with it.
            PackageModel pckgModel = pkgs.get(0);
            // split the id to get the PackageManagerType, packageName and version.
            String[] values = pckgModel.getId().split(":");
            // invalid id if values array size is less than 4
            if (values.length < 4) {
                continue;
            }
            String name = values[2];

            String url = CommonUtils.nullToEmptyString(pckgModel.getVcsProcessed().getUrl());
            String componentName = url.isEmpty() ? name : url.substring(url.lastIndexOf('/') + 1);
            // Create Component
            Component comp = createComponent(componentName); 
            addDocumentRequestSummary = addComponent(comp);
            comp.setId(addDocumentRequestSummary.getId());
            affectedCount = AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()) ? (affectedCount + 1) : affectedCount;
            total += total;

            // Create Release
            String version = values[3];
            Release rel = createRelease(version, comp, pckgModel);
            addDocumentRequestSummary = addRelease(rel);
            rel.setId(addDocumentRequestSummary.getId());
            affectedCount = AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()) ? (affectedCount + 1) : affectedCount;
            total += total;

            releaseIdToUsage.put(rel.getId(), prRelation);
            String type = values[0];
            if (pkgs.size() == 1) {
                // Create packages
                Package pckg = createPackage(type, name, version, rel, pckgModel);
                addDocumentRequestSummary = addPackage(pckg);
                rel.setId(addDocumentRequestSummary.getId());
                affectedCount = AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()) ? (affectedCount + 1) : affectedCount;
                total += total;
            } else {
                for (PackageModel pkgModel : pkgs) {
                    values = pkgModel.getId().split(":");
                    if (values.length < 4) {
                        continue;
                    }
                    name = values[2];
                    version = values[3];

                    // Create packages
                    Package pckg = createPackage(type, name, version, rel, pkgModel);
                    addDocumentRequestSummary = addPackage(pckg);
                    rel.setId(addDocumentRequestSummary.getId());
                    affectedCount = AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()) ? (affectedCount + 1) : affectedCount;
                    total += total;
                }
            }
        }

        for (ProjectModel projectModel : projectModels) {
            // Create Project
            Project project = createProject(projectModel, releaseIdToUsage, attachmentContent);
            addDocumentRequestSummary = addProject(project);
            project.setId(addDocumentRequestSummary.getId());
            affectedCount = AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()) ? (affectedCount + 1) : affectedCount;
            total += total;
            projectIds.add(project.getId());
        }
        requestSummary.setTotalElements(total);
        requestSummary.setTotalAffectedElements(affectedCount);
        requestSummary.setMessage(projectIds.get(0));
        requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        return requestSummary;
    }

    private Attachment makeAttachmentFromContent(AttachmentContent attachmentContent) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContent.getId());
        attachment.setAttachmentType(AttachmentType.OTHER);
        attachment.setCreatedComment("Used for importing package BOM");
        attachment.setFilename(attachmentContent.getFilename());

        return attachment;
    }
    
    private Project createProject(ProjectModel projectModel, Map<String, ProjectReleaseRelationship> releaseIdToUsage, AttachmentContent attachmentContent) {
        // split the id to get the PackageManagerType, packageName and version.
        String[] values = projectModel.getId().split(":");
        // invalid id if values array size is less than 4
        if (values.length < 4) {
            return null;
        }
        Project project = new Project();
        project.setName(values[2]);
        project.setVersion(values[3]);
        project.setReleaseIdToUsage(releaseIdToUsage);
        project.setVisbility(Visibility.EVERYONE);
        project.setAttachments(Sets.newHashSet());
        if(attachmentContent != null) {
            Attachment attachment = makeAttachmentFromContent(attachmentContent);
            project.setAttachments(Collections.singleton(attachment));
        }

        return project;
    }

    private Component createComponent(String name) {
        Component component = new Component();
        component.setName(name);
        return component;
    }

    private Release createRelease(String version, Component component, PackageModel pkg) {
        Release release = new Release();
        release.setName(component.getName());
        release.setVersion(StringUtils.defaultIfBlank(version, "NA"));
        release.setComponentId(component.getId());
        release.setCreatorDepartment(user.getDepartment());
        release.setDownloadurl(null != pkg.getVcsProcessed() ? CommonUtils.nullToEmptyString(pkg.getVcsProcessed().getUrl()) : "");
        return release;
    }

    private Package createPackage(String type, String name, String version, Release release, PackageModel pkgModel) {
        Package pckg = new Package();
        pckg.setName(name);
        pckg.setVersion(version);
        pckg.setReleaseId(release.getId());
        pckg.setDescription(pkgModel.getDescription());
        pckg.setDeclaredLicenses(pkgModel.getDeclaredLicenses());
        pckg.setBinaryArtifactUrl(pkgModel.getBinaryArtifact().getUrl());
        pckg.setSourceArtifactUrl(pkgModel.getSourceArtifact().getUrl());
        pckg.setVcs(pkgModel.getVcs().getUrl());
        pckg.setVcsProcessed(pkgModel.getVcsProcessed().getUrl());
        pckg.setPackageManagerType(ThriftEnumUtils.stringIgnoreCaseToEnum(type, PackageManagerType.class));
        return pckg;
    }

    private AddDocumentRequestSummary addComponent(Component component) throws SW360Exception {
        log.debug("create Component { name='" + component.getName() + "' }");
        final AddDocumentRequestSummary addDocumentRequestSummary = componentDatabaseHandler.addComponent(component, user.getEmail());

        final String componentId = addDocumentRequestSummary.getId();
        if(componentId == null || componentId.isEmpty()) {
            throw new SW360Exception("Component Id should not be empty. " + addDocumentRequestSummary.toString());
        }
        return addDocumentRequestSummary;
    }

    public AddDocumentRequestSummary addRelease(Release release) throws SW360Exception {
        log.debug("create Release { name='" + release.getName() + "', version='" + release.getVersion() + "' }");
        final AddDocumentRequestSummary addDocumentRequestSummary = componentDatabaseHandler.addRelease(release, user);

        final String releaseId = addDocumentRequestSummary.getId();
        if(releaseId == null || releaseId.isEmpty()) {
            throw new SW360Exception("Release Id should not be empty. " + addDocumentRequestSummary.toString());
        }
        return addDocumentRequestSummary;
    }

    public AddDocumentRequestSummary addPackage(Package pkg) throws SW360Exception {
        log.debug("create Package { name='" + pkg.getName() + "', version='" + pkg.getVersion() + "' }");
        final AddDocumentRequestSummary addDocumentRequestSummary = packageDatabaseHandler.addPackage(pkg, user);

        final String releaseId = addDocumentRequestSummary.getId();
        if(releaseId == null || releaseId.isEmpty()) {
            throw new SW360Exception("Release Id should not be empty. " + addDocumentRequestSummary.toString());
        }
        return addDocumentRequestSummary;
    }

    public AddDocumentRequestSummary addProject(Project project) throws SW360Exception {
        log.debug("create Project { name='" + project.getName() + "', version='" + project.getVersion() + "' }");

        final Set<Attachment> attachments = project.getAttachments();
        if(attachments != null && attachments.size() > 0) {
            project.setAttachments(attachments.stream()
                    .map(a -> a.setCreatedBy(user.getEmail()))
                    .collect(Collectors.toSet()));
        }
        final AddDocumentRequestSummary addDocumentRequestSummary = projectDatabaseHandler.addProject(project, user);

        final String projectId = addDocumentRequestSummary.getId();
        if(projectId == null || projectId.isEmpty()) {
            throw new SW360Exception("Project Id should not be empty. " + addDocumentRequestSummary.toString());
        }
        return addDocumentRequestSummary;
    }
}
