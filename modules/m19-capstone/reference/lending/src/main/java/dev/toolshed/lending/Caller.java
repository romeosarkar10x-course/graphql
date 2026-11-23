package dev.toolshed.lending;

import dev.toolshed.common.Unauthenticated;
import dev.toolshed.lending.Model.Member;
import java.util.Optional;

/**
 * Who is calling, resolved once per request by CallerInterceptor and read by resolvers as
 * {@code @ContextValue Caller caller}: no field looks the member up again.
 */
record Caller(Optional<Member> member, boolean librarian) {
    static final Caller ANONYMOUS = new Caller(Optional.empty(), false);

    Member require() {
        return member.orElseThrow(() -> new Unauthenticated("sign in as a member first"));
    }

    boolean is(String memberId) {
        return member.map(m -> m.id().equals(memberId)).orElse(false);
    }
}
