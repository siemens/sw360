package org.eclipse.sw360.rest.resourceserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Bean
    public JwtBlacklistService jwtBlacklistService() {
        return new JwtBlacklistService();
    }
}
