/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.packages.PackageImporter;
import org.eclipse.sw360.packages.model.OrtModel;
import org.eclipse.sw360.packages.model.ProjectModel;
import org.ektorp.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Class for accessing the CouchDB database for Artifacts.
 *
 * @author: abdul.mannankapti@siemens.com
 */
public class PackageDatabaseHandler extends AttachmentAwareDatabaseHandler {

    private final AttachmentConnector attachmentConnector;
    private final DatabaseConnector db;
    private final PackageRepository packageRepository;
    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;

    private static final Logger log = Logger.getLogger(PackageDatabaseHandler.class);

    public PackageDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName,
            AttachmentDatabaseHandler attachmentDatabaseHandler,  ComponentDatabaseHandler componentDatabaseHandler,
            ProjectDatabaseHandler projectDatabaseHandler) throws MalformedURLException {

        super(attachmentDatabaseHandler);
        db = new DatabaseConnector(httpClient, dbName);
        packageRepository = new PackageRepository(db);

        // Create the attachment connector
        attachmentConnector = new AttachmentConnector(httpClient, attachmentDbName, Duration.durationOf(30, TimeUnit.SECONDS));

        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
    }

    public PackageDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        this(httpClient, dbName, attachmentDbName, new AttachmentDatabaseHandler(httpClient, dbName, attachmentDbName),
                new ComponentDatabaseHandler(httpClient, dbName, attachmentDbName), new ProjectDatabaseHandler(httpClient, dbName, attachmentDbName));
    }

    public Package getPackageById(String id) throws SW360Exception {
        Package pkg = packageRepository.get(id);
        assertNotNull(pkg, "Invalid Package Id");
        return pkg;
    }

    public List<Package> getPackageByIds(Set<String> ids) throws SW360Exception {
        List<Package> packages = packageRepository.get(ids);
        assertNotEmpty(packages, "Invalid Package Ids");
        return packages;
    }

    public Set<Package> getPackagesByReleaseIds(Set<String> ids) {
        Set<Package> packages = Sets.newHashSet();
        for (String id : ids) {
            packages.addAll(packageRepository.getPackagesFromReleaseId(id));
        }
        return packages;
    }

    public List<Package> getAllPackages() {
        return packageRepository.getAll();
    }

    public AddDocumentRequestSummary addPackage(Package pkg, User user) {
        
        pkg.setCreatedBy(user.getEmail());
        pkg.setCreatedOn(SW360Utils.getCreatedOn());

        packageRepository.add(pkg);

        return new AddDocumentRequestSummary().setId(pkg.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    public RequestSummary importPackagesFromAttachmentContent(User user, String attachmentContentId) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector.unsafeGetAttachmentStream(attachmentContent)) {
                final PackageImporter packageImporter = new PackageImporter(this, projectDatabaseHandler, componentDatabaseHandler, user);
                return packageImporter.importFromBOM(inputStream, attachmentContent);
            }
        } catch (IOException e) {
            log.error("Exception while parsing Packages Yaml!", e);
            throw new SW360Exception(e.getMessage());
        }
    }

}
