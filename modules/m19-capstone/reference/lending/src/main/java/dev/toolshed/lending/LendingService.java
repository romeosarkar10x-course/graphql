package dev.toolshed.lending;

import dev.toolshed.lending.Model.*;
import io.micrometer.core.instrument.Metrics;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Borrow and return: the rules (LendingRules) applied inside one transaction each. */
@Service
class LendingService {
    private static final Map<String, Class<? extends BorrowResult>> STORED = Map.of(
            "ToolUnavailable", ToolUnavailable.class, "LoanLimitReached", LoanLimitReached.class,
            "ToolNotFound", ToolNotFound.class, "InvalidInput", InvalidInput.class);
    private final LendingRepository db;
    private final TransactionTemplate tx;
    private final Availability availability;
    private final LendingProperties props;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();

    LendingService(LendingRepository db, TransactionTemplate tx, Availability availability, LendingProperties props) {
        this.db = db;
        this.tx = tx;
        this.availability = availability;
        this.props = props;
        this.clock = Clock.systemUTC();
    }

    OffsetDateTime now() {
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * The first answer for (member, idempotencyKey) is stored with the loan, in the same transaction, and
     * returned again for a repeat: a retried request borrows nothing more (docs/adr/0005).
     */
    BorrowResult borrow(Member member, BorrowInput input) {
        Optional<InvalidInput> invalid = LendingRules.checkInput(input, props.maxDays());
        if (invalid.isPresent() && invalid.get().field().equals("idempotencyKey")) return invalid.get();   // nothing to store it under
        boolean[] fresh = {false};
        BorrowResult result = tx.execute(status -> {
            db.lockMember(member.id());
            Optional<String[]> stored = db.storedResult(member.id(), input.idempotencyKey());
            if (stored.isPresent()) return replay(stored.get());
            BorrowResult decided = decide(member, input, invalid);
            store(member, input.idempotencyKey(), decided);
            fresh[0] = true;
            return decided;
        });
        Metrics.counter("toolshed.borrow.results", "result", result.getClass().getSimpleName(), "replay", String.valueOf(!fresh[0])).increment();
        // after the commit, and only for a new loan: a replay changes nothing, so it tells no subscriber
        if (fresh[0] && result instanceof Borrowed b)
            availability.publish(new AvailabilityChange(new Tool(b.loan().toolId()), false, now()));
        return result;
    }

    private BorrowResult decide(Member member, BorrowInput input, Optional<InvalidInput> invalid) {
        if (invalid.isPresent()) return invalid.get();
        if (!db.toolKnown(input.toolId())) return new ToolNotFound("no tool with id " + input.toolId(), input.toolId());
        Optional<LoanLimitReached> limit = LendingRules.checkLimit(db.openLoanCount(member.id()), props.loanLimit());
        if (limit.isPresent()) return limit.get();
        Optional<Loan> loan = db.insertLoan(input.toolId(), member.id(), now(), input.days());
        if (loan.isPresent()) return new Borrowed(loan.get());
        Loan open = db.openLoansByTool(java.util.List.of(input.toolId())).get(input.toolId());
        return new ToolUnavailable("this tool is out until " + open.dueAt().toLocalDate(), new Tool(input.toolId()), open.dueAt());
    }

    private void store(Member member, String key, BorrowResult result) {
        String body = result instanceof Borrowed b ? json.writeValueAsString(Map.of("loanId", b.loan().id())) : json.writeValueAsString(result);
        db.storeResult(member.id(), key, result.getClass().getSimpleName(), body);
    }

    private BorrowResult replay(String[] stored) {
        if (stored[0].equals("Borrowed")) {
            String loanId = json.readTree(stored[1]).get("loanId").asString();
            return new Borrowed(db.loan(loanId).orElseThrow());
        }
        return json.readValue(stored[1], STORED.get(stored[0]));
    }

    ReturnResult giveBack(Member member, boolean librarian, ReturnInput input) {
        ReturnResult result = tx.execute(status -> {
            Optional<Loan> loan = db.loan(input.loanId()).filter(l -> LendingRules.canSee(l, member.id(), librarian));
            if (loan.isEmpty()) return new LoanNotFound("no loan of yours with id " + input.loanId(), input.loanId());
            if (loan.get().returnedAt() != null)
                return new AlreadyReturned("this loan was returned on " + loan.get().returnedAt().toLocalDate(), loan.get());
            return db.markReturned(input.loanId(), now()).<ReturnResult>map(Returned::new)
                    .orElseGet(() -> new AlreadyReturned("this loan was just returned", db.loan(input.loanId()).orElseThrow()));
        });
        if (result instanceof Returned r) availability.publish(new AvailabilityChange(new Tool(r.loan().toolId()), true, now()));
        return result;
    }
}
