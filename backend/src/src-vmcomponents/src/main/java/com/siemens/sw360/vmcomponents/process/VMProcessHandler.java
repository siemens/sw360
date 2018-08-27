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
package com.siemens.sw360.vmcomponents.process;

import com.siemens.sw360.datahandler.thrift.vmcomponents.VMComponent;
import com.siemens.sw360.vmcomponents.common.SVMConstants;
import com.siemens.sw360.vmcomponents.handler.SVMSyncHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertTrue;

/**
 * Created by stefan.jaeger on 10.03.16.
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMProcessHandler {
    private static final Logger log = getLogger(VMProcessHandler.class);

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                                                                    SVMConstants.PROCESSING_CORE_POOL_SIZE,
                                                                    SVMConstants.PROCESSING_MAX_POOL_SIZE,
                                                                    SVMConstants.PROCESSING_KEEP_ALIVE_SECONDS,
                                                                    TimeUnit.SECONDS,
                                                                    new PriorityBlockingQueue<>());

    /**
     * it is very important to use ConcurrentHashMap to be threadsafe
     */
    private static final ConcurrentHashMap<Class<?>, List<SVMSyncHandler>> syncHandlersFree = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SVMSyncHandler> syncHandlersBusy = new ConcurrentHashMap<>(SVMConstants.PROCESSING_CORE_POOL_SIZE);

