/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.core.SimpleAuthenticationEntryPoint;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationFilter;
import org.eclipse.sw360.rest.resourceserver.security.apiToken.ApiTokenAuthenticationProvider;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Profile("!SECURITY_MOCK")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfiguration {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private ApiTokenAuthenticationFilter filter;

    @Autowired
    private ApiTokenAuthenticationProvider authProvider;

    @Autowired
    private Sw360CustomUserDetailsService userDetailsService;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    String issuerUri;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/", "/*/*/.html/", "/*/*.css/", "/*/*.js/", "/*/*.json/", "/*/*.png/", "/*/*.gif/", "/*/*.ico/", "/*/*.woff/*", "/*/*.ttf/");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChainRS1(HttpSecurity http) throws Exception {
        return http.authorizeRequests(auth -> auth.anyRequest().authenticated()).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwkSetUri(issuerUri))).build();
//        
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SimpleAuthenticationEntryPoint saep = new SimpleAuthenticationEntryPoint();
        http
                .addFilterBefore(filter, BasicAuthenticationFilter.class)
                .authenticationProvider(authProvider)
                .userDetailsService(userDetailsService)
                .httpBasic()
                .and()
                .authorizeRequests()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/info").hasAuthority("WRITE")
                .requestMatchers(HttpMethod.GET, "/api").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("READ")
                .requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("WRITE")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasAuthority("WRITE")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAuthority("WRITE")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasAuthority("WRITE").and()
                .csrf().disable().exceptionHandling().authenticationEntryPoint(saep);
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}