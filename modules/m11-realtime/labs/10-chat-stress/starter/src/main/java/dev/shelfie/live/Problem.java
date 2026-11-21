package dev.shelfie.live;

import org.springframework.graphql.execution.ErrorType;

/** A failure the client caused: becomes a GraphQL error with this classification. */
public class Problem extends RuntimeException {
  final ErrorType type;

  public Problem(ErrorType type, String message) {
    super(message);
    this.type = type;
  }
}
