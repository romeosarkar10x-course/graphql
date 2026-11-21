package dev.shelfie.secure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.support.BearerTokenAuthenticationExtractor;
import org.springframework.graphql.server.webmvc.AuthenticationWebSocketInterceptor;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lecture 10.7: the wire.
 *   - WebSocket: the token arrives in connection_init's payload ({"Authorization": "Bearer ..."}), is
 *     verified like an HTTP one, and is the security context of every subscription on that socket.
 *   - The handshake is refused (403) from any origin but Shelfie's. There is no code for it: Spring
 *     refuses cross-origin handshakes unless CORS allows the origin, and Boot hands spring.graphql.cors
 *     to the WebSocket handler, so the starter's allow-every-origin CORS opened the socket too.
 *   - Logs never hold credentials. Errors never carry internal messages: Spring for GraphQL answers an
 *     unresolved exception with INTERNAL_ERROR and the execution id, and the detail stays in the log.
 *   - CORS: see application.yaml.
 */
@Configuration
public class Transport {
  private static final Logger log = LoggerFactory.getLogger(Transport.class);
  private static final Set<String> SECRET_HEADERS = Set.of("authorization", "cookie", "x-api-key", "proxy-authorization");

  @Bean
  AuthenticationWebSocketInterceptor webSocketAuthentication(JwtDecoder decoder) {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("preferred_username");
    converter.setJwtGrantedAuthoritiesConverter(roles);
    var provider = new JwtAuthenticationProvider(decoder);
    provider.setJwtAuthenticationConverter(converter);
    return new AuthenticationWebSocketInterceptor(new BearerTokenAuthenticationExtractor(), new ProviderManager(provider));
  }

  @Bean
  OncePerRequestFilter requestLog() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String headers = Collections.list(req.getHeaderNames()).stream()
            .map(h -> h + "=" + (SECRET_HEADERS.contains(h.toLowerCase()) ? "[redacted]" : req.getHeader(h)))
            .collect(Collectors.joining(", "));
        log.info("{} {} headers: {}", req.getMethod(), req.getRequestURI(), headers);
        chain.doFilter(req, res);
      }
    };
  }
}
