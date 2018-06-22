/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import com.google.common.base.Strings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.ektorp.support.View;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import java.util.Set;

/**
 * CRUD access for the Vendor class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'vendor') emit(null, doc._id) }")
public class VendorRepository extends DatabaseRepository<Vendor> {

    private static final String BY_LOWERCASE_VENDOR_SHORTNAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vendor' && doc.shortname != null) {" +
                    "    emit(doc.shortname.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VENDOR_FULLNAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vendor' && doc.fullname != null) {" +
                    "    emit(doc.fullname.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    public VendorRepository(DatabaseConnector db) {
        super(Vendor.class, db);

        initStandardDesignDocument();
    }

    public void fillVendor(Release release) {
        if (release.isSetVendorId()) {
            final String vendorId = release.getVendorId();
            if (!isNullOrEmpty(vendorId)) {
                final Vendor vendor = get(vendorId);
                if (vendor != null)
                    release.setVendor(vendor);
            }
            release.unsetVendorId();
        }
    }

    @View(name = "vendorbyshortname", map = BY_LOWERCASE_VENDOR_SHORTNAME_VIEW)
    public Set<String> getVendorByLowercaseShortnamePrefix(String shortnamePrefix) {
        return queryForIdsByPrefix("vendorbyshortname", shortnamePrefix != null ? shortnamePrefix.toLowerCase() : shortnamePrefix);
    }

    @View(name = "vendorbyfullname", map = BY_LOWERCASE_VENDOR_FULLNAME_VIEW)
    public Set<String> getVendorByLowercaseFullnamePrefix(String fullnamePrefix) {
        return queryForIdsByPrefix("vendorbyfullname", fullnamePrefix != null ? fullnamePrefix.toLowerCase() : fullnamePrefix);
    }
}
