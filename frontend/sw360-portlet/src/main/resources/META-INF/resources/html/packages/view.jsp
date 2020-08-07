<%--
  ~ Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
  - With contributions by Bosch Software Innovations GmbH, 2016-2017.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.PackageManagerType" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<jsp:useBean id="packageList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.packages.Package>" class="java.util.ArrayList" scope="request" />

<portlet:renderURL var="friendlyPackageURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.PACKAGE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</portlet:renderURL>

<liferay-portlet:renderURL var="friendlyLicenseURL" portletName="sw360_portlet_licenses">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<portlet:resourceURL var="packagesURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_COMPONENT_LIST%>'/>
</portlet:resourceURL>

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
				<div id="searchInput" class="card">
					<div class="card-header">
						<liferay-ui:message key="advanced.search" />
					</div>
                    <div class="card-body">
                        <form method="post">
                            <div class="form-group">
                                <label for="package_name">Package Name</label>
                                <input type="text" class="form-control form-control-sm"
                                    value="" id="package_name">
                            </div>
                            <div class="form-group">
                                <label for="package_type">Package Type</label>
                                <select class="form-control form-control-sm" id="package_type" name="<portlet:namespace/><%=Package._Fields.PACKAGE_MANAGER_TYPE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                    <sw360:DisplayEnumOptions type="<%=PackageManagerType.class%>" selectedName="${packageManagerType}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="main_licenses">Main Licenses</label>
                                <input type="text" class="form-control form-control-sm"
                                    value="" id="main_licenses">
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block"><liferay-ui:message key="search" /></button>
				        </form>
					</div>
				</div>
			</div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
							<button type="button" class="btn btn-primary">Add Package</button>
							<button type="button" class="btn btn-secondary" data-action="import-packages">Import Packages BOM</button>
						</div>
						<div id="btnExportGroup" class="btn-group" role="group">
							<button id="btnExport" type="button" class="btn btn-secondary">Export Package SPDX</button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="packages" />">
					<liferay-ui:message key="packages" />
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="packagesTable" class="table table-bordered"></table>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs"></div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ include file="/html/utils/includes/importPackages.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render' ], function($, autocomplete, dialog, datatables, render) {

                var tableData = [],
                    licenses;
                <core_rt:forEach items="${packageList}" var="pkg">
                    licenses = [];
                    <c:set var = "declaredLicenses" value = "" />

                    <core_rt:if test="${not empty pkg.declaredLicenses}">
                    <core_rt:forEach items="${pkg.declaredLicenses}" var="license">
                        licenses.push("<sw360:out value='${license}'/>");
                    </core_rt:forEach >
                    </core_rt:if>
                    <c:set var = "lic" value = "${licenses}" />
                	tableData.push({
                        "DT_RowId": "${pkg.id}",
                        "0": "",
                        "1": "<sw360:out value='${pkg.name} (${pkg.version})'/>",
                        "2": "${pkg.declaredLicenses}",
                        "3": "<sw360:DisplayEnum value="${pkg.packageManagerType}"/>",
                        "4": "",
                        "5": "${lic}"
                    });
                </core_rt:forEach>

            var packagesTable = createPackagesTable();
            // create and render data table
            function createPackagesTable() {
                let columns = [
                    {"title": "<liferay-ui:message key="vendor" />", width: "15%"},
                    {"title": "Package Name", render: {display: renderPackageNameLink}, width: "45%"},
                    {"title": "<liferay-ui:message key="main.licenses" />", render: {display: renderLicenseLink}, width: "20%"},
                    {"title": "Package Type", width: "10%"},
                    {"title": "<liferay-ui:message key="actions" />", render: {display: renderPackageActions}, className: 'two actions', orderable: false, width: "10%"}
                ];
                let printColumns = [0, 1, 2, 3];
                var packagesTable = datatables.create('#packagesTable', {
                    searching: true,
                    deferRender: false, // do not change this value
                    data: tableData,
                    columns: columns,
                    initComplete: datatables.showPageContainer,
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    order: [
                        [1, 'asc']
                    ]
                }, printColumns);

                return packagesTable;
            }

            function renderLicenseLink(lics, type, row) {
                lics = lics.replace(/[\[\]]+/g,'');
                lics = lics.split(",");
                var links = [],
                    licensePortletURL = '<%=friendlyLicenseURL%>'
                    .replace(/packages/g, "licenses");// DIRTY WORKAROUND

                for (var i = 0; i < lics.length; i++) {
                    links[i] = render.linkTo(replaceFriendlyUrlParameter(licensePortletURL.toString(), lics[i], '<%=PortalConstants.PAGENAME_DETAIL%>'), lics[i]);
                }

                if(type == 'display') {
                    return links.join(', ');
                } else if(type == 'print') {
                    return lics.join(', ');
                } else if(type == 'type') {
                    return 'string';
                } else {
                    return lics.join(', ');
                }
            }
            
            // helper functions
            function makePackageUrl(packageId, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                    .setParameter('<%=PortalConstants.PAGENAME%>', page)
                    .setParameter('<%=PortalConstants.PACKAGE_ID%>', packageId);
                return portletURL.toString();
            }

            function renderPackageActions(id, type, row) {
                var $actions = $('<div>', {
				    'class': 'actions'
                    }),
                    $editAction = $('<svg>', {
                        'class': 'edit lexicon-icon',
                    }),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                    });

                $editAction.append($('<title><liferay-ui:message key="edit" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/>'));
                $deleteAction.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                $actions.append($editAction, $deleteAction);
                return $actions[0].outerHTML;
            }

            function renderPackageNameLink(data, type, row) {
                return render.linkTo(makePackageUrl(row.DT_RowId, '<%=PortalConstants.PAGENAME_DETAIL%>'), row[1]);
            }

            function replaceFriendlyUrlParameter(portletUrl, id, page) {
                return portletUrl
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
            }
        });
    });
</script>