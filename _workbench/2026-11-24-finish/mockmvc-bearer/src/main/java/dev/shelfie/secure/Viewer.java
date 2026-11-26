package dev.shelfie.secure;

import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Who is calling. The token was verified by Spring Security before GraphQL ran. */
@Component
public class Viewer {
  private final Store store;

  public Viewer(Store store) { this.store = store; }

  /** The signed-in reader, or null for an anonymous request. */
  public Reader current() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth instanceof JwtAuthenticationToken jwt ? store.byUsername(jwt.getName()) : null;
  }

  public Reader require() {
    Reader me = current();
    if (me == null) throw new Denied(ErrorType.UNAUTHORIZED, "sign in first");
    return me;
  }
}
