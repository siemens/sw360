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

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageSearchHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Implementation of the Thrift service
 *
 * @author abdul.mannankapti@siemens.com
 */
public class PackageHandler implements PackageService.Iface {

    private final PackageDatabaseHandler handler;
    private final PackageSearchHandler searchHandler;

    PackageHandler() throws IOException {
        handler = new PackageDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
        searchHandler = new PackageSearchHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    @Override
    public Package getPackageById(String packageId) throws TException {
        assertId(packageId);
        return handler.getPackageById(packageId);
    }

    @Override
    public List<Package> getPackageByIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getPackageByIds(ids);
    }

    @Override
    public List<Package> getAllPackages() throws TException {
        return handler.getAllPackages();
    }

    @Override
    public Set<Package> getPackagesByReleaseIds(Set<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getPackagesByReleaseIds(ids);
    }

    @Override
    public RequestSummary importPackagesFromAttachmentContent(User user, String attachmentContentId) throws TException {
        assertNotNull(attachmentContentId);
        assertUser(user);
        return handler.importPackagesFromAttachmentContent(user, attachmentContentId);
    }

    @Override
    public AddDocumentRequestSummary addPackage(Package pkg, User user) throws TException {
        assertNotNull(pkg);
        assertUser(user);
        return handler.addPackage(pkg, user);
    }
}