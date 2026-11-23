package dev.toolshed.lending;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication at the HTTP layer (Module 10.1): no token is anonymous, a bad token is 401 before GraphQL
 * runs. Authorization is per field, in LendingController (docs/DESIGN.md, "Authorization").
 */
@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2ResourceServer(o -> o.jwt(jwt -> {}).authenticationEntryPoint(SecurityConfig::invalidToken))
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * A bad token is 401 with the Bearer challenge, and a GraphQL error body: the router passes that error on
     * to the client instead of "empty response" (Hive Router does not forward a subgraph's status code).
     */
    static void invalidToken(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                             org.springframework.security.core.AuthenticationException e) throws java.io.IOException {
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        response.setContentType("application/json");
        response.getWriter().write("{\"errors\":[{\"message\":\"the token is invalid or expired\",\"extensions\":{\"classification\":\"UNAUTHORIZED\"}}]}");
    }

    /** RS256 with the course key; exp, nbf, iss and aud checked. No network call, so no identity provider at runtime. */
    @Bean
    JwtDecoder jwtDecoder(LendingProperties props) throws Exception {
        String pem = props.jwt().publicKey().getContentAsString(java.nio.charset.StandardCharsets.US_ASCII)
                .replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.jwt().issuer()),
                new JwtClaimValidator<java.util.List<String>>("aud", aud -> aud != null && aud.contains(props.jwt().audience()))));
        return decoder;
    }
}
