/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package com.siemens.sw360.vmcomponents.db;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import com.siemens.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.ektorp.support.View;

import java.util.Set;

/**
 * CRUD access for the VMComponent class
 *
 * @author stefan.jaeger@evosoft.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'vmcomponent') emit(null, doc._id) }")
public class VMComponentRepository extends DatabaseRepository<VMComponent> {

    private static final String BY_VMID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent') {" +
                    "    emit(doc.vmid, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LAST_UPDATE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.lastUpdateDate != null) {" +
                    "    emit(doc.lastUpdateDate, doc._id);" +
                    "  } " +
                    "}";

    private static final String ALL_VMIDS =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent') {" +
                    "    emit(null, doc.vmid);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_NAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.name != null) {" +
                    "    emit(doc.name.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VENDOR_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.vendor != null) {" +
                    "    emit(doc.vendor.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VERSION_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.version != null) {" +
                    "    emit(doc.version.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    public VMComponentRepository(DatabaseConnector db) {
        super(VMComponent.class, db);

        initStandardDesignDocument();
    }

    @View(name = "byvmid", map = BY_VMID_VIEW)
    public VMComponent getComponentByVmid(String vmid) {
        final Set<String> idList = queryForIdsAsValue("byvmid", vmid);
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    @View(name = "bylastupdate", map = BY_LAST_UPDATE_VIEW)
    public VMComponent getComponentByLastUpdate(String lastUpdateDate) {
        final Set<String> idList;
        if (lastUpdateDate == null){
            idList = getAllIdsByView("bylastupdate", true);
        } else {
            idList = queryForIdsAsValue("bylastupdate", lastUpdateDate);
        }
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    @View(name = "all_vmids", map = ALL_VMIDS)
    public Set<String> getAllVmids() {
        return queryForIdsAsValue(createQuery("all_vmids"));
    }

    @View(name = "componentByName", map = BY_LOWERCASE_NAME_VIEW)
    public Set<String> getComponentByLowercaseNamePrefix(String namePrefix) {
        return queryForIdsByPrefix("componentByName", namePrefix != null ? namePrefix.toLowerCase() : namePrefix);
    }

    @View(name = "componentByVendor", map = BY_LOWERCASE_VENDOR_VIEW)
    public Set<String> getComponentByLowercaseVendorPrefix(String vendorPrefix) {
        return queryForIdsByPrefix("componentByVendor", vendorPrefix != null ? vendorPrefix.toLowerCase() : vendorPrefix);
    }

    @View(name = "componentByVersion", map = BY_LOWERCASE_VERSION_VIEW)
    public Set<String> getComponentByLowercaseVersionPrefix(String versionPrefix) {
        return queryForIdsByPrefix("componentByVersion", versionPrefix != null ? versionPrefix.toLowerCase() : versionPrefix);
    }

}
