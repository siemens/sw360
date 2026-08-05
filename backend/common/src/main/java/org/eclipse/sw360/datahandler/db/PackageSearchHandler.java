/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Packages with paginated access control filtering.
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class PackageSearchHandler extends BaseNouveauSearchHandler<Package> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> PACKAGE_FIELDS = List.of(
            IndexField.standard("name"),
            IndexField.standard("version"),
            IndexField.simple("purl"),
            IndexField.simple("releaseId"),
            IndexField.simple("vcs"),
            IndexField.simple("packageManager", "keyword"),
            IndexField.simple("packageType", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.date("createdOn")
    );

    /**
     * Package-specific JS for array-backed fields that should support text
     * and sort lookups via arrayToStringIndex helper.
     */
    private static final String PACKAGE_CUSTOM_JS =
            "    arrayToStringIndex(doc.licenseIds, 'licenseIds');";

    /**
     * Analyzer overrides for fields created by {@code arrayToStringIndex}.
     * The helper generates {@code <field>_sort} string indexes that require
     * the {@code keyword} analyzer for correct sorting behavior.
     */
    private static final Map<String, String> PACKAGE_CUSTOM_ANALYZERS = Map.of(
            "licenseIds_sort", "keyword"
    );

    private static final BuiltIndexDefinition PACKAGE_INDEX_DEFINITION = buildIndexFunction(
            "package",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            PACKAGE_FIELDS,
            PACKAGE_CUSTOM_JS,
            PACKAGE_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    public PackageSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Package.class, "packages", PACKAGE_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API
    // -------------------------------------------------------------------------

    /**
     * Paginated search with permission filtering.
     */
    public Map<PaginationData, List<Package>> searchAccessiblePackages(
            final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Package>> result = baseSearch(connector, subQueryRestrictions, pageData);

        PaginationData respPageData = result.keySet().iterator().next();
        List<Package> packageList = result.values().iterator().next();

        packageList = packageList.stream().filter(pkg ->
                makePermission(pkg, user).isActionAllowed(RequestedAction.READ))
                .toList();

        return Collections.singletonMap(respPageData, packageList);
    }

    /**
     * Non-paginated search (legacy callers).
     */
    public List<Package> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictionsWithAnd(Package.class, getIndexName(),
                text, subQueryRestrictions);
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (PackageSortColumn.findByValue(sortColumnNumber)) {
            case PackageSortColumn.BY_NAME -> List.of("name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_PURL -> List.of("purl_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_LICENSE -> List.of("licenseIds_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_PACKAGE_MANAGER -> List.of("packageManager_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case PackageSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
