package com.hairhealth.platform.controller

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/test")
class TestController {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, String> {
        return mapOf("message" to "This is a public endpoint")
    }

    @GetMapping("/protected")
    fun protectedEndpoint(authentication: Authentication?): Map<String, Any> {
        return if (authentication != null) {
            mapOf(
                "message" to "This is a protected endpoint",
                "user" to authentication.name,
                "authorities" to authentication.authorities.map { it.authority }
            )
        } else {
            mapOf(
                "message" to "This is a protected endpoint",
                "user" to "anonymous",
                "authorities" to emptyList<String>()
            )
        }
    }
}