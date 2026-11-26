package dev.shelfie.privacy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Module 10's set-up: a bad token is a 401 before GraphQL runs; no token reaches GraphQL anonymous. */
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
            .oauth2ResourceServer(o -> o.jwt(jwt -> { }))
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
