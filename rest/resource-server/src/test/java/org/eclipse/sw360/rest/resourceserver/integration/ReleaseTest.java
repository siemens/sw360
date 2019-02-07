/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    private Release release1;
    private String release1Id = "121831bjh1v2j";
    private String attachmentShaUsedMultipleTimes = "12345";
    private String attachmentShaInvalid = "999";

    @Before
    public void before() throws TException {
        List<Attachment> attachments = new ArrayList<>();
        Attachment attachment1 = new Attachment();
        attachment1.setAttachmentContentId("a1");
        attachment1.setSha1(attachmentShaUsedMultipleTimes);
        attachment1.setFilename("Attachment 1");
        attachment1.setAttachmentType(AttachmentType.BINARY);
        attachments.add(attachment1);

        Source source1 = new Source(Source._Fields.RELEASE_ID, release1Id);

        Attachment attachment2 = new Attachment();
        attachment2.setAttachmentContentId("a2");
        attachment2.setSha1(attachmentShaUsedMultipleTimes);
        attachment2.setFilename("Attachment 2");
        attachment2.setAttachmentType(AttachmentType.SOURCE);
        attachments.add(attachment2);

        String releaseId2 = "3451831bjh1v2jxxz";
        Source source2 = new Source(Source._Fields.RELEASE_ID, releaseId2);

        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        AttachmentInfo attachmentInfo1 = new AttachmentInfo(attachment1);
        attachmentInfo1.setOwner(source1);
        attachmentInfos.add(attachmentInfo1);

        AttachmentInfo attachmentInfo2 = new AttachmentInfo(attachment2);
        attachmentInfo2.setOwner(source2);
        attachmentInfos.add(attachmentInfo2);

        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(attachmentShaUsedMultipleTimes))).willReturn(attachmentInfos);

        List<AttachmentInfo> emptyAttachmentInfos = new ArrayList<>();
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(attachmentShaInvalid))).willReturn(emptyAttachmentInfos);

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        List<Release> releases = new ArrayList<>();

        release1 = new Release();
        release1.setName("Release 1");
        release1.setId(this.release1Id);
        release1.setComponentId("component123");
        release1.setVersion("1.0.4");
        release1.setCpeid("cpe:id-1231");
        release1.setMainlineState(MainlineState.MAINLINE);
        release1.setClearingState(ClearingState.APPROVED);
        releases.add(release1);

        given(this.releaseServiceMock.getReleaseForUserById(eq(this.release1Id), eq(user))).willReturn(release1);

        Release release2 = new Release();
        release2.setName("Release 2");
        release2.setId(releaseId2);
        release2.setComponentId("component456");
        release2.setVersion("2.0.0");
        release2.setCpeid("cpe:id-4567");
        release2.setMainlineState(MainlineState.OPEN);
        release2.setClearingState(ClearingState.NEW_CLEARING);
        releases.add(release2);

        given(this.releaseServiceMock.getReleasesForUser(anyObject())).willReturn(releases);
        given(this.releaseServiceMock.getReleaseForUserById(eq(releaseId2), eq(user))).willReturn(release2);
    }

    @Test
    public void should_update_release_valid() throws IOException, TException {
        String updatedReleaseName = "updatedReleaseName";
        given(this.releaseServiceMock.updateRelease(anyObject(), anyObject())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.getReleaseForUserById(eq(release1Id), anyObject())).willReturn(release1);
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        body.put("wrong_prop", "abc123");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + release1Id,
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(responseBody.get("name").textValue(), updatedReleaseName);
        assertNull(responseBody.get("wrong_prop"));
    }

    @Test
    public void should_update_release_invalid() throws IOException, TException {
        doThrow(TException.class).when(this.releaseServiceMock).getReleaseForUserById(anyObject(), anyObject());
        String updatedReleaseName = "updatedReleaseName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/unknownId123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_all_releases_with_fields() throws IOException {
        String extraField = "cpeId";
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?fields=" + extraField,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2, Collections.singletonList(extraField));
    }

    @Test
    public void should_delete_releases() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(eq(release1Id), anyObject())).willReturn(RequestStatus.SUCCESS);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + release1Id + "," + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(release1Id, HttpStatus.OK));
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_delete_release() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(anyObject(), anyObject())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_get_empty_collection_for_invalid_sha() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?sha1=" + attachmentShaInvalid,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 0);
    }

    @Test
    public void should_get_collection_for_duplicated_shas() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?sha1=" + attachmentShaUsedMultipleTimes + "&fields=mainlineState,clearingState",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }
}
