package dev.shelfie.secure;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.*;
import graphql.execution.instrumentation.parameters.*;
import graphql.GraphqlErrorBuilder;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.InMemoryPersistedQueryCache;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The vulnerable fork's plumbing. Every comment in quotes is the excuse someone gave for the shortcut.
 * Labs 10.3, 10.9 and 10.10. Do not copy any of this into a real server.
 */
@Configuration
@EnableScheduling
public class CtfSecurity {
  static final String ISSUER = "http://shelfie-ctf";

  /** A fresh key every start, so tokens cannot be minted offline: sign in with login(...). */
  @org.springframework.stereotype.Component
  static class Keys {
    final KeyPair pair;

    Keys() throws Exception {
      KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
      g.initialize(2048);
      pair = g.generateKeyPair();
    }

    String sign(Reader r) {
      try {
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).audience("shelfie-api").subject(r.id())
            .claim("preferred_username", r.username()).claim("roles", r.roles())
            .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(3600))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(pair.getPrivate()));
        return jwt.serialize();
      } catch (JOSEException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Bean
  JwtDecoder jwtDecoder(Keys keys) {
    NimbusJwtDecoder rsa = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.pair.getPublic()).build();
    rsa.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
    return token -> {
      try {
        if (JWTParser.parse(token) instanceof PlainJWT plain) {
          // "Unsigned tokens are handy for local testing."
          Map<String, Object> claims = new HashMap<>(plain.getJWTClaimsSet().getClaims());
          claims.replaceAll((k, v) -> v instanceof Date d ? d.toInstant() : v);
          return Jwt.withTokenValue(token).header("alg", "none").claims(c -> c.putAll(claims)).build();
        }
      } catch (java.text.ParseException e) {
        throw new BadJwtException("Malformed token", e);
      }
      return rsa.decode(token);
    };
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var c = new JwtAuthenticationConverter();
    c.setPrincipalClaimName("preferred_username");
    c.setJwtGrantedAuthoritiesConverter(roles);
    return c;
  }

  @Bean
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();
    // "The web app keeps the token in a cookie."
    BearerTokenResolver headerOrCookie = request -> {
      String t = header.resolve(request);
      if (t != null) return t;
      if (request.getCookies() != null)
        for (Cookie c : request.getCookies()) if (c.getName().equals("shelfie_token")) return c.getValue();
      return null;
    };
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .oauth2ResourceServer(o -> o.bearerTokenResolver(headerOrCookie).jwt(j -> {}))
        .csrf(c -> c.disable());   // "it's an API"
    return http.build();
  }

  /** "An old client sends text/plain." Turns a CORS-simple request into a GraphQL request. */
  @Bean
  Filter plainTextIsJson() {
    return (req, res, chain) -> {
      HttpServletRequest http = (HttpServletRequest) req;
      String type = http.getContentType();
      if (type != null && type.startsWith("text/plain")) {
        req = new HttpServletRequestWrapper(http) {
          @Override public String getContentType() { return "application/json"; }
          @Override public String getHeader(String name) {
            return name.equalsIgnoreCase("Content-Type") ? "application/json" : super.getHeader(name);
          }
          @Override public Enumeration<String> getHeaders(String name) {
            return name.equalsIgnoreCase("Content-Type") ? Collections.enumeration(List.of("application/json")) : super.getHeaders(name);
          }
        };
      }
      chain.doFilter(req, res);
    };
  }

  /** Automatic persisted queries: "persisted means reviewed". */
  @Bean
  GraphQlSourceBuilderCustomizer apq() {
    return b -> b.configureGraphQl(g -> g.preparsedDocumentProvider(
        new ApolloPersistedQuerySupport(InMemoryPersistedQueryCache.newInMemoryPersistedQueryCache().build())));
  }

  final AtomicLong graphqlRequests = new AtomicLong();
  private final Map<String, Deque<Long>> joinAttempts = new ConcurrentHashMap<>();

  @Bean
  WebGraphQlInterceptor ctfInterceptor(Viewer viewer) {
    return (request, chain) -> {
      graphqlRequests.incrementAndGet();
      boolean persisted = request.getExtensions().containsKey("persistedQuery");
      // "joinClub is rate limited: 10 requests a minute per reader."
      boolean joinAllowed = true;
      if (request.getDocument().contains("joinClub")) {
        Reader me = viewer.current();
        Deque<Long> q = joinAttempts.computeIfAbsent(me == null ? "anonymous" : me.id(), k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (q) {
          while (!q.isEmpty() && q.peekFirst() < now - 60_000) q.pollFirst();
          joinAllowed = q.size() < 10;
          if (joinAllowed) q.addLast(now);
        }
      }
      boolean allowed = joinAllowed;
      request.configureExecutionInput((input, b) -> b.graphQLContext(c -> c.put("persisted", persisted).put("joinAllowed", allowed)).build());
      return chain.next(request);
    };
  }

  static final class FetchCount implements InstrumentationState {
    final AtomicLong n = new AtomicLong();
  }

  /** "Expensive queries carry a debug block so the ops team can find them": 100,000 field fetches or more. */
  @Bean
  Instrumentation expensiveQueryLog() {
    return new SimplePerformantInstrumentation() {
      @Override
      public InstrumentationState createState(InstrumentationCreateStateParameters parameters) { return new FetchCount(); }

      @Override
      public FieldFetchingInstrumentationContext beginFieldFetching(InstrumentationFieldFetchParameters p, InstrumentationState state) {
        ((FetchCount) state).n.incrementAndGet();
        return FieldFetchingInstrumentationContext.NOOP;
      }

      @Override
      public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult result, InstrumentationExecutionParameters p, InstrumentationState state) {
        long n = ((FetchCount) state).n.get();
        if (n < 100_000) return CompletableFuture.completedFuture(result);
        return CompletableFuture.completedFuture(result.transform(b -> b.addExtension("debug",
            Map.of("fieldFetches", n, "slowQueryLog", Flags.of("slow-query")))));
      }
    };
  }

  /** "Real error messages make debugging easier." */
  @Bean
  DataFetcherExceptionResolverAdapter leakyErrors() {
    return new DataFetcherExceptionResolverAdapter() {
      @Override
      protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof Denied || ex instanceof AccessDeniedException || ex instanceof AuthenticationException) return null;
        return GraphqlErrorBuilder.newError(env).message(ex.toString()).build();
      }
    };
  }

  @RestController
  static class Stats {
    private final CtfSecurity security;

    Stats(CtfSecurity security) { this.security = security; }

    /** Lab 10.3 counts your brute force's HTTP requests here. */
    @GetMapping("/_ctf/stats")
    Map<String, Object> stats() { return Map.of("graphqlRequests", security.graphqlRequests.get()); }
  }
}
