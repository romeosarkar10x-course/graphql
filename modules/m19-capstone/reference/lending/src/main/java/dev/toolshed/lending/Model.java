package dev.toolshed.lending;

import java.time.OffsetDateTime;

/** Lending's types. Names match the GraphQL types, so the unions and interfaces resolve by class name. */
public final class Model {
    private Model() {}

    public enum LoanStatus { OPEN, OVERDUE, RETURNED }

    /** Inventory's entity, as lending knows it: an id. */
    public record Tool(String id) {}

    public record Member(String id, String handle, String displayName, String email) {}

    public record Loan(String id, String toolId, String memberId, OffsetDateTime borrowedAt, OffsetDateTime dueAt,
                       OffsetDateTime returnedAt) {
        public LoanStatus status(OffsetDateTime now) {
            return LendingRules.status(this, now);
        }
    }

    public record BorrowInput(String toolId, int days, String idempotencyKey) {}

    public record ReturnInput(String loanId) {}

    /** Expected failures: union members with a message, never GraphQL errors (docs/DESIGN.md, "Errors"). */
    public sealed interface Problem permits ToolUnavailable, LoanLimitReached, ToolNotFound, InvalidInput, LoanNotFound, AlreadyReturned {
        String message();
    }

    public sealed interface BorrowResult permits Borrowed, ToolUnavailable, LoanLimitReached, ToolNotFound, InvalidInput {}

    public sealed interface ReturnResult permits Returned, LoanNotFound, AlreadyReturned {}

    public record Borrowed(Loan loan) implements BorrowResult {}

    public record ToolUnavailable(String message, Tool tool, OffsetDateTime expectedBack) implements BorrowResult, Problem {}

    public record LoanLimitReached(String message, int limit) implements BorrowResult, Problem {}

    public record ToolNotFound(String message, String toolId) implements BorrowResult, Problem {}

    public record InvalidInput(String message, String field) implements BorrowResult, Problem {}

    public record Returned(Loan loan) implements ReturnResult {}

    public record LoanNotFound(String message, String loanId) implements ReturnResult, Problem {}

    public record AlreadyReturned(String message, Loan loan) implements ReturnResult, Problem {}

    public record AvailabilityChange(Tool tool, boolean available, OffsetDateTime at) {}

    public record PageInfo(boolean hasNextPage, String endCursor) {}

    public record LoanEdge(String cursor, Loan node) {}

    public record LoanConnection(java.util.List<LoanEdge> edges, PageInfo pageInfo) {}
}
