<%--
  ~ Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<h4>Welcome to SW360!</h4>

<p>Your user name will be your e-mail address. Go to the sign-in in the upper right corner. Please use your e-mail address and your e-mail password.</p>

<h4>Notice</h4>

<table border="0" cellpadding="3" cellspacing="3">
	<tbody>
		<tr bgcolor="#DDDDDD">
			<td width="180px">Your are on:</td>
			<td width="650px">
			<p>Productive Version</p>
			</td>
		</tr>
		<tr bgcolor="#DDDDDD">
			<td >Login:</td>
			<td>
			<p>Please use your Siemens E-Mail and Siemens E-Mail Password.</p>
			</td>
		</tr>
		<tr bgcolor="#DDDDDD">
			<td>Feature Requests, Bugs, Operational Issues:</td>
			<td><a href="https://code.siemens.com/sw360/sw360portal/issues">Issue tracker at code.siemens.com</a></td>
		</tr>
		<tr bgcolor="#DDDDDD">
			<td>Contact E-Mail:</td>
			<td>
			<p>Urgent issues can be sent to <a href="mailto:sw360.support.oss@internal.siemens.com">sw360.support.oss@internal.siemens.com</a>.</p>
			</td>
		</tr>
		<tr bgcolor="#DDDDDD">
			<td>Maintenance Downtime:</td>
			<td>
			<p>Please note our regular maintenance interval every Friday from 15:00 CET onwards.</p>
			</td>
		</tr>
		<tr bgcolor="#DDDDDD">
			<td>Known Issue:</td>
			<td>
			<p>SW360 does not support Internet Explorer 11 or older, please use Firefox or Chrome instead.</p>
			</td>
		</tr>
	</tbody>
</table>

<h4>General Information</h4>

<ul>
	<li><a href="https://wiki.siemens.com/display/en/Social+Media+Recommendations+For+Employees">Social Media Recommendations For Employees</a></li>
	<li><a href="https://findit.compliance.siemens.com/content/10000101/Compliance/CL_CO/CL_CO_AT/findIT_CL_CO_AT_55.pdf">Siemens Business Conduct Guidelines</a></li>
	<li><a href="http://www.siemens.com/corp/en/index/privacy.htm">Privacy Policy (Germany: In accordance to P 87 Sec. 1 subsection 6 BetrVG an assessment of personal data will not be done)</a></li>
	<li><a href="http://www.siemens.com/corp/en/index/terms_of_use.htm">Terms of Use (includes Compliance with Export Control Regulations)</a></li>
</ul>

<h4>More Information</h4>

<p>For more infomation please see:</p>

<ul>
	<li><a href="https://wiki.siemens.com/pages/viewpage.action?pageId=61346243">Siemens Intranet Information Pages.</a></li>
	<li><a href="https://github.com/eclipse/sw360">The project in the WWW (Public Internet).</a></li>
	<li><a href="https://code.siemens.com/sw360/sw360portal">The project pages on Gitlab (Siemens Intranet).</a></li>
</ul>

<p>In particular, you might want to check the following pages:</p>

<ul>
	<li><a href="https://github.com/eclipse/sw360/wiki/User-Workflows:-sw360">Workflow descriptions: how to create components, releases and projects.</a></li>
	<li><a href="https://github.com/eclipse/sw360/wiki/User-Workflows:-sw360-and-FOSSology">How to interact with FOSSology.</a></li>
	<li><a href="https://github.com/eclipse/sw360/wiki/Dev-Role-Authorisation-Model">Role access model.</a></li>
	<li><a href="https://github.com/eclipse/sw360/wiki/Dev-Moderation-Requests">Description about Moderation Reqeusts.</a></li>
</ul>

<core_rt:if test="${themeDisplay.signedIn}">
    <p style="font-weight: bold;">You are signed in, please go to the private pages on the top-right corner of this site:</p>
    <img src="<%=request.getContextPath()%>/images/welcome/select_private_pages.png" alt=""
         border="0" width="150"/><br/>
</core_rt:if>
<core_rt:if test="${not themeDisplay.signedIn}">
    <p style="font-weight: bold;"> In order to go ahead, please use the "Sign In" with your account. If you don&apos;t have an account, go to the <a href="/web/guest/sign-up">Sign Up</a> page to request one.</p>
</core_rt:if>
