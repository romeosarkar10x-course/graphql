package dev.toolshed.lending;

import dev.toolshed.lending.Model.*;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * The lending rules as pure functions: no database, no clock of their own. Unit-tested and mutation-tested
 * (PIT) on their own; LendingService applies them inside a transaction.
 */
public final class LendingRules {
    static final int MAX_KEY_LENGTH = 64;

    private LendingRules() {}

    public static LoanStatus status(Loan loan, OffsetDateTime now) {
        if (loan.returnedAt() != null) return LoanStatus.RETURNED;
        return loan.dueAt().isBefore(now) ? LoanStatus.OVERDUE : LoanStatus.OPEN;
    }

    /** The input problem, if any: checked before anything is read or locked. */
    public static Optional<InvalidInput> checkInput(BorrowInput input, int maxDays) {
        String key = input.idempotencyKey();
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH)
            return Optional.of(new InvalidInput("idempotencyKey must be 1 to " + MAX_KEY_LENGTH + " characters", "idempotencyKey"));
        if (input.days() < 1 || input.days() > maxDays)
            return Optional.of(new InvalidInput("days must be between 1 and " + maxDays, "days"));
        return Optional.empty();
    }

    /** A member at the limit may not borrow; one under it may. */
    public static Optional<LoanLimitReached> checkLimit(int openLoans, int limit) {
        return openLoans >= limit
                ? Optional.of(new LoanLimitReached("you already have " + openLoans + " tools out; return one first", limit))
                : Optional.empty();
    }

    /** Members see their own loans; librarians see every loan. */
    public static boolean canSee(Loan loan, String viewerMemberId, boolean librarian) {
        return librarian || loan.memberId().equals(viewerMemberId);
    }
}
