package dev.shelfie.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Shelfie with real dependencies, for integration tests (Module 13.3). */
@SpringBootApplication
public class ItApplication {

    public static void main(String[] args) { SpringApplication.run(ItApplication.class, args); }

    /** Anyone may call /graphql; a bearer token, when sent, must verify. Fields decide what needs a user. */
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .oauth2ResourceServer(o -> o.jwt(j -> {}))
            .build();
    }
}
