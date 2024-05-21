/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;
import org.eclipse.sw360.rest.authserver.security.key.KeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * This class configures the oauth2 authorization server specialties for the
 * authorization parts of this server.
 */
@Configuration
@Import(OAuth2AuthorizationServerConfiguration.class)
public class Sw360AuthorizationServerConfiguration {

    @Value("${jwt.secretkey:sw360SecretKey}")
    private String secretKey;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new Sw360ClientDetailsService();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new Sw360UserDetailsService(principalProvider(), registeredClientRepository(), sw360UserAndClientAuthoritiesCalculator());
    }

    @Bean
    public Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator() {
        return new Sw360GrantedAuthoritiesCalculator();
    }

    @Bean
    protected Sw360UserDetailsProvider principalProvider() {
        return new Sw360UserDetailsProvider();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyManager keyManager) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        JWKSet set = new JWKSet(keyManager.rsaKey());
        return (j, sc) -> j.select(set);
    }

}