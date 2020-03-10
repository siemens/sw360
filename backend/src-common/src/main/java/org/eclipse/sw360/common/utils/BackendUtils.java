/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.sw360.datahandler.common.CommonUtils;

public class BackendUtils {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    protected static final Properties loadedProperties;
    public static final Boolean MAINLINE_STATE_ENABLED_FOR_USER;
    public static final Map<String, Set<String>> DEPARTMENT_TO_BA_BL_MAP;

    static {
        loadedProperties = CommonUtils.loadProperties(BackendUtils.class, PROPERTIES_FILE_PATH);
        MAINLINE_STATE_ENABLED_FOR_USER = Boolean.parseBoolean(loadedProperties.getProperty("mainline.state.enabled.for.user", "false"));
        DEPARTMENT_TO_BA_BL_MAP = new HashMap<String, Set<String>>();
        Set<String> departments = CommonUtils.splitToSet(loadedProperties.getProperty("department.with.business.area", "DEPARTMENT"));
        departments.forEach(dep -> {
            DEPARTMENT_TO_BA_BL_MAP.putIfAbsent(dep, CommonUtils.splitToSet(loadedProperties.getProperty(dep+".business.area", "BA 1,BA 2, BA 3")));
        });
    }

    protected BackendUtils() {
        // Utility class with only static functions
    }
}
