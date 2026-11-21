package dev.shelfie.secure;

import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/** Who is calling. */
@Component
public class Viewer {
  private final Store store;

  public Viewer(Store store) { this.store = store; }

  /** The signed-in reader, or null for an anonymous request. */
  public Reader current() {
    return null; // TODO lab 10.1: read the authenticated principal and look the reader up by username
  }

  public Reader require() {
    Reader me = current();
    if (me == null) throw new Denied(ErrorType.UNAUTHORIZED, "sign in first");
    return me;
  }
}
