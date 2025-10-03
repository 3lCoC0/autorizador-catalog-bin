package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

public class JwtValidatorsChain implements OAuth2TokenValidator<Jwt> {

    private final OAuth2TokenValidator<Jwt> delegate;

    public JwtValidatorsChain(String expectedIssuer, String requiredAudience) {
        var withIssuer = JwtValidators.createDefaultWithIssuer(expectedIssuer);
        var audienceValidator = new AudienceValidator(requiredAudience);
        this.delegate = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return delegate.validate(token);
    }

    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String requiredAud;
        AudienceValidator(String aud) { this.requiredAud = aud; }
        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            List<String> aud = token.getAudience();
            if (aud != null && aud.contains(requiredAud)) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "Missing required audience: " + requiredAud, null));
        }
    }
}