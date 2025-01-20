/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Lucene search for the Release class
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler {

    private static final LuceneSearchView luceneSearchView
            = new LuceneSearchView("lucene", "releases",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'release') return ret;" +
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
                    "    idx(doc);" +
                    "    return ret;" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public ReleaseSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cClient, dbName);
        connector.addView(luceneSearchView);
    }

    public List<Release> search(String searchText) {
        return connector.searchView(Release.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

    public List<Release> searchAccessibleReleasesByPurl(String text, final Map<String, Set<String>> subQueryRestrictions, User user) {
        List<Release> releaseList = new ArrayList<>();
        try {
            List<Release> resultReleaseList = connector.searchViewWithRestrictions(Release.class, luceneSearchView, text, subQueryRestrictions);
            for (Release release : resultReleaseList) {
                if (makePermission(release, user).isActionAllowed(RequestedAction.READ)) {
                    releaseList.add(release);
                }
            }
        } catch (Exception e) {
            // Handle the exception (e.g., log the error)
            System.err.println("Error during search: " + e.getMessage());
        }
        return releaseList;
    }
}
