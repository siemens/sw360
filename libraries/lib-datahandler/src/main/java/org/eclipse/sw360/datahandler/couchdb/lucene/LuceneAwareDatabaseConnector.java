/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb.lucene;

import com.github.ldriscoll.ektorplucene.LuceneAwareCouchDbConnector;
import com.github.ldriscoll.ektorplucene.LuceneQuery;
import com.github.ldriscoll.ektorplucene.LuceneResult;
import com.github.ldriscoll.ektorplucene.util.IndexUploader;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.ThriftEnumUtils.enumByString;


/**
 * Generic database connector for handling lucene searches
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class LuceneAwareDatabaseConnector extends LuceneAwareCouchDbConnector {

    private static final Logger log = Logger.getLogger(LuceneAwareDatabaseConnector.class);

    private static final Joiner AND = Joiner.on(" AND ");
    private static final Joiner OR = Joiner.on(" OR ");

    private final DatabaseConnector connector;

    private static final List<String> LUCENE_SPECIAL_CHARACTERS = Arrays.asList("[\\\\\\+\\-\\!\\~\\*\\?\\^\\:\\(\\)\\{\\}\\[\\]]", "\\&\\&", "\\|\\|");

    /**
     * Maximum number of results to return
     */
    private int resultLimit = 0;

    /**
     * URL/DbName constructor
     */
    public LuceneAwareDatabaseConnector(Supplier<HttpClient> httpClient, String dbName) throws IOException {
        this(new DatabaseConnector(httpClient, dbName));
    }

    /**
     * Constructor using a Database connector
     */
    public LuceneAwareDatabaseConnector(DatabaseConnector connector) throws IOException {
        super(connector.getDbName(), connector.getInstance());
        this.connector = connector;
        setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public boolean addView(LuceneSearchView function) {
        // make sure that the indexer is up-to-date
        IndexUploader uploader = new IndexUploader();
        return uploader.updateSearchFunctionIfNecessary(this, function.searchView,
                function.searchFunction, function.searchBody);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public <T> List<T> searchView(Class<T> type, LuceneSearchView function, String queryString) {
        return connector.get(type, searchIds(type,function, queryString));
    }

    /**
     * Search with lucene using the previously declared search function only for ids
     */
    public <T> List<String> searchIds(Class<T> type, LuceneSearchView function, String queryString) {
        LuceneResult queryLuceneResult = searchView(function, queryString, false);
        return getIdsFromResult(queryLuceneResult);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    public LuceneResult searchView(LuceneSearchView function, String queryString) {
        return searchView(function, queryString, true);
    }

    /**
     * Search with lucene using the previously declared search function
     */
    private LuceneResult searchView(LuceneSearchView function, String queryString, boolean includeDocs) {
        if (isNullOrEmpty(queryString)) {
            return null;
        }

        LuceneQuery query = new LuceneQuery(function.searchView, function.searchFunction);
        query.setQuery(queryString);
        query.setIncludeDocs(includeDocs);
        setQueryLimit(query);

        try {
            return queryLucene(query);
        } catch (DbAccessException e) {
            log.error("Error querying database.", e);
            return null;
        }
    }

    /////////////////////////
    // GETTERS AND SETTERS //
    /////////////////////////

    private void setQueryLimit(LuceneQuery query) {
        if (resultLimit > 0) {
            query.setLimit(resultLimit);
        }
    }

    public void setResultLimit(int limit) {
        if (limit >= 0) {
            resultLimit = limit;
        }
    }

    ////////////////////
    // HELPER METHODS //
    ////////////////////

    private static List<String> getIdsFromResult(LuceneResult result) {
        List<String> ids = new ArrayList<>();
        if (result != null) {
            for (LuceneResult.Row row : result.getRows()) {
                ids.add(row.getId());
            }
        }
        return ids;
    }


    /**
     * Search the database for a given string and types
     */
    public <T> List<T> searchViewWithRestrictions(Class<T> type,LuceneSearchView luceneSearchView, String text, final Map<String , Set<String > > subQueryRestrictions) {
        List <String> subQueries = new ArrayList<>();
        for (Map.Entry<String, Set<String>> restriction : subQueryRestrictions.entrySet()) {

            final Set<String> filterSet = restriction.getValue();

            if (!filterSet.isEmpty()) {
                final String fieldName = restriction.getKey();
                String subQuery = formatSubquery(filterSet, fieldName);
                subQueries.add(subQuery);
            }
        }

        if (!isNullOrEmpty(text)) {
            subQueries.add(prepareWildcardQuery(text));
        }

        String query  = AND.join(subQueries);
        return searchView(type, luceneSearchView, query);
    }

    public List<Project> searchProjectViewWithRestrictionsAndFilter(LuceneSearchView luceneSearchView, String text,
            final Map<String, Set<String>> subQueryRestrictions, User user) {
        List<Project> projectList = searchViewWithRestrictions(Project.class, luceneSearchView, text,
                subQueryRestrictions);
        return projectList.stream().filter(ProjectPermissions.isVisible(user)).collect(Collectors.toList());
    }

    private static String formatSubquery(Set<String> filterSet, final String fieldName) {
        final Function<String, String> addType = input -> {
            if (fieldName.equals("state")) {
                return fieldName + ":\"" + (enumByString(input, ProjectState.class).toString()) + "\"";
            } else if (fieldName.equals("projectType")) {
                return fieldName + ":\"" + (enumByString(input, ProjectType.class).toString()) + "\"";
            } else if (fieldName.equals("businessUnit") || fieldName.equals("tag")) {
                return fieldName + ":\"" + input + "\"";
            } else {
                return fieldName + ":" + input;
            }
        };

        FluentIterable<String> searchFilters = FluentIterable.from(filterSet).transform(addType);
        return "( " + OR.join(searchFilters) + " ) ";
    }

    public static String prepareWildcardQuery(String query) {
        if (DatabaseSettings.LUCENE_LEADING_WILDCARD) {
            return "*" + sanitizeQueryInput(query) + "*";
        } else {
            return sanitizeQueryInput(query) + "*";
        }
    }

    public static String prepareFuzzyQuery(String query) {
        return sanitizeQueryInput(query) + "~";
    }

    private static String sanitizeQueryInput(String input) {
        if (isNullOrEmpty(input)) {
            return nullToEmpty(input);
        } else {
            for (String removeStr : LUCENE_SPECIAL_CHARACTERS) {
                input = input.replaceAll(removeStr, " ");
            }
            return input.trim();
        }
    }
}
