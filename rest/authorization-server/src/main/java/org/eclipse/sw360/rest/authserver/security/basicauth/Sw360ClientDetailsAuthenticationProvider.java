/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.basicauth;

import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthoritiesCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This {@link AuthenticationProvider} is able to verify the given credentials
 * of the {@link Authentication} object against a configured Liferay instance.
 *
 * In addition it supports the special password grant flow of spring in
 * retrieving information about the oauth client that has initiated the request
 * and cutting the user authorities to those of the client in such case by using
 * the {@link Sw360GrantedAuthoritiesCalculator}.
 */
//@Component
public class Sw360ClientDetailsAuthenticationProvider implements AuthenticationProvider {

//    private final Logger log = LogManager.getLogger(this.getClass());

//    private static final String SUPPORTED_GRANT_TYPE = "password";

    @Value("${sw360.sw360-portal-server-url}")
    private String sw360PortalServerURL;

    @Value("${sw360.sw360-liferay-company-id}")
    private String sw360LiferayCompanyId;

//    @Autowired
//    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private RegisteredClientRepository clientDetailsService;
    
    
    private @Autowired HttpServletRequest request;

//    @Autowired
//    private Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider;
    
//    @Autowired
//    private RegisteredClientRepository regClientRepo;

//    @Autowired
//    private Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userIdentifier = authentication.getName();
        Object possiblePassword = authentication.getCredentials();
        if (possiblePassword == null) {
            return null;
        }
        String password = possiblePassword.toString();
        RegisteredClient rc = clientDetailsService.findByClientId(userIdentifier);
        if(rc != null) {
            UsernamePasswordAuthenticationToken utoken = new UsernamePasswordAuthenticationToken(userIdentifier, password);
            utoken.setDetails(request.getParameterMap());
            return utoken;
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
