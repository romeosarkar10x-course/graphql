package dev.toolshed.lending;

import dev.toolshed.common.BadInput;
import dev.toolshed.common.Forbidden;
import dev.toolshed.lending.Model.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Lending's resolvers. Authorization is here, per field (docs/DESIGN.md, "Authorization"):
 * Member.email and Loan.member for that member or a librarian, overdueLoans for librarians,
 * Tool.currentLoan for signed-in members.
 */
@Controller
class LendingController {
    static final int MAX_PAGE = 50;
    private final LendingRepository db;
    private final LendingService lending;
    private final Availability availability;

    LendingController(LendingRepository db, LendingService lending, Availability availability, BatchLoaderRegistry loaders) {
        this.db = db;
        this.lending = lending;
        this.availability = availability;
        // Loan.member checks access per loan, then loads through this DataLoader: one statement for a page.
        loaders.forTypePair(String.class, Member.class).registerMappedBatchLoader((ids, env) ->
                Mono.fromCallable(() -> index(db.membersByIds(ids), Member::id)));
    }

    @QueryMapping
    Member me(@ContextValue Caller caller) {
        return caller.member().orElse(null);
    }

    @QueryMapping
    Loan loan(@Argument String id, @ContextValue Caller caller) {
        if (caller.member().isEmpty()) return null;
        return db.loan(id).filter(l -> LendingRules.canSee(l, caller.member().get().id(), caller.librarian())).orElse(null);
    }

    @QueryMapping
    LoanConnection overdueLoans(@Argument int first, @Argument String after, @ContextValue Caller caller) {
        caller.require();
        if (!caller.librarian()) throw new Forbidden("librarians only");
        return db.overdue(pageSize(first), after, lending.now());
    }

    @MutationMapping
    BorrowResult borrowTool(@Argument BorrowInput input, @ContextValue Caller caller) {
        return lending.borrow(caller.require(), input);
    }

    @MutationMapping
    ReturnResult returnTool(@Argument ReturnInput input, @ContextValue Caller caller) {
        return lending.giveBack(caller.require(), caller.librarian(), input);
    }

    @SubscriptionMapping
    Flux<AvailabilityChange> availabilityChanged(@Argument String toolId) {
        return availability.changes(toolId);
    }

    /** The router's _entities call for Tool: lending answers for any id; unknown tools are simply never out. */
    @EntityMapping
    List<Tool> tool(@Argument List<String> idList) {
        return idList.stream().map(Tool::new).toList();
    }

    @EntityMapping
    List<Member> member(@Argument List<String> idList) {
        Map<String, Member> byId = index(db.membersByIds(idList), Member::id);
        return idList.stream().map(byId::get).toList();
    }

    @BatchMapping(typeName = "Tool")
    Map<Tool, Boolean> available(List<Tool> tools) {
        Map<String, Loan> open = db.openLoansByTool(tools.stream().map(Tool::id).toList());
        return tools.stream().collect(Collectors.toMap(t -> t, t -> !open.containsKey(t.id()), (a, b) -> a, LinkedHashMap::new));
    }

    /** Signed in only: the whole field is refused for anonymous callers (null and an UNAUTHORIZED error). */
    @SchemaMapping(typeName = "Tool")
    CompletableFuture<Loan> currentLoan(Tool tool, DataLoader<String, Loan> openLoans, @ContextValue Caller caller) {
        caller.require();
        return openLoans.load(tool.id());
    }

    @SchemaMapping(typeName = "Member")
    String email(Member member, @ContextValue Caller caller) {
        requireSelfOrLibrarian(caller, member.id());
        return member.email();
    }

    @SchemaMapping(typeName = "Member")
    LoanConnection loans(Member member, @Argument int first, @Argument String after, @Argument LoanStatus status,
                         @ContextValue Caller caller) {
        requireSelfOrLibrarian(caller, member.id());
        return db.memberLoans(member.id(), status, pageSize(first), after, lending.now());
    }

    @SchemaMapping(typeName = "Loan")
    Tool tool(Loan loan) {
        return new Tool(loan.toolId());
    }

    @SchemaMapping(typeName = "Loan")
    CompletableFuture<Member> member(Loan loan, DataLoader<String, Member> members, @ContextValue Caller caller) {
        requireSelfOrLibrarian(caller, loan.memberId());
        return members.load(loan.memberId());
    }

    @SchemaMapping(typeName = "Loan")
    LoanStatus status(Loan loan) {
        return loan.status(lending.now());
    }

    private static void requireSelfOrLibrarian(Caller caller, String memberId) {
        caller.require();
        if (!caller.is(memberId) && !caller.librarian()) throw new Forbidden("only that member and librarians can see this");
    }

    static int pageSize(int first) {
        if (first < 1 || first > MAX_PAGE) throw new BadInput("first", "first must be between 1 and " + MAX_PAGE);
        return first;
    }

    private static <T> Map<String, T> index(Collection<T> items, Function<T, String> id) {
        return items.stream().collect(Collectors.toMap(id, t -> t));
    }
}
