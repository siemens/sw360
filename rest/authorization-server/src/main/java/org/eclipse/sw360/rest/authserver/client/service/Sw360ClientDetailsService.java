/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.service;

import java.time.Duration;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient.Builder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

public class Sw360ClientDetailsService implements RegisteredClientRepository {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Value("${security.accesstoken.validity}")
    private Integer accessTokenValidity;

    @Autowired
    private OAuthClientRepository clientRepo;

    @Override
    public RegisteredClient findByClientId(String clientId) {
        log.debug("client registration findbyClientId() called!!");
        return getByClientId(clientId);
    }

    private RegisteredClient getByClientId(String clientId) {
        OAuthClientEntity oce = clientRepo.getByClientId(clientId);
        Set<String> scopes = oce.getScope();

        Builder builder = RegisteredClient.withId(oce.getClientId()).clientId(oce.getClientId())
                .clientSecret(oce.getClientSecret()).scopes(sc -> sc.addAll(scopes)).clientName(oce.getDescription())
                .tokenSettings(getTokenSettings()).authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.JWT_BEARER);
        return builder.build();
    }

    private TokenSettings getTokenSettings() {
        return TokenSettings.builder().accessTokenTimeToLive(Duration.ofDays(accessTokenValidity)).build();
    }

    @Override
    public RegisteredClient findById(String clientId) {
        log.debug("client registration findbyId() called!!");
        return getByClientId(clientId);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        log.debug("client registration save() called!!");
    }
}
