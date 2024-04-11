/*
 * Copyright Siemens AG, 2024.
 * Copyright Bosch Software Innovations GmbH, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.keycloak;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Profile("!SECURITY_MOCK")
@Component
public class KeycloakAccessTokenConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LogManager.getLogger(KeycloakAccessTokenConverter.class);
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    private static final String JWT_EMIAL = "email";

    @Value("${jwt.auth.converter.principle-attribute:email}")
    private String principleAttribute;

    @Value("${jwt.auth.converter.resource-id:sw360-rest-api}")
    private String resourceId;

    @Autowired
    private Sw360UserService userService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream
                .concat(jwtGrantedAuthoritiesConverter.convert(jwt).stream(), extractResourceRoles(jwt).stream())
                .collect(Collectors.toSet());
        return new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
    }

    private String getPrincipleClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (principleAttribute != null) {
            claimName = principleAttribute;
        }
        return jwt.getClaim(claimName);
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        User sw360User = null;
        String email = jwt.getClaim(JWT_EMIAL);
        if (email != null && CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            sw360User = userService.getUserByEmail(email);
            if (sw360User == null || sw360User.isDeactivated()) {
                throw new UnauthorizedUserException("User is deactivated or not available.");
            }
        }

        List<GrantedAuthority> grantedAuthList = Sw360GrantedAuthoritiesCalculator.generateFromUser(sw360User);
        String clientScopes = jwt.getClaim("scope");
        String[] scopes = clientScopes.split("\\s+");
        Set<String> scopeSet = new HashSet<>(Arrays.asList(scopes));

        return grantedAuthList.stream().filter(ga -> scopeSet.contains(ga.toString())).toList();
    }
}
