package com.hairhealth.platform.security

import com.hairhealth.platform.service.AuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val authService: AuthService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = authHeader.substring(7)
            val userPrincipal = runBlocking { authService.validateAccessToken(token) }
            
            if (userPrincipal != null && SecurityContextHolder.getContext().authentication == null) {
                val authorities = userPrincipal.roles.map { role ->
                    SimpleGrantedAuthority("ROLE_$role")
                }
                
                val authToken = UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    authorities
                )
                
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        } catch (e: Exception) {
            // Invalid token, continue without authentication
        }

        filterChain.doFilter(request, response)
    }
}