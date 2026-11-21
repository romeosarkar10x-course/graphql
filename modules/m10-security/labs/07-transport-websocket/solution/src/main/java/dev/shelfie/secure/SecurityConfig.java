package dev.shelfie.secure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity   // lab 10.2: @PreAuthorize on ClubService
public class SecurityConfig {
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
