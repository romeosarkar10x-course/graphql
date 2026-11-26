package dev.shelfie.secure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Lecture 10.1: authentication happens at the HTTP layer, before GraphQL parses anything.
 * A request with no token is anonymous (viewer is null). A request with a bad token is rejected
 * with 401, even though every path is permitted: the bearer-token filter refuses it first.
 * The issuer, audience and claim names are in application.yaml.
 */
@Configuration
public class SecurityConfig {
  /**
   * Course tokens carry Keycloak's issuer. Check it here, offline: with issuer-uri in application.yaml, Spring
   * Boot 4.2 would fetch Keycloak's discovery document even though the public key is given. Boot adds this
   * validator to the decoder it builds from the key, next to the expiry and audience checks.
   */
  @Bean
  OAuth2TokenValidator<Jwt> courseIssuer() {
    return new JwtIssuerValidator("http://keycloak:8180/realms/shelfie");
  }

  @Bean
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .oauth2ResourceServer(o -> o.jwt(jwt -> {}))
        // Bearer tokens in a header are not sent by browsers on their own, so CSRF protection is off.
        // A cookie-based session would need it back on (lecture 10.3).
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }
}
