package dev.shelfie.live;

/** The subscription's source stream broke (the bus went away, a feed errored). */
public class SourceStreamFailed extends RuntimeException {
  public SourceStreamFailed(String message) { super(message); }
}
