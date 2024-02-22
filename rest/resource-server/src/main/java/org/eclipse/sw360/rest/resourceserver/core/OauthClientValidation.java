/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
/**
 * Created by Smruti on 21/02/24.
 *
 * @author smruti.sahoo@siemens.com
 */

@Service
public class OauthClientValidation {

    @Autowired
    private WebClient.Builder builder;

    @Value("${authorization.server.url}")
    private String authServerURL;

    public Map<String, String> getClientById(String clientId) {
        return builder.build()
                .get()
                .uri(authServerURL+"/client-management/" + clientId)
                .retrieve()
                .bodyToMono(Map.class).block();
    }

}