//    private static final ConcurrentHashMap<Reporting, Integer> processReporting = new ConcurrentHashMap<>();
//    private static Date processStart;
//    private static Date processEnd;
//    private static long processingTime;

    private VMProcessHandler(){}

    public static <T extends TBase> void getVulnerabilitiesByComponentId(String componentId, String url, boolean triggerVulMasterData){
        try {
            queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void getVulnerabilitiesByComponentIds(Collection<String> componentIds, String url, boolean triggerVulMasterData){
        if (componentIds != null && !componentIds.isEmpty()){
            for (String componentId : componentIds) {
                try {
                    queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void findComponentMatch(String componentId, boolean triggerGettingVulnerabilities){
        if (!StringUtils.isEmpty(componentId)){
            try {
                queueing(VMComponent.class, componentId, VMProcessType.MATCH_SVM, null, triggerGettingVulnerabilities);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static void findReleaseMatch(String releaseId, boolean triggerGettingVulnerabilities){
        if (!StringUtils.isEmpty(releaseId)){
            try {
                queueing(Release.class, releaseId, VMProcessType.MATCH_SW360, null, triggerGettingVulnerabilities);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, Collection<String> elementIds, String url, boolean triggerMatchCpe){
        if (elementIds != null && elementIds.size()>0){
            String time = SW360Utils.getCreatedOnTime();
            for (String elementId : elementIds) {
                getMasterData(elementType, elementId, url, triggerMatchCpe);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, String elementId, String url, boolean triggerMatchCpe){
        if (!StringUtils.isEmpty(elementId)){
            try {
                queueing(elementType, elementId, VMProcessType.MASTER_DATA, url, triggerMatchCpe);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void storeElements(Class<T> elementType, Collection<String> elementIds, String url, boolean triggerMasterData){
        if (elementIds != null && elementIds.size()>0){
            for (String elementId : elementIds) {
                try {
                    queueing(elementType, elementId, VMProcessType.STORE_NEW, url, triggerMasterData);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static <T extends TBase> void cleanupMissingElements(Class<T> elementType, List<String> elementIds){
        try {
            queueing(elementType, elementIds, VMProcessType.CLEAN_UP, null, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void getElementIds(Class<T> elementType, String url, boolean triggerStoring){
        try {
            queueing(elementType, "", VMProcessType.GET_IDS, url, triggerStoring);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void triggerReport(Class<T> elementType, String startTime){
        try {
            queueing(elementType, startTime, VMProcessType.FINISH, null, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, String input, VMProcessType task, String url, boolean triggerNextStep) throws SW360Exception {
        queueing(elementType, input == null ? Collections.emptyList() : Collections.singletonList(input), task, url, triggerNextStep);
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, List<String> input, VMProcessType task, String url, boolean triggerNextStep) throws SW360Exception {
        assertNotNull(elementType);
        assertNotNull(input);
        assertTrue(!input.isEmpty());
        assertNotNull(task);

        switch(task){
            case GET_IDS:
            case CLEAN_UP:
            case STORE_NEW:
            case MASTER_DATA:
            case MATCH_SVM:
            case MATCH_SW360:
            case VULNERABILITIES:
            case FINISH:
                VMProcessor<T> processor = new VMProcessor<>(elementType, input, task, url, triggerNextStep);
                executor.execute(processor);
                break;

            default: throw new IllegalArgumentException("unknown task '"+task+"'. do not know what to do :( ");
        }
    }


    public static <T extends TBase> void giveSyncHandlerBack(String syncHandlerId) throws MalformedURLException, SW360Exception {
        handleSyncHandler(null, syncHandlerId, ProcessTask.FINISH);
    }
    public static <T extends TBase> void destroySyncHandler(String syncHandlerId) throws MalformedURLException, SW360Exception {
        handleSyncHandler(null, syncHandlerId, ProcessTask.ERROR);
    }

    public static <T extends TBase> SVMSyncHandler getSyncHandler(Class<T> elementType) throws MalformedURLException, SW360Exception {
        return handleSyncHandler(elementType, null, ProcessTask.START);
    }

    private enum ProcessTask{
        START,
        FINISH,
        ERROR
    }

    /**
     * this important method have to be threadsafe(synchronized) to handle the different handlers is a safe way only this method is allowed to exit the ConcurrentHashMaps
     * @param elementType the specific type
     * @param syncHandlerId uuid of the handler
     * @param task trigger for making a {@link SVMSyncHandler} available or getting a used one back
     * @param <T> specification of the element type
     * @return on ProcessTask.START the available {@link SVMSyncHandler} will be returned, otherwise NULL
     * @throws SW360Exception
     * @throws MalformedURLException
     */
    private static synchronized <T extends TBase> SVMSyncHandler handleSyncHandler(Class<T> elementType, String syncHandlerId, ProcessTask task) throws SW360Exception, MalformedURLException {
        assertNotNull(task);
        switch (task){
            case START:
                assertNotNull(elementType);
                SVMSyncHandler<T> syncHandler = null;
                if (!syncHandlersFree.isEmpty()){
                    List<SVMSyncHandler> syncHandlers = syncHandlersFree.get(elementType);
                    if(syncHandlers != null && syncHandlers.size() > 0){
                        syncHandler = syncHandlers.remove(0);
                    }
                }
                if (syncHandler == null){
                    syncHandler = new SVMSyncHandler<T>(elementType);
                }
                syncHandlersBusy.put(syncHandler.getUuid(), syncHandler);
                return syncHandler;

            case FINISH:
                assertNotNull(syncHandlerId);
                syncHandler = syncHandlersBusy.remove(syncHandlerId);
                if (syncHandler != null){
                    List<SVMSyncHandler> syncHandlers = syncHandlersFree.get(syncHandler.getType());
                    if (syncHandlers == null){
                        syncHandlers = new ArrayList<>();
                        syncHandlersFree.put(syncHandler.getType(), syncHandlers);
                    }
                    syncHandlers.add(syncHandler);
                }
                return null;

            case ERROR:
                assertNotNull(syncHandlerId);
                syncHandlersBusy.remove(syncHandlerId);
                return null;

            default: throw new IllegalArgumentException("unknown task '"+task+"'. do not know what to do :( ");
        }
    }

//    private static synchronized void reporting(ProcessTask task, Reporting reporting) throws SW360Exception {
//        assertNotNull(task);
//        switch(task){
//            case START:
//                processReporting.clear();
//                processStart = new Date();
//                processEnd = null;
//                processingTime = 0L;
//                break;
//            case FINISH:
//                assertNotNull(reporting);
//                Integer count = processReporting.get(reporting);
//                if (count == null){
//                    count = 0;
//                }
//                count++;
////                pro
//                break;
//
//        }
//    }

    // Reporting:   GetIds: #totalGet #error
    //              StoreNew: #totalNew #new #error
    //              Cleanup: #deleted #error
    //              MD: #totalMD #updated #error
    //              Match: #totalMatch #MatchCPE #MatchL3/L2/L1 #error
    //              Vul: #total #newvul #updated #deleted #error
//    private enum ReportingType{
//        TOTAL,
//        ERROR,
//        NEW,
//        DELETED,
//        UPDATED,
//        IN_PROGRESS
//    }

//    private class Reporting{
//        public Reporting(Class<?> elementType, VMMatchType matchType) {
//            this.elementType = elementType;
//            this.matchType = matchType;
//        }
//
//        public Reporting(Class<?> elementType, ReportingType reportingType, VMProcessType processType) {
//
//            this.elementType = elementType;
//            this.reportingType = reportingType;
//            this.processType = processType;
//        }
//
//        Class<?> elementType = null;
//        ReportingType reportingType = null;
//        VMProcessType processType = null;
//        VMMatchType matchType = null;
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            Reporting reporting = (Reporting) o;
//
//            if (elementType != null ? !elementType.equals(reporting.elementType) : reporting.elementType != null)
//                return false;
//            if (reportingType != reporting.reportingType) return false;
//            if (processType != reporting.processType) return false;
//            return matchType == reporting.matchType;
//
//        }
//
//        @Override
//        public int hashCode() {
//            int result = elementType != null ? elementType.hashCode() : 0;
//            result = 31 * result + (reportingType != null ? reportingType.hashCode() : 0);
//            result = 31 * result + (processType != null ? processType.hashCode() : 0);
//            result = 31 * result + (matchType != null ? matchType.hashCode() : 0);
//            return result;
//        }
//
//        @Override
//        public String toString() {
//            final StringBuffer sb = new StringBuffer("Reporting{");
//            sb.append("elementType=").append(elementType);
//            sb.append(", reportingType=").append(reportingType);
//            sb.append(", processType=").append(processType);
//            sb.append(", matchType=").append(matchType);
//            sb.append('}');
//            return sb.toString();
//        }
//    }

}
