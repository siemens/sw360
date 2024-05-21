/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * A {@link UserDetailsService} that should only be necessary in the
 * refresh_token workflow (but therefore needs to be configured for all oauth2
 * endpoints).
 */
public class Sw360UserDetailsService implements UserDetailsService {

    private final Logger log = LogManager.getLogger(this.getClass());

    private Sw360UserDetailsProvider userProvider;

    private RegisteredClientRepository clientProvider;

    private Sw360GrantedAuthoritiesCalculator authoritiesCalculator;

    public Sw360UserDetailsService(Sw360UserDetailsProvider userProvider, RegisteredClientRepository clientProvider,
            Sw360GrantedAuthoritiesCalculator authoritiesMerger) {
        this.userProvider = userProvider;
        this.clientProvider = clientProvider;
        this.authoritiesCalculator = authoritiesMerger;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails result = null;
        
//        User user = userProvider.provideUserDetails(username, null);
//        
//        result = new org.springframework.security.core.userdetails.User(user.getEmail(),
//                "PreAuthenticatedPassword", authoritiesCalculator.generateFromUser(user));
//        return result;

        Authentication clientAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (clientAuthentication != null && clientAuthentication instanceof UsernamePasswordAuthenticationToken) {
            String clientId = ((org.springframework.security.core.userdetails.User) clientAuthentication.getPrincipal())
                    .getUsername();
            try {
                RegisteredClient clientDetails = clientProvider.findByClientId(clientId);
                log.debug("Sw360ClientDetailsService returned client " + clientDetails + " for id " + clientId
                        + " from authentication details.");

                User user = userProvider.provideUserDetails(username, null);
                log.debug("Sw360UserDetailsProvider returned user " + user);

                if (clientDetails != null && user != null) {
                    result = new org.springframework.security.core.userdetails.User(user.getEmail(),
                            "PreAuthenticatedPassword", authoritiesCalculator.mergedAuthoritiesOf(user, clientDetails));
                }
            } catch (Exception e) {
                log.warn("No valid client for id " + clientId + " could be found. It is possible that it is "
                        + "locked, expired, disabled, or invalid for any other reason.");
                throw new UsernameNotFoundException("We cannot provide UserDetails for an invalid client: ", e);
            }
        } else {
            log.warn("Called in unwanted case: " + clientAuthentication);
        }

        if (result != null) {
            return result;
        } else {
            throw new UsernameNotFoundException("No user with username " + username + " found in sw360 users.");
        }
    }
}
