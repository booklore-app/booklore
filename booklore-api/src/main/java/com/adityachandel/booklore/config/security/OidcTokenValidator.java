package com.adityachandel.booklore.config.security;

import com.adityachandel.booklore.model.dto.settings.OidcProviderDetails;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class OidcTokenValidator {

    private final AppSettingService appSettingService;

    public Authentication validate(String token) {
        if (!appSettingService.getAppSettings().isOidcEnabled()) {
            return null;
        }
        try {
            OidcProviderDetails providerDetails = appSettingService.getAppSettings().getOidcProviderDetails();
            String jwksUrl = providerDetails.getJwksUrl();
            if (jwksUrl == null || jwksUrl.isEmpty()) {
                log.error("JWKS URL is not configured");
                return null;
            }

            URL jwksURL = new URI(jwksUrl).toURL();
            DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(2000, 2000);
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(jwksURL, resourceRetriever);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.warn("OIDC token is expired or missing exp claim");
                return null;
            }
            return new UsernamePasswordAuthenticationToken("oidc-user", null, Collections.emptyList());
        } catch (Exception e) {
            log.error("OIDC token validation failed", e);
            return null;
        }
    }
}