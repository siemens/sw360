/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.packages;

import static org.eclipse.sw360.portal.common.PortalConstants.ATTACHMENT_CONTENT_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGES_PORTLET_NAME;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGE_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGE_LIST;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_DETAIL;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.FossologyAwarePortlet;
import org.eclipse.sw360.portal.portlets.projects.ProjectPortlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + PACKAGES_PORTLET_NAME,

        "javax.portlet.display-name=Packages",
        "javax.portlet.info.short-title=Packages",
        "javax.portlet.info.title=Packages",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/packages/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class PackagePortlet extends FossologyAwarePortlet {

    private static final Logger log = LogManager.getLogger(ProjectPortlet.class);

    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {

        final var action = request.getParameter(PortalConstants.ACTION);

        PackageService.Iface packageClient = thriftClients.makePackageClient();
        if (PortalConstants.IMPORT_PACKAGES.equals(action)) {
            importPackages(request, response);
        } else if (isGenericAction(action)) {
            dealWithGenericAction(request, response, action);
        }
    }


    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {

        final var pageName = request.getParameter(PAGENAME);
        final var id = request.getParameter("id");
        final var packageId = request.getParameter(PACKAGE_ID);
        if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/packages/detail.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<Package> packageList;
        try {
            PackageService.Iface packageClient = thriftClients.makePackageClient();
            packageList = packageClient.getAllPackages();

        } catch (TException e) {
            log.error("Could not get Packages from backend ", e);
            packageList = Collections.emptyList();
        }
        request.setAttribute(PACKAGE_LIST, packageList);
    }

    private void importPackages(ResourceRequest request, ResourceResponse response) {
        PackageService.Iface packageClient = thriftClients.makePackageClient();
        final User user = UserCacheHolder.getUserFromRequest(request);
        String attachmentContentId = request.getParameter(ATTACHMENT_CONTENT_ID);

        try {
            final RequestSummary requestSummary = packageClient.importPackagesFromAttachmentContent(user, attachmentContentId);

            LiferayPortletURL projectUrl = getProjectPortletUrl(request, requestSummary.getMessage());

            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            jsonObject.put("redirectUrl", projectUrl.toString());

            renderRequestSummary(request, response, requestSummary, jsonObject);
        } catch (TException e) {
            log.error("Failed to import Package BOM.", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }


    private static LiferayPortletURL getProjectPortletUrl(PortletRequest request, String projectId) {
        com.liferay.portal.kernel.model.Portlet portlet = PortletLocalServiceUtil.getPortletById(PortalConstants.PROJECT_PORTLET_NAME);
        Optional<Layout> layout = LayoutLocalServiceUtil.getLayouts(portlet.getCompanyId()).stream()
                .filter(l -> ("/" + PortalConstants.PROJECTS.toLowerCase()).equals(l.getFriendlyURL())).findFirst();
        if (layout.isPresent()) {
            long plId = layout.get().getPlid();
            LiferayPortletURL projectUrl = PortletURLFactoryUtil.create(request, PortalConstants.PROJECT_PORTLET_NAME, plId, PortletRequest.RENDER_PHASE);
            projectUrl.setParameter(PortalConstants.PROJECT_ID, projectId);
            projectUrl.setParameter(PortalConstants.PAGENAME, PortalConstants.PAGENAME_DETAIL);
            return projectUrl;
        }
        return null;
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) {
        String id = request.getParameter(PACKAGE_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        PackageService.Iface packageClient = thriftClients.makePackageClient();
        Package pkg;
        try {
            pkg = packageClient.getPackageById(id);
            request.setAttribute("pkg", pkg);
            ComponentService.Iface compClient = thriftClients.makeComponentClient();
            Release release = compClient.getReleaseById(pkg.getReleaseId(), user);
            String releaseName = SW360Utils.printFullname(release);
            request.setAttribute("releaseName", releaseName);
        } catch (TException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void dealWithFossologyAction(ResourceRequest request, ResourceResponse response, String action)
            throws IOException, PortletException {
        throw new UnsupportedOperationException("cannot call this action on the package portlet");
    }


    @Override
    protected Set<Attachment> getAttachments(String documentId, String documentType, User user) {
        // throw new UnsupportedOperationException("cannot call this getAttachments action on the project portlet");
        return Collections.emptySet();
    }
}
