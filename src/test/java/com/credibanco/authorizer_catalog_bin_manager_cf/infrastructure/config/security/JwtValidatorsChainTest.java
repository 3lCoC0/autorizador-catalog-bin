package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorsChainTest {

    @Test
    void validatesIssuerAndAudienceSuccessfully() {
        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "issuer")
                .audience(List.of("aud", "extra"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        JwtValidatorsChain chain = new JwtValidatorsChain("issuer", "aud");
        assertEquals(OAuth2TokenValidatorResult.success(), chain.validate(token));
    }

    @Test
    void failsWhenIssuerDoesNotMatch() {
        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "other")
                .audience(List.of("aud"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        JwtValidatorsChain chain = new JwtValidatorsChain("issuer", "aud");
        assertTrue(chain.validate(token).hasErrors());
    }

    @Test
    void audienceValidatorReturnsFailureWhenAudienceMissing() {
        Jwt token = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "issuer")
                .audience(List.of("wrong"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        JwtValidatorsChain.AudienceValidator validator = new JwtValidatorsChain.AudienceValidator("aud");
        OAuth2TokenValidatorResult result = validator.validate(token);

        assertTrue(result.hasErrors());
        assertEquals("invalid_token", result.getErrors().iterator().next().getErrorCode());
    }
}
