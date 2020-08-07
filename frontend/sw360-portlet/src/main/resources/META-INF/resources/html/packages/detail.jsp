<%--
  ~ Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="pkg" type="org.eclipse.sw360.datahandler.thrift.packages.Package" scope="request"/>
    <jsp:useBean id="releaseName" class="java.lang.String" scope="request"/>
 </c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
			<a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab"><liferay-ui:message key="summary" /></a>
		    </div>
	    </div>
	    <div class="col">
		<div class="row portlet-toolbar">
				<div class="col-auto">
                        <div class="btn-toolbar" role="toolbar">
                            <div class="btn-group" role="group">
                                <button type="button" class="btn btn-primary" >Edit Package</button>
                            </div>
                        </div>
				</div>
				<div class="col portlet-title text-truncate" title="${pkg.name}">
					<sw360:out value="${pkg.name}"/>
				</div>
			</div>
			<div class="row">
				<div class="col">
		            <div class="tab-content">
		                <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
		                    <table class="table label-value-table" id="componentOverview">
							    <thead>
							        <tr>
							            <th colspan="2"><liferay-ui:message key="general" /></th>
							        </tr>
							    </thead>
							    <tr>
							        <td><liferay-ui:message key="name" />:</td>
							        <td><sw360:out value="${pkg.name}"/></td>
							    </tr>
							    <tr>
							        <td><liferay-ui:message key="version" />:</td>
							        <td><sw360:out value="${pkg.version}"/></td>
							    </tr>
							    <tr>
							        <td><liferay-ui:message key="vendor" />:</td>
							        <td><sw360:out value="${pkg.vendor}"/></td>
							    </tr>
							    <tr>
							        <td>Package Manager Type:</td>
							        <td><sw360:out value="${pkg.packageManagerType}"/></td>
							    </tr>
							    <tr>
							        <td>VCS:</td>
							        <td><a href="</a><sw360:out value="${pkg.vcsProcessed}"/>" ><sw360:out value="${pkg.vcsProcessed}"/></a></td>
							    </tr>
							    <tr>
							        <td>Homepage Url:</td>
							        <td><a href="</a><sw360:out value="${pkg.homepageUrl}"/>" ><sw360:out value="${pkg.homepageUrl}"/></a></td>
							    </tr>
							    <tr>
							        <td><liferay-ui:message key="main.licenses" />:</td>
							        <td><sw360:out value="${pkg.declaredLicenses}"/></td>
							    </tr>
							    <tr>
							        <td>Linked Component Release:</td>
							        <td><a href="<sw360:DisplayReleaseLink releaseId="${pkg.releaseId}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                                        <sw360:out value="${releaseName}" maxChar="60"/>
                                        </a>
                                    </td>
							    </tr>
							    <tr>
							        <td><liferay-ui:message key="created.on" />:</td>
							        <td><sw360:out value="${pkg.createdOn}"/></td>
							    </tr>
							    <tr>
							        <td><liferay-ui:message key="created.by" />:</td>
							        <td><sw360:DisplayUserEmail email="${pkg.createdBy}"/></td>
							    </tr>
							</table>
		                </div>
		            </div>
		        </div>
		    </div>
        </div>
    </div>
</div>
</core_rt:if>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<script>

	require(['jquery', 'modules/listgroup'], function($, listgroup) {
		listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');
	});
</script>
