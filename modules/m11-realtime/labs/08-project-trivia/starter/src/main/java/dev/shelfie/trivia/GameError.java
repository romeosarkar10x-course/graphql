package dev.shelfie.trivia;

import org.springframework.graphql.execution.ErrorType;

class GameError extends RuntimeException {
  final ErrorType type;
  final String code;

  GameError(ErrorType type, String code, String message) {
    super(message);
    this.type = type;
    this.code = code;
  }
}
