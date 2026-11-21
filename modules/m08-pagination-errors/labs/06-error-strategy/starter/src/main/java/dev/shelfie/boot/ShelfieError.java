package dev.shelfie.boot;

import graphql.ErrorClassification;

/**
 * The error contract's codes. Each error carries its code twice: as the classification (graphql-java's
 * slot, which Spring writes to extensions.classification) and as extensions.code, the field clients
 * are told to switch on. Codes are API: add new ones, never rename or reuse.
 */
public enum ShelfieError implements ErrorClassification {
    /** No or bad credentials. Retry after signing in. */
    UNAUTHENTICATED,
    /** Signed in, not allowed. Don't retry. */
    FORBIDDEN,
    /** No such thing, or one you may not know exists. */
    NOT_FOUND,
    /** The request conflicts with current state (already a member). */
    CONFLICT,
    /** The request is malformed: syntax, validation, variables, bad arguments. Fix the client. */
    INVALID_REQUEST,
    /** Our fault. The message is generic; extensions.errorId finds the log entry. Retry may help. */
    INTERNAL
}
