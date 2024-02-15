<%--
  ~ Copyright Siemens AG, 2016-2017, 2019, 2021. Part of the SW360 Portal Project.
  ~ With modifications from Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@include file="/html/init.jsp"%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.RequestStatus" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<jsp:useBean id="vulnerabilityList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO>" scope="request"/>
<jsp:useBean id="vulnerabilityRatings" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject>" scope="request"/>
<jsp:useBean id="writeAccessUser" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="vulnerabilityCheckstatusTooltips" type="java.util.Map<java.lang.String, java.lang.String>" scope="request"/>
<jsp:useBean id="vulnerabilityMatchedByHistogram" type="java.util.Map<java.lang.String, java.lang.Long>" scope="request"/>

<jsp:useBean id="viewSize" type="java.lang.Integer" scope="request"/>

<div class="container">
    <core_rt:if test="${project.enableVulnerabilitiesDisplay}">
        <div class="row">
            <div class="col">
                <div class="alert alert-info" role="alert">
                    <liferay-ui:message key="total.vulnerabilities" /> <strong>${numberOfVulnerabilities}</strong>
                </div>
            </div>
        </div>
    </core_rt:if>
    <div class="row mt-2">
        <div class="col">
            <h4><liferay-ui:message key="vulnerability.state.information" /></h4>
        </div>
    </div>
    <div class="row stateinfo">
        <div class="col-3">
            <liferay-ui:message key="security.vulnerability.monitoring" />:
        </div>
        <div class="col">
            <core_rt:if test="${project.enableSvm}">
                <span class="badge badge-success"><liferay-ui:message key="enabled" /></span>
            </core_rt:if>
            <core_rt:if test="${not project.enableSvm}">
                <span class="badge badge-light"><liferay-ui:message key="disabled" /></span>
            </core_rt:if>
        </div>
    </div>
    <div class="row stateinfo">
        <div class="col-3">
            <liferay-ui:message key="security.vulnerabilities.display" />:
        </div>
        <div class="col">
            <core_rt:if test="${project.enableVulnerabilitiesDisplay}">
                <span class="badge badge-success"><liferay-ui:message key="enabled" /></span>
            </core_rt:if>
            <core_rt:if test="${not project.enableVulnerabilitiesDisplay}">
                <span class="badge badge-light"><liferay-ui:message key="disabled" /></span>
            </core_rt:if>
        </div>
    </div>
    <core_rt:if test="${project.enableVulnerabilitiesDisplay}">
        <div class="row mt-4">
            <div class="col">
                <h4><liferay-ui:message key="vulnerabilities" /></h4>
                <form class="form-inline">
                    <table id="vulnerabilityTable_${project.id}" class="table table-bordered" data-view-size="${viewSize}" data-write-access="${writeAccessUser}">
                    </table>
                    <core_rt:if test="${writeAccessUser}">
                        <div class="form-group">
                            <label for="rating-change-for-selected"><liferay-ui:message key="change.rating.and.action.of.selected.vulnerabilities" />
                                <button id="apply-to-selected_${project.id}" type="button" class="btn btn-primary"><liferay-ui:message key="change" /></button>
                            </label>
                        </div>
                    </core_rt:if>
                </form>
            </div>
        </div>
        <div class="row mt-4">
            <div class="col">
                <h4><liferay-ui:message key="vulnerability.matching.statistics" /></h4>
                <ul>
                    <core_rt:forEach items="${vulnerabilityMatchedByHistogram.keySet()}" var="matchedBy">
                        <li>
                            <b><sw360:out value='${vulnerabilityMatchedByHistogram.get(matchedBy)}'/></b>
                            of the vulnerabilities were matched by
                            <b><sw360:out value='${matchedBy}'/></b>
                        </li>
                    </core_rt:forEach>
                </ul>
            </div>
        </div>
    </core_rt:if>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<script type="text/javascript">
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'bridges/jquery-ui'], function ($, datatable, dialog) {
        var vulnerabilityTable;

        vulnerabilityTable = createVulnerabilityTable();
        $('#vulnerabilityTable_${project.id}').on('change', '[data-action="select-all"]', function(event) {
            if($(event.currentTarget).is(':checked')) {
                vulnerabilityTable.rows().select();
            } else {
                vulnerabilityTable.rows().deselect();
            }
        });
        $('#vulnerabilityTable_${project.id}').on('change', 'td :checkbox', function(event) {
            $('#vulnerabilityTable th [data-action="select-all"]').prop('checked', !($('#vulnerabilityTable td :checkbox:not(:checked)').length > 0));
        });

        vulnerabilityTable.on('preDraw', function() {
            $('[role="tooltip"]').css("display", "none");
            $('#vulnerabilityTable_${project.id} .info-text').tooltip("close");
        });

        vulnerabilityTable.on('draw', function() {
            $('[role="tooltip"]').css("display", "none");
            $('#vulnerabilityTable_${project.id} .info-text').tooltip("close");
        });

        var viewSize = $('#vulnerabilityTable_${project.id}').data() ? $('#vulnerabilityTable_${project.id}').data().viewSize : 0;
        $('#btnShowVulnerabilityCount [data-name="count"]').text(viewSize > 0 ? '<liferay-ui:message key="latest" /> ' + viewSize : '<liferay-ui:message key="all" />');
        $('#btnShowVulnerabilityCount + div > a').on('click', function(event) {
            var viewSize = $(event.currentTarget).data().type;
            <core_rt:if test="${not empty listVulnerabilityWithViewSizeFriendlyUrl}">
                let listVulnerabilityWithViewSizeUrl = "${listVulnerabilityWithViewSizeFriendlyUrl}";
                listVulnerabilityWithViewSizeUrl = listVulnerabilityWithViewSizeUrl.replace("replacewithviewsize", viewSize)
                window.location.href = listVulnerabilityWithViewSizeUrl + window.location.hash;
            </core_rt:if>
        });

        if($('#vulnerabilityTable_${project.id}').data() && $('#vulnerabilityTable_${project.id}').data().writeAccess) {
            $('#apply-to-selected_${project.id}').on('click', function () {
                batchChangeRating();
            });
        }

        function displayVulnerabilityLink(extId) {
            let vulfriendlyUrl = "${vulfriendlyUrl}";
            let vulUrl = vulfriendlyUrl.replace("replacewithexternalid", extId);
            return $("<a></a>").attr("href", vulUrl).text(extId)[0].outerHTML;
        }

        function createVulnerabilityTable() {
            var table,
                tableDefinition,
                result = [];

            <core_rt:if test="${project.enableVulnerabilitiesDisplay and not empty vulnerabilityList}">
                <core_rt:forEach items="${vulnerabilityList}" var="vulnerability">
                    result.push({
                        releaseName: "<sw360:out value='${vulnerability.intReleaseName}'/>",
                        externalId: "${vulnerability.externalId}",
                        <core_rt:if test="${not isSubProject}">
                            externalIdLink: "<sw360:DisplayVulnerabilityLink vulnerabilityId="${vulnerability.externalId}"/>",
                        </core_rt:if>
                        <core_rt:if test="${isSubProject}">
                            externalIdLink: displayVulnerabilityLink("${vulnerability.externalId}"),
                        </core_rt:if>
                        intReleaseId: "<sw360:out value="${vulnerability.intReleaseId}"/>",
                        priority: {
                            text: "<sw360:out value='${vulnerability.priority}'/>",
                            tooltip: "<sw360:out value='${vulnerability.priorityToolTip}'/>"
                        },
                        title: {
                            text: "<sw360:out value='${vulnerability.title}' jsQuoting='true'/>",
                            tooltip: "<sw360:out value='${vulnerability.description}' jsQuoting='true'/>"
                        },
                        matchedBy: {
                            text: "<sw360:out value='${vulnerability.matchedBy}'/>",
                            tooltip: "Found with needle: <sw360:out value='${vulnerability.usedNeedle}'/>"
                        },
                        relevance: {
                            text: "<sw360:DisplayEnum value="${vulnerabilityRatings.get(vulnerability.externalId).get(vulnerability.intReleaseId)}"/>",
                            tooltip: "<sw360:out value="${vulnerabilityCheckstatusTooltips.get(vulnerability.externalId).get(vulnerability.intReleaseId)}"/>"
                        },
                        action: "<sw360:out value="${vulnerabilityActions.get(vulnerability.externalId).get(vulnerability.intReleaseId)}"/>"
                    });
                </core_rt:forEach>
            </core_rt:if>

            tableDefinition = {
                data: result,
                columns: [
                    { title: '', data: null, className: 'text-center', render: $.fn.dataTable.render.inputCheckbox('vulnerability', '') },
                    { title: "<liferay-ui:message key="release" />", data: 'releaseName' },
                    { title: "<liferay-ui:message key="external.id" />", data: 'externalIdLink' },
                    { title: "<liferay-ui:message key="priority" />", data: 'priority', render: $.fn.dataTable.render.infoText('/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open') },
                    { title: "<liferay-ui:message key="matched.by" />", data: 'matchedBy', render: $.fn.dataTable.render.infoText() },
                    { title: "<liferay-ui:message key="title" />", data: 'title', render: $.fn.dataTable.render.infoText() },
                    { title: "<liferay-ui:message key="relevance.for.project" />", data: 'relevance', render: $.fn.dataTable.render.infoText('/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open') },
                    { title: "<liferay-ui:message key="actions" />", data: 'action', default: '' }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                order: [[3, 'asc'], [4, 'desc']],
            }

            if($('#vulnerabilityTable_${project.id}').data() && $('#vulnerabilityTable_${project.id}').data().writeAccess) {
                tableDefinition.select = 'multi+shift';
                tableDefinition.columns[0].title = '<div class="form-check"><input data-action="select-all" type="checkbox" class="form-check-input" /></div>';
            }

            table = datatable.create('#vulnerabilityTable_${project.id}', tableDefinition, [1, 2, 3, 4, 5, 6, 7], [0], true);
            if($('#vulnerabilityTable_${project.id}').data() && $('#vulnerabilityTable_${project.id}').data().writeAccess) {
                $("#vulnerabilityTable_${project.id}").on('init.dt', function() {
                   datatable.enableCheckboxForSelection(table, 0);
                });
            }

            return table;
        }

        <core_rt:if test="${writeAccessUser}">
            function batchChangeRating() {
                var selectedRows = vulnerabilityTable.rows('.selected');
                $("#action").removeClass("d-none");
                $("#rating").removeClass("d-none");
                var $dialog = dialog.open('#vulnerabilityDialog', {
                    button: 'Change Rating',
                    vulnerabilityCount: selectedRows.data().length,
                    /* verification: newValueText */
                }, function(submit, callback, data) {
                    var selectedValue = $("#rating-change-for-selected").children("option:selected");
                    var newValue = selectedValue.val();
                    var newValueText = selectedValue.text();
                    executeRatingChange(selectedRows, newValue, newValueText, data.comment, data.action).then(function() {
                        callback(true);
                    }).catch(function(error) {
                        $dialog.alert(error);
                        callback();
                    });
                });
            }

            function executeRatingChange(selectedRows, newValue, newValueText, comment, action) {
                var vulnerabilityIds = [];
                var releaseIds = [];

                selectedRows.data().each(function (item) {
                    vulnerabilityIds.push(item['externalId']);
                    releaseIds.push(item['intReleaseId']);
                });

                var data = {};
            
                data["<portlet:namespace/><%=PortalConstants.ACTUAL_PROJECT_ID%>"] = "${project.id}";
                data["<portlet:namespace/><%=PortalConstants.VULNERABILITY_IDS%>"] = vulnerabilityIds;
                data["<portlet:namespace/><%=PortalConstants.RELEASE_IDS%>"] = releaseIds;
                data["<portlet:namespace/><%=PortalConstants.VULNERABILITY_RATING_VALUE%>"] = newValue;
                data["<portlet:namespace/><%=PortalConstants.VULNERABILITY_RATING_COMMENT%>"] = comment;
                data["<portlet:namespace/><%=PortalConstants.VULNERABILITY_RATING_ACTION%>"] = action;

                return new Promise(function(resolve, reject) {
                    $.ajax({
                        url: '${updateVulnerabilityRatings}',
                        type: 'POST',
                        dataType: 'json',
                        data: data,
                        success: function(response){
                            switch (response.<%=PortalConstants.REQUEST_STATUS%>) {
                                case '<%=RequestStatus.FAILURE%>':
                                    reject("Update failed.");
                                    break;
                                case '<%=RequestStatus.SUCCESS%>':
                                    $('#numberOfVulnerabilities').addClass('bg-warning');
                                    $('#numberOfVulnerabilities').html('&#8634;');

                                    selectedRows.every(function () {
                                        this.data().relevance = {
                                            text: newValueText,
                                            tooltip: "<liferay-ui:message key="you.just.changed.this.value.please.reload.page" />"
                                        };
                                        this.data().action = action;
                                        this.invalidate();
                                    });
                                    selectedRows.deselect();

                                    resolve();
                                    break;
                                default:
                            }
                        },
                        error: function() {
                            reject('<liferay-ui:message key="unknown.request.error" />');
                        }
                    });
                });
            }
        </core_rt:if>
    });
</script>
