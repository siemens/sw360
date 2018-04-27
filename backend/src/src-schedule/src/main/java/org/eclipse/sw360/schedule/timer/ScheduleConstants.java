/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications from Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.schedule.timer;


import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.log4j.Logger.getLogger;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class ScheduleConstants {
    private static final Logger log = getLogger(ScheduleConstants.class);

    private ScheduleConstants(){}

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final String CVESEARCH_OFFSET_PROPERTY_NAME = "schedule.cvesearch.firstOffset.seconds";
    public static final String CVESEARCH_INTERVAL_PROPERTY_NAME = "schedule.cvesearch.interval.seconds";
    public static final String AUTOSTART_PROPERTY_NAME = "autostart";
    public static final String CVESEARCH_OFFSET_DEFAULT  = 0 + "" ; // default 00:00 am, in seconds
    public static final String CVESEARCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMSYNC_OFFSET_PROPERTY_NAME = "schedule.svmsync.firstOffset.seconds";
    public static final String SVMSYNC_INTERVAL_PROPERTY_NAME = "schedule.svmsync.interval.seconds";
    public static final String SVMSYNC_OFFSET_DEFAULT  = (1*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMSYNC_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMMATCH_OFFSET_PROPERTY_NAME = "schedule.svmmatch.firstOffset.seconds";
    public static final String SVMMATCH_INTERVAL_PROPERTY_NAME = "schedule.svmmatch.interval.seconds";
    public static final String SVMMATCH_OFFSET_DEFAULT  = (2*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMMATCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME = "schedule.svmlistupdate.firstOffset.seconds";
    public static final String SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME = "schedule.svmlistupdate.interval.seconds";
    public static final String SVM_LIST_UPDATE_OFFSET_DEFAULT  = (3*60*60) + "" ; // default 03:00 am, in seconds
    public static final String SVM_LIST_UPDATE_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds


    // scheduler properties
    public static final ConcurrentHashMap<String, Integer> SYNC_FIRST_RUN_OFFSET_SEC = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> SYNC_INTERVAL_SEC = new ConcurrentHashMap<>();
    public static final String[] autostartServices;
    public static Set<String> invalidConfiguredServices = new HashSet<>();

    static {
        Properties props = CommonUtils.loadProperties(ScheduleConstants.class, PROPERTIES_FILE_PATH);

        if(! props.containsKey(CVESEARCH_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ CVESEARCH_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String cveSearchOffset  = props.getProperty(CVESEARCH_OFFSET_PROPERTY_NAME, CVESEARCH_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.CVESEARCH_SERVICE, Integer.parseInt(cveSearchOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + CVESEARCH_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.CVESEARCH_SERVICE);
        }

        if(! props.containsKey(CVESEARCH_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ CVESEARCH_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String cveSearchInterval  = props.getProperty(CVESEARCH_INTERVAL_PROPERTY_NAME, CVESEARCH_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.CVESEARCH_SERVICE, Integer.parseInt(cveSearchInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + CVESEARCH_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.CVESEARCH_SERVICE);
        }

        if(! props.containsKey(SVMSYNC_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ SVMSYNC_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmSyncOffset  = props.getProperty(SVMSYNC_OFFSET_PROPERTY_NAME, SVMSYNC_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.SVMSYNC_SERVICE, Integer.parseInt(svmSyncOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVMSYNC_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVMSYNC_SERVICE);
        }

        if(! props.containsKey(SVMSYNC_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ SVMSYNC_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmSyncInterval  = props.getProperty(SVMSYNC_INTERVAL_PROPERTY_NAME, SVMSYNC_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.SVMSYNC_SERVICE, Integer.parseInt(svmSyncInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVMSYNC_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVMSYNC_SERVICE);
        }

        if(! props.containsKey(SVMMATCH_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ SVMMATCH_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmMatchOffset  = props.getProperty(SVMMATCH_OFFSET_PROPERTY_NAME, SVMMATCH_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.SVMMATCH_SERVICE, Integer.parseInt(svmMatchOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVMMATCH_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVMMATCH_SERVICE);
        }

        if(! props.containsKey(SVMMATCH_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ SVMMATCH_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmMatchInterval  = props.getProperty(SVMMATCH_INTERVAL_PROPERTY_NAME, SVMMATCH_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.SVMMATCH_SERVICE, Integer.parseInt(svmMatchInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVMMATCH_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVMMATCH_SERVICE);
        }

        if(! props.containsKey(SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmListUpdateOffset  = props.getProperty(SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME, SVM_LIST_UPDATE_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.SVM_LIST_UPDATE_SERVICE, Integer.parseInt(svmListUpdateOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVM_LIST_UPDATE_SERVICE);
        }

        if(! props.containsKey(SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String svmListUpdateInterval  = props.getProperty(SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME, SVM_LIST_UPDATE_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.SVM_LIST_UPDATE_SERVICE, Integer.parseInt(svmListUpdateInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.SVM_LIST_UPDATE_SERVICE);
        }

        String autostartServicesString = props.getProperty(AUTOSTART_PROPERTY_NAME, "");
        autostartServices = autostartServicesString.split(",");
    }

}
