package dev.toolshed.lending;

import static org.assertj.core.api.Assertions.assertThat;

import dev.toolshed.lending.Model.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The lending rules, without Spring or a database. PIT's target (mvn -pl lending -am verify -Ppit). */
class LendingRulesTest {
    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 23, 12, 0, 0, 0, ZoneOffset.UTC);

    static Loan loan(OffsetDateTime due, OffsetDateTime returned) {
        return new Loan("l1", "t01", "m-ada", NOW.minusDays(7), due, returned);
    }

    @Test
    void aReturnedLoanIsReturnedEvenWhenItWasLate() {
        assertThat(LendingRules.status(loan(NOW.minusDays(1), NOW), NOW)).isEqualTo(LoanStatus.RETURNED);
    }

    @Test
    void anOpenLoanPastItsDueTimeIsOverdue() {
        assertThat(LendingRules.status(loan(NOW.minusSeconds(1), null), NOW)).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    void anOpenLoanDueExactlyNowIsStillOpen() {
        assertThat(LendingRules.status(loan(NOW, null), NOW)).isEqualTo(LoanStatus.OPEN);
    }

    @Test
    void daysMustBeBetweenOneAndTheMaximum() {
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 0, "k"), 14)).get().extracting(InvalidInput::field).isEqualTo("days");
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 15, "k"), 14)).get().extracting(InvalidInput::field).isEqualTo("days");
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 1, "k"), 14)).isEmpty();
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 14, "k"), 14)).isEmpty();
    }

    @Test
    void theKeyMustBePresentAndShort() {
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 7, ""), 14)).get().extracting(InvalidInput::field).isEqualTo("idempotencyKey");
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 7, "  "), 14)).isPresent();
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 7, null), 14)).isPresent();
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 7, "k".repeat(64)), 14)).isEmpty();
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 7, "k".repeat(65)), 14)).isPresent();
    }

    @Test
    void theKeyIsCheckedBeforeTheDays() {
        assertThat(LendingRules.checkInput(new BorrowInput("t01", 99, ""), 14)).get().extracting(InvalidInput::field).isEqualTo("idempotencyKey");
    }

    @Test
    void theLimitStopsTheNextLoanNotTheLastOneAllowed() {
        assertThat(LendingRules.checkLimit(2, 3)).isEmpty();
        assertThat(LendingRules.checkLimit(3, 3)).get().satisfies(p -> {
            assertThat(p.limit()).isEqualTo(3);
            assertThat(p.message()).contains("3 tools out");
        });
    }

    @Test
    void membersSeeTheirOwnLoansAndLibrariansSeeAll() {
        Loan l = loan(NOW, null);
        assertThat(LendingRules.canSee(l, "m-ada", false)).isTrue();
        assertThat(LendingRules.canSee(l, "m-ben", false)).isFalse();
        assertThat(LendingRules.canSee(l, "m-ben", true)).isTrue();
    }
}
