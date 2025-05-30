package com.hairhealth.platform.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

@Configuration
@ConditionalOnProperty(name = ["app.security.jwt.mode"], havingValue = "development", matchIfMissing = true)
class JwtDevelopmentConfig {

    @Bean
    fun jwtDecoder(): JwtDecoder {
        // Generate a key pair for development
        val keyPair = generateKeyPair()
        return NimbusJwtDecoder
            .withPublicKey(keyPair.public as RSAPublicKey)
            .build()
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}