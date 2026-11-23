package dev.toolshed.lending;

import dev.toolshed.common.Cursors;
import dev.toolshed.lending.Model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for lending. List reads take every id at once: one statement per batch. */
@Repository
class LendingRepository {
    private static final String LOAN = "SELECT id, tool_id, member_id, borrowed_at, due_at, returned_at FROM toolshed_lending.loan";
    private final JdbcClient db;

    LendingRepository(JdbcClient db) {
        this.db = db;
    }

    Optional<Member> memberByHandle(String handle) {
        return db.sql("SELECT id, handle, display_name, email FROM toolshed_lending.member WHERE handle = ?").param(handle).query(Member.class).optional();
    }

    List<Member> membersByIds(Collection<String> ids) {
        return db.sql("SELECT id, handle, display_name, email FROM toolshed_lending.member WHERE id = ANY(?)").param(ids.toArray(String[]::new)).query(Member.class).list();
    }

    /** Locks the member's row: one borrow at a time per member, so the limit check and the key check cannot race. */
    void lockMember(String memberId) {
        db.sql("SELECT id FROM toolshed_lending.member WHERE id = ? FOR UPDATE").param(memberId).query(String.class).single();
    }

    Optional<Loan> loan(String id) {
        return db.sql(LOAN + " WHERE id = ?").param(id).query(LendingRepository::loan).optional();
    }

    /** The open loan of each tool that has one. */
    Map<String, Loan> openLoansByTool(Collection<String> toolIds) {
        Map<String, Loan> open = new HashMap<>();
        db.sql(LOAN + " WHERE tool_id = ANY(?) AND returned_at IS NULL").param(toolIds.toArray(String[]::new))
                .query(LendingRepository::loan).list().forEach(l -> open.put(l.toolId(), l));
        return open;
    }

    int openLoanCount(String memberId) {
        return db.sql("SELECT count(*) FROM toolshed_lending.loan WHERE member_id = ? AND returned_at IS NULL").param(memberId).query(Integer.class).single();
    }

    boolean toolKnown(String toolId) {
        return db.sql("SELECT count(*) FROM toolshed_lending.tool_stock WHERE tool_id = ?").param(toolId).query(Integer.class).single() > 0;
    }

    /** Empty when the tool already has an open loan: the partial unique index decides, not a read before the write. */
    Optional<Loan> insertLoan(String toolId, String memberId, OffsetDateTime now, int days) {
        return db.sql("""
                INSERT INTO toolshed_lending.loan (tool_id, member_id, borrowed_at, due_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (tool_id) WHERE returned_at IS NULL DO NOTHING
                RETURNING id, tool_id, member_id, borrowed_at, due_at, returned_at""")
                .params(toolId, memberId, now, now.plusDays(days)).query(LendingRepository::loan).optional();
    }

    /** Empty when the loan was already returned (or does not exist). */
    Optional<Loan> markReturned(String loanId, OffsetDateTime now) {
        return db.sql("UPDATE toolshed_lending.loan SET returned_at = ? WHERE id = ? AND returned_at IS NULL "
                        + "RETURNING id, tool_id, member_id, borrowed_at, due_at, returned_at")
                .params(now, loanId).query(LendingRepository::loan).optional();
    }

    Optional<String[]> storedResult(String memberId, String key) {
        return db.sql("SELECT result_type, result::text FROM toolshed_lending.borrow_request WHERE member_id = ? AND idempotency_key = ?")
                .params(memberId, key).query((rs, i) -> new String[] {rs.getString(1), rs.getString(2)}).optional();
    }

    void storeResult(String memberId, String key, String type, String json) {
        db.sql("INSERT INTO toolshed_lending.borrow_request (member_id, idempotency_key, result_type, result) VALUES (?, ?, ?, ?::jsonb)")
                .params(memberId, key, type, json).update();
    }

    /** A member's loans, newest first, by keyset on (borrowed_at, id). */
    LoanConnection memberLoans(String memberId, LoanStatus status, int first, String after, OffsetDateTime now) {
        List<Object> params = new ArrayList<>(List.of(memberId));
        StringBuilder where = new StringBuilder(" WHERE member_id = ?");
        if (status != null) {
            switch (status) {
                case RETURNED -> where.append(" AND returned_at IS NOT NULL");
                case OPEN -> { where.append(" AND returned_at IS NULL AND due_at >= ?"); params.add(now); }
                case OVERDUE -> { where.append(" AND returned_at IS NULL AND due_at < ?"); params.add(now); }
            }
        }
        if (after != null) {
            List<String> key = Cursors.decode(after, 2);
            where.append(" AND (borrowed_at, id) < (?::timestamptz, ?)");
            params.add(key.get(0));
            params.add(key.get(1));
        }
        params.add(first + 1);
        List<Loan> rows = db.sql(LOAN + where + " ORDER BY borrowed_at DESC, id DESC LIMIT ?").params(params).query(LendingRepository::loan).list();
        return connection(rows, first, l -> Cursors.encode(l.borrowedAt().toString(), l.id()));
    }

    /** Open loans past due, oldest due first, by keyset on (due_at, id). */
    LoanConnection overdue(int first, String after, OffsetDateTime now) {
        List<Object> params = new ArrayList<>(List.of(now));
        String where = " WHERE returned_at IS NULL AND due_at < ?";
        if (after != null) {
            List<String> key = Cursors.decode(after, 2);
            where += " AND (due_at, id) > (?::timestamptz, ?)";
            params.add(key.get(0));
            params.add(key.get(1));
        }
        params.add(first + 1);
        List<Loan> rows = db.sql(LOAN + where + " ORDER BY due_at, id LIMIT ?").params(params).query(LendingRepository::loan).list();
        return connection(rows, first, l -> Cursors.encode(l.dueAt().toString(), l.id()));
    }

    private static LoanConnection connection(List<Loan> rows, int first, java.util.function.Function<Loan, String> cursor) {
        List<LoanEdge> edges = rows.stream().limit(first).map(l -> new LoanEdge(cursor.apply(l), l)).toList();
        return new LoanConnection(edges, new PageInfo(rows.size() > first, edges.isEmpty() ? null : edges.getLast().cursor()));
    }

    private static Loan loan(ResultSet rs, int row) throws SQLException {
        return new Loan(rs.getString("id"), rs.getString("tool_id"), rs.getString("member_id"),
                rs.getObject("borrowed_at", OffsetDateTime.class), rs.getObject("due_at", OffsetDateTime.class),
                rs.getObject("returned_at", OffsetDateTime.class));
    }
}
