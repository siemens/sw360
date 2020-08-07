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

import java.util.List;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.support.View;
import org.ektorp.support.Views;

/**
 * CRUD access for the Package class
 *
 * @author abdul.mannankapti@siemens.com
 */
@Views({
    @View(name = "all",
            map = "function(doc) { if (doc.type == 'package') emit(null, doc._id) }"),
    @View(name = "byname",
            map = "function(doc) { if(doc.type == 'package') { emit(doc.name, doc) } }"),
    @View(name = "packagesByReleaseId",
            map = "function(doc) {" +
                " if (doc.type == 'package'){" +
                "      emit(doc.releaseId, doc);" +
                "  }" +
                "}")

})
public class PackageRepository extends DatabaseRepository<Package> {

    public PackageRepository(DatabaseConnector db) {
        super(Package.class, db);
        initStandardDesignDocument();
    }

    public List<Package> getPackagesFromReleaseId(String id) {
         return queryView("packagesByReleaseId", id);
    }

    public List<Package> searchByName(String name, User user) {
        return queryView("byname", name);
    }

}
