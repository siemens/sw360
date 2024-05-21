/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class Sw360WebSecurityConfiguration {
    
//    @Autowired
//    public RegisteredClientRepository clientDetailsService;

//    @Bean
//    public SecurityFilterChain securityFilterChainWS(HttpSecurity http) throws Exception {
//        return http
//                .httpBasic()
//                .and()
//                .authorizeRequests().requestMatchers("/client-managment").permitAll()
//                .anyRequest()
//                .authenticated()
//                .and()
//                .build();
//    }

//    @Bean
//    public Sw360ClientDetailsAuthenticationProvider ccprovider() {
//        return new Sw360ClientDetailsAuthenticationProvider();
//    }

//    @Bean
//    protected Sw360CustomHeaderAuthenticationProvider sw360CustomHeaderAuthenticationProvider() {
//        return new Sw360CustomHeaderAuthenticationProvider();
//    }

//    @Bean
//    protected Sw360ClientDetailsAuthenticationProvider getclientsAuthenticatioinProvider() {
//        return new Sw360ClientDetailsAuthenticationProvider();
//        
//    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        var u1 = User.withUsername("bill").password("12345").authorities("ADMIN").build();
//        var uds = new InMemoryUserDetailsManager();
//        uds.createUser(u1);
//        return uds;
//        return new Sw360UserDetailsService(principalProvider(), clientDetailsService,
//                sw360UserAndClientAuthoritiesCalculator());
//    }

//    @Bean
//    protected Sw360CustomHeaderAuthenticationFilter sw360CustomHeaderAuthenticationFilter() {
//        return new Sw360CustomHeaderAuthenticationFilter();
//    }


    @Bean
    protected ThriftClients thriftClients() {
        return new ThriftClients();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}