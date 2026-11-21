package dev.shelfie.secure;

import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * @auth(role: MODERATOR) on a field definition: the schema says who may call it, and this wiring wraps
 * the field's data fetcher. ADMIN passes every check.
 */
@Configuration
public class AuthDirective implements SchemaDirectiveWiring {
  private final Viewer viewer;

  public AuthDirective(Viewer viewer) { this.viewer = viewer; }

  @Bean
  RuntimeWiringConfigurer authDirectiveWiring() {
    return wiring -> wiring.directive("auth", this);
  }

  @Override
  public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> env) {
    String role = env.getAppliedDirective().getArgument("role").getValue().toString().toLowerCase();
    GraphQLFieldsContainer parent = env.getFieldsContainer();
    FieldCoordinates field = FieldCoordinates.coordinates(parent.getName(), env.getElement().getName());
    DataFetcher<?> original = env.getCodeRegistry().getDataFetcher(field, env.getElement());
    DataFetcher<?> guarded = dfe -> {
      // The roles come from the token's claims.
      var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
      if (viewer.current() == null || auth == null) throw new AuthenticationCredentialsNotFoundException("sign in first");
      var granted = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
      if (!granted.contains("ROLE_" + role) && !granted.contains("ROLE_admin")) throw new AccessDeniedException("needs role " + role);
      return original.get(dfe);
    };
    env.getCodeRegistry().dataFetcher(field, guarded);
    return env.getElement();
  }
}
