<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %><%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
  ~ With modifications by Siemens AG, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<jsp:useBean id='cveSearchIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='anyServiceIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='cvesearchOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='cvesearchInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='cvesearchNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmSyncIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmSyncOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmSyncInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmSyncNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmMatchOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmListUpdateOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='trackingFeedbackOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackNextSync' type="java.lang.String" scope="request"/>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="scheduleCvesearchURL" name="scheduleCveSearch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleCvesearchURL" name="unscheduleCveSearch">
</portlet:actionURL>

<portlet:actionURL var="scheduleSvmSyncURL" name="scheduleSvmSync">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmSyncURL" name="unscheduleSvmSync">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmSyncURL" name="triggerSvmSync">
</portlet:actionURL>

<portlet:actionURL var="scheduleSvmMatchURL" name="scheduleSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmMatchURL" name="unscheduleSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmMatchURL" name="triggerSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="scheduleSvmListUpdateURL" name="scheduleSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmListUpdateURL" name="unscheduleSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmListUpdateURL" name="triggerSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="scheduleTrackingFeedbackURL" name="scheduleTrackingFeedback">
</portlet:actionURL>

<portlet:actionURL var="unscheduleTrackingFeedbackURL" name="unscheduleTrackingFeedback">
</portlet:actionURL>

<portlet:actionURL var="triggerTrackingFeedbackURL" name="triggerTrackingFeedback">
</portlet:actionURL>

<portlet:actionURL var="unscheduleAllServicesURL" name="unscheduleAllServices">
</portlet:actionURL>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Schedule Task Administration</span> </p>

<h4 class="withTopMargin">CVE Search: </h4>
<br/>
<b>Settings for scheduling the CVE search service:</b><br/>
Offset: ${cvesearchOffset} (hh:mm:ss)<br/>
Interval: ${cvesearchInterval} (hh:mm:ss)<br/>
Next Synchronization: ${cvesearchNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${cveSearchIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not cveSearchIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleCvesearchURL%>'"</core_rt:if>
       value="Schedule CveSearch Updates">

<input type="button"
       <core_rt:if test="${cveSearchIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleCvesearchURL%>'"</core_rt:if>
       <core_rt:if test="${not cveSearchIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled CveSearch Updates">

<h4 class="withTopMargin">SVM Vulnerabilities Sync</h4>
<br/>
<b>Settings for scheduling the SVM Sync service:</b><br/>
Offset: ${svmSyncOffset} (hh:mm:ss)<br/>
Interval: ${svmSyncInterval} (hh:mm:ss)<br/>
Next Synchronization: ${svmSyncNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${svmSyncIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not svmSyncIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleSvmSyncURL%>'"</core_rt:if>
       value="Schedule SVM Sync"/>

<input type="button"
       <core_rt:if test="${svmSyncIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleSvmSyncURL%>'"</core_rt:if>
       <core_rt:if test="${not svmSyncIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled SVM Sync"/>

<h4 class="withTopMargin">SVM Vulnerabilities Reverse Match</h4>
<br/>
<b>Settings for scheduling the SVM Reverse Match service:</b><br/>
Offset: ${svmMatchOffset} (hh:mm:ss)<br/>
Interval: ${svmMatchInterval} (hh:mm:ss)<br/>
Next Synchronization: ${svmMatchNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${svmMatchIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not svmMatchIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleSvmMatchURL%>'"</core_rt:if>
       value="Schedule SVM Reverse Match"/>

<input type="button"
       <core_rt:if test="${svmMatchIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleSvmMatchURL%>'"</core_rt:if>
       <core_rt:if test="${not svmMatchIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled SVM Reverse Match"/>

<h4 class="withTopMargin">SVM Monitoring List Update</h4>
<br/>
<b>Settings for scheduling the SVM Monitoring List Update service:</b><br/>
Offset: ${svmListUpdateOffset} (hh:mm:ss)<br/>
Interval: ${svmListUpdateInterval} (hh:mm:ss)<br/>
Next Synchronization: ${svmListUpdateNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${svmListUpdateIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not svmListUpdateIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleSvmListUpdateURL%>'"</core_rt:if>
       value="Schedule SVM Monitoring List Update"/>

<input type="button"
       <core_rt:if test="${svmListUpdateIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleSvmListUpdateURL%>'"</core_rt:if>
       <core_rt:if test="${not svmListUpdateIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled SVM Monitoring List Update"/>

<h4 class="withTopMargin">SVM Release Tracking Feedback</h4>
<br/>
<b>Settings for scheduling the SVM Release Tracking Feedback service:</b><br/>
Offset: ${trackingFeedbackOffset} (hh:mm:ss)<br/>
Interval: ${trackingFeedbackInterval} (hh:mm:ss)<br/>
Next Synchronization: ${trackingFeedbackNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${trackingFeedbackIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not trackingFeedbackIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleTrackingFeedbackURL%>'"</core_rt:if>
       value="Schedule SVM Release Tracking Feedback"/>

<input type="button"
       <core_rt:if test="${trackingFeedbackIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleTrackingFeedbackURL%>'"</core_rt:if>
       <core_rt:if test="${not trackingFeedbackIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled SVM Release Tracking Feedback"/>

<h4 class="withTopMargin">All Services:</h4>

<input type="button"
       <core_rt:if test="${anyServiceIsScheduled}">class="addButton" onclick="window.location.href='<%=unscheduleAllServicesURL%>'" </core_rt:if>
       <core_rt:if test="${not anyServiceIsScheduled}">class="notApplicableButton" </core_rt:if>
       value="Cancel All Scheduled Tasks"/>

<h4 class="withTopMargin">Manual triggering of scheduled services</h4>
<input type="button" class="addButton" onclick="window.location.href='<%=triggerSvmSyncURL%>'" value="SVM Vulnerabilities Sync"/>
<input type="button" class="addButton" onclick="window.location.href='<%=triggerSvmMatchURL%>'" value="SVM Vulnerabilities Reverse Match"/>
<input type="button" class="addButton" onclick="window.location.href='<%=triggerSvmListUpdateURL%>'" value="SVM Monitoring List Update"/>
<input type="button" class="addButton" onclick="window.location.href='<%=triggerTrackingFeedbackURL%>'" value="SVM Release Tracking Feedback"/>
