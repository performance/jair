package com.hairhealth.platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
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
                    
                    // Authentication endpoints
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                    
                    // Health checks and actuator
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    
                    // User endpoints - require USER role
                    .requestMatchers("/api/v1/me/**").hasRole("USER")
                    .requestMatchers("/api/v1/users/me/**").hasRole("USER")
                    
                    // Professional endpoints - require PROFESSIONAL role
                    .requestMatchers("/api/v1/professionals/me/**").hasRole("PROFESSIONAL")
                    
                    // Admin endpoints - require ADMIN role
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    
                    // All other requests require authentication
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            // Extract roles from JWT claims
            val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
            val scopes = jwt.getClaimAsStringList("scope") ?: emptyList()
            
            val authorities: MutableCollection<GrantedAuthority> = mutableListOf()
            
            // Add role-based authorities
            roles.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_${role.uppercase()}"))
            }
            
            // Add scope-based authorities
            scopes.forEach { scope ->
                authorities.add(SimpleGrantedAuthority("SCOPE_$scope"))
            }
            
            authorities // as Collection<GrantedAuthority> 
        }
        return converter
    }

    // @Bean
    // fun jwtDecoder(): JwtDecoder {
    //     // For development - we'll create a simple JWT decoder
    //     // In production, this would point to your actual JWT issuer
    //     return NimbusJwtDecoder
    //         .withJwkSetUri("http://localhost:8080/.well-known/jwks.json")
    //         .build()
    // }
}