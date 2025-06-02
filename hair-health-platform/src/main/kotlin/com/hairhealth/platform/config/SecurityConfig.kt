package com.hairhealth.platform.config

import com.hairhealth.platform.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                // .anyRequest().permitAll() . toggle for debug
                    .requestMatchers("/api/v1/auth/**", "/api/v1/test/public", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll() // Public auth and docs
                    .requestMatchers(HttpMethod.GET, "/api/v1/professionals/me").hasAuthority("ROLE_PROFESSIONAL") // Secure professional /me
                    // Example for securing recommendation endpoints for professionals - adjust paths as needed
                    .requestMatchers("/api/v1/professionals/me/recommendations/**").hasAuthority("ROLE_PROFESSIONAL")
                    .requestMatchers("/api/v1/me/**").hasAuthority("ROLE_USER") // Secure user-specific /me endpoints
                    .anyRequest().authenticated() // All other requests need authentication
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}