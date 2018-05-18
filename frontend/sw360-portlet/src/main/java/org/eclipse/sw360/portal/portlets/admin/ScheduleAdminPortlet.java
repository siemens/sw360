/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;
import java.io.IOException;

public class ScheduleAdminPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(ScheduleAdminPortlet.class);


    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        prepareStandardView(request, response);
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request, RenderResponse response) {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            ScheduleService.Iface scheduleClient = new ThriftClients().makeScheduleClient();

            boolean isAnyServiceScheduled = isAnyServiceScheduled(scheduleClient, user);
            request.setAttribute(PortalConstants.ANY_SERVICE_IS_SCHEDULED, isAnyServiceScheduled);

            boolean isCveSearchScheduled = isServiceScheduled(ThriftClients.CVESEARCH_SERVICE, scheduleClient, user);
            request.setAttribute(PortalConstants.CVESEARCH_IS_SCHEDULED, isCveSearchScheduled);
            int offsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_OFFSET, CommonUtils.formatTime(offsetInSeconds));
            int intervalInSeconds = scheduleClient.getInterval(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_INTERVAL, CommonUtils.formatTime(intervalInSeconds));
            String nextSync = scheduleClient.getNextSync(ThriftClients.CVESEARCH_SERVICE);
            request.setAttribute(PortalConstants.CVESEARCH_NEXT_SYNC, nextSync);

            boolean isSvmSyncScheduled = isServiceScheduled(ThriftClients.SVMSYNC_SERVICE, scheduleClient, user);
            request.setAttribute(PortalConstants.SVMSYNC_IS_SCHEDULED, isSvmSyncScheduled);
            int svmSyncOffsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.SVMSYNC_SERVICE);
            request.setAttribute(PortalConstants.SVMSYNC_OFFSET, CommonUtils.formatTime(svmSyncOffsetInSeconds));
            int svmSyncIntervalInSeconds = scheduleClient.getInterval(ThriftClients.SVMSYNC_SERVICE);
            request.setAttribute(PortalConstants.SVMSYNC_INTERVAL, CommonUtils.formatTime(svmSyncIntervalInSeconds));
            String svmSyncNextSync = scheduleClient.getNextSync(ThriftClients.SVMSYNC_SERVICE);
            request.setAttribute(PortalConstants.SVMSYNC_NEXT_SYNC, svmSyncNextSync);

            boolean isSvmMatchScheduled = isServiceScheduled(ThriftClients.SVMMATCH_SERVICE, scheduleClient, user);
            request.setAttribute(PortalConstants.SVMMATCH_IS_SCHEDULED, isSvmMatchScheduled);
            int svmMatchOffsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.SVMMATCH_SERVICE);
            request.setAttribute(PortalConstants.SVMMATCH_OFFSET, CommonUtils.formatTime(svmMatchOffsetInSeconds));
            int svmMatchIntervalInSeconds = scheduleClient.getInterval(ThriftClients.SVMMATCH_SERVICE);
            request.setAttribute(PortalConstants.SVMMATCH_INTERVAL, CommonUtils.formatTime(svmMatchIntervalInSeconds));
            String svmMatchNextSync = scheduleClient.getNextSync(ThriftClients.SVMMATCH_SERVICE);
            request.setAttribute(PortalConstants.SVMMATCH_NEXT_SYNC, svmMatchNextSync);

            boolean isSvmListUpdateScheduled = isServiceScheduled(ThriftClients.SVM_LIST_UPDATE_SERVICE, scheduleClient, user);
            request.setAttribute(PortalConstants.SVM_LIST_UPDATE_IS_SCHEDULED, isSvmListUpdateScheduled);
            int svmListUpdateOffsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.SVM_LIST_UPDATE_SERVICE);
            request.setAttribute(PortalConstants.SVM_LIST_UPDATE_OFFSET, CommonUtils.formatTime(svmListUpdateOffsetInSeconds));
            int svmListUpdateIntervalInSeconds = scheduleClient.getInterval(ThriftClients.SVM_LIST_UPDATE_SERVICE);
            request.setAttribute(PortalConstants.SVM_LIST_UPDATE_INTERVAL, CommonUtils.formatTime(svmListUpdateIntervalInSeconds));
            String svmListUpdateNextSync = scheduleClient.getNextSync(ThriftClients.SVM_LIST_UPDATE_SERVICE);
            request.setAttribute(PortalConstants.SVM_LIST_UPDATE_NEXT_SYNC, svmListUpdateNextSync);
        } catch (TException te) {
            log.error(te.getMessage());
        }

    }

    private boolean isServiceScheduled(String serviceName, ScheduleService.Iface scheduleClient, User user) throws TException{
            RequestStatusWithBoolean requestStatus = scheduleClient.isServiceScheduled(serviceName, user);
            if(RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())){
                return requestStatus.isAnswerPositive();
            } else {
                throw new SW360Exception("Backend query for schedule status of cvesearch failed.");
            }
    }

    private boolean isAnyServiceScheduled(ScheduleService.Iface scheduleClient, User user) throws TException{
        RequestStatusWithBoolean requestStatus = scheduleClient.isAnyServiceScheduled(user);
        if(RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())){
            return requestStatus.isAnswerPositive();
        } else {
            throw new SW360Exception("Backend query for schedule status of services failed.");
        }
    }

    @UsedAsLiferayAction
    public void scheduleCveSearch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.CVESEARCH_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleCveSearch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.CVESEARCH_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void scheduleSvmSync(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.SVMSYNC_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleSvmSync(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.SVMSYNC_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void scheduleSvmMatch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.SVMMATCH_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleSvmMatch(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.SVMMATCH_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void scheduleSvmListUpdate(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.SVM_LIST_UPDATE_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    /**
     * This action is for manually triggering export to SVM for monitoring lists (as opposed to scheduled execution)
     * The action is performed _synchronously_!
     *
     * @param request
     * @param response
     * @throws PortletException
     * @throws IOException
     */
    @UsedAsLiferayAction
    public void triggerSvmListUpdate(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus = new ThriftClients().makeProjectClient().exportForMonitoringList();
            setSessionMessage(request, requestStatus, "Task", "perform");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleSvmListUpdate(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.SVM_LIST_UPDATE_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }

    @UsedAsLiferayAction
    public void unscheduleAllServices(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleAllServices(user);
            setSessionMessage(request, requestStatus, "Every task", "unschedule");
        } catch (TException e) {
            log.error(e);
        }
    }
}
