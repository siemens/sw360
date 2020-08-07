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

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class PackageSearchHandler {

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "packages",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'package') return ret;" +
                    "    function idx(obj) {" +
                    "        for (var key in obj) {" +
                    "            switch (typeof obj[key]) {" +
                    "                case 'object':" +
                    "                    idx(obj[key]);" +
                    "                    break;" +
                    "                case 'function':" +
                    "                    break;" +
                    "                default:" +
                    "                    ret.add(obj[key]);" +
                    "                    break;" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    return ret;" +
                    "}");


    private final LuceneAwareDatabaseConnector connector;

    public PackageSearchHandler(Supplier<HttpClient> httpClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Package> search(String text, final Map<String , Set<String > > subQueryRestrictions, User user ){
        return connector.searchPackagesViewWithRestrictionsAndFilter(luceneSearchView, text, subQueryRestrictions, user);
    }

    public List<Package> search(String searchText) {
        return connector.searchView(Package.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

}
