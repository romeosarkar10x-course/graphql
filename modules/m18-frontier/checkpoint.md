# Checkpoint 18

`bin/course checkpoint 18` checks lab 18.3 (the MCP server with its safety policy) and lab 18.6 (the ported
subgraph), and that `REDTEAM.md` and `PORT.md` are in your workspace, then tags `checkpoint-18`. The written
parts are graded separately (`bin/course grade m18/NN`). Answer these first, without looking.

1. An agent's query is valid, cheap and returns 200 with no errors, and the answer is wrong. Give Shelfie's
   example and the sentence that prevents it.
2. Why does an MCP server for Shelfie strip descriptions from its operations before sending them?
3. What do curated tools rule out that `run_graphql` allows? Name three.
4. `run_graphql` refuses mutations. Why does it also refuse a document with two operations?
5. Who approves a write in lab 18.3, through what, and why can the agent not do it itself?
6. PostGraphile generated `readerByOwnerId: Reader`. Name two things a hand-built Shelfie schema would do
   differently, and why.
7. Error behaviors (#1163) and service capabilities (#1208): stage, the change each makes, and why one needs the
   other.
8. Your TypeScript port passes every Hurl check but the test still fails. What did it catch, and why could Hurl
   not?

<details><summary>Answers</summary>

1. "When was the October wrap-up?" with `meetups` and no arguments: `upcomingOnly` defaults to true and hides
   past meetups. ClubMeetups' description says to pass `$upcomingOnly: false` for the past.
2. The September 2025 edition allows descriptions on operations, but Shelfie's graphql-java 26.1 rejects them;
   they are for the agent only.
3. Any three of: mutations, introspection, unbounded fan-out (no cost limit), selecting fields no operation
   selects, multi-operation documents.
4. So that a mutation cannot hide next to a query; and with one operation, what is checked is what runs.
5. A human, with `POST /approvals/{id}` and the approver token, a route outside MCP; the agent never sees the
   token, so it cannot grant its own request, and the approval is bound to the reader and used once.
6. Product names (`owner: User!`), non-null where the domain guarantees it (the column is NOT NULL), and a
   contract that survives a column rename through deprecation; plus authorization and cost limits outside SQL.
7. Both RFC 1 (Proposal) on 2026-11-23. #1163 adds the request's `onError` (`PROPAGATE`, `NULL`, `HALT`); #1208
   adds `__service { capabilities }` and SDL to advertise features. A client should only send `onError` to a
   server that advertises `graphql.onError`.
8. The field signatures from introspection: a nullability difference (Pothos's nullable default) returns the same
   data, so every Hurl assertion passes, but it is a different API.

</details>
