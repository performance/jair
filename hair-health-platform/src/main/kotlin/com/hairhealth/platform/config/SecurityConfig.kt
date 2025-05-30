package com.hairhealth.platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints - no authentication required
                    .requestMatchers(HttpMethod.GET, "/api/v1/education/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/products").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/professionals").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/professionals/*/availability").permitAll()
                    
                    // Test endpoints
                    .requestMatchers("/api/v1/test/public").permitAll()
                    .requestMatchers("/api/v1/health").permitAll()
                    
                    // Authentication endpoints
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                    
                    // Health checks and actuator
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    
                    // For development - temporarily allow all other endpoints
                    .anyRequest().permitAll()
            }
            .build()
    }
}