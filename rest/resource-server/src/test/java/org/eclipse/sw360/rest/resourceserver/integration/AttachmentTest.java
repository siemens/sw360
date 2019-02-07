/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Attachment.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
public class AttachmentTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360AttachmentService attachmentServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    private final String shaMultiple = "12345";
    private final String shaInvalid = "56789";

    @Before
    public void before() throws TException {
        List<Attachment> attachments = new ArrayList<>();
        Attachment attachment1 = new Attachment();
        attachment1.setAttachmentContentId("a1");
        attachment1.setSha1(shaMultiple);
        attachment1.setFilename("Attachment 1");
        attachment1.setAttachmentType(AttachmentType.BINARY);
        attachments.add(attachment1);

        Source source1 = new Source(Source._Fields.RELEASE_ID, "release1");

        Attachment attachment2 = new Attachment();
        attachment2.setAttachmentContentId("a2");
        attachment2.setSha1(shaMultiple);
        attachment2.setFilename("Attachment 2");
        attachment2.setAttachmentType(AttachmentType.SOURCE);
        attachments.add(attachment2);

        Source source2 = new Source(Source._Fields.RELEASE_ID, "release2");

        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        AttachmentInfo attachmentInfo1 = new AttachmentInfo(attachment1);
        attachmentInfo1.setOwner(source1);
        attachmentInfos.add(attachmentInfo1);

        AttachmentInfo attachmentInfo2 = new AttachmentInfo(attachment2);
        attachmentInfo2.setOwner(source2);
        attachmentInfos.add(attachmentInfo2);

        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(shaMultiple))).willReturn(attachmentInfos);
        given(this.attachmentServiceMock.getAttachmentsBySha1(eq(shaInvalid))).willReturn(new ArrayList<>());

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        Release release1 = new Release();
        release1.setName("Release 1");
        release1.setId("release1");
        release1.setComponentId("component123");
        release1.setVersion("1.0.4");
        release1.setCpeid("cpe:id-1231");

        given(this.releaseServiceMock.getReleaseForUserById(eq("release1"), eq(user))).willReturn(release1);

        Release release2 = new Release();
        release2.setName("Release 2");
        release2.setId("release2");
        release2.setComponentId("component456");
        release2.setVersion("2.0.0");
        release2.setCpeid("cpe:id-4567");

        given(this.releaseServiceMock.getReleaseForUserById(eq("release2"), eq(user))).willReturn(release2);
    }

    @Test
    public void should_get_multiple_attachments_by_sha1() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments?sha1=" + shaMultiple,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "attachments", 2);
    }

    @Test
    public void should_get_empty_attachment_collection_by_sha1() throws IOException {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/attachments?sha1=" + shaInvalid,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        TestHelper.checkResponse(response.getBody(), "attachments", 0);
    }
}
