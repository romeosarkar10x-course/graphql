package dev.shelfie.secure;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lab 10.7: what this app does on the wire. Some of these choices are wrong; so is part of application.yaml. */
@Configuration
public class Transport {
  private static final Logger log = LoggerFactory.getLogger(Transport.class);

  /** "Log every request so support can replay it." */
  @Bean
  OncePerRequestFilter requestLog() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res, jakarta.servlet.FilterChain chain)
          throws java.io.IOException, jakarta.servlet.ServletException {
        String headers = Collections.list(req.getHeaderNames()).stream().map(h -> h + "=" + req.getHeader(h)).collect(Collectors.joining(", "));
        log.info("{} {} headers: {}", req.getMethod(), req.getRequestURI(), headers);
        chain.doFilter(req, res);
      }
    };
  }

  /** "Real error messages make debugging easier." */
  @Bean
  DataFetcherExceptionResolverAdapter errorMessages() {
    return new DataFetcherExceptionResolverAdapter() {
      @Override
      protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof Denied || ex instanceof org.springframework.security.access.AccessDeniedException
            || ex instanceof org.springframework.security.core.AuthenticationException) return null;
        return GraphqlErrorBuilder.newError(env).message(ex.getMessage()).build();
      }
    };
  }
}
