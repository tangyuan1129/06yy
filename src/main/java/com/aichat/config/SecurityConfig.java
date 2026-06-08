package com.aichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/api/auth/**",
                    "/api/init/**",
                    "/api/characters/**",
                    "/api/messages/**",
                    "/api/chat/**",
                    "/api/groups/**",
                    "/api/avatars/**",
                    "/api/group-messages/**",
                    "/api/group-members/**",
                    "/api/group-init/**",
                    "/api/group-chat/**",
                    "/api/health/**",
                    "/api/active-messages/**",
                    "/images/**",
                    "/**.html",
                    "/**.js",
                    "/**.css",
                    "/**.png",
                    "/**.jpg",
                    "/**.jpeg",
                    "/**.gif",
                    "/**.ico",
                    "/**.svg",
                    "/**.woff",
                    "/**.woff2",
                    "/**.ttf",
                    "/**.eot",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}