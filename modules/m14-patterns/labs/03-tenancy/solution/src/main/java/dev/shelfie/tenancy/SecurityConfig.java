package dev.shelfie.tenancy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Module 10's set-up: a bad token is a 401 before GraphQL runs; no token reaches GraphQL anonymous. */
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .oauth2ResourceServer(o -> o.jwt(jwt -> { }))
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
