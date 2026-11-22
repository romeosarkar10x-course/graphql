# Reading notes: two open RFCs

Reference notes, read 2026-11-23 from the RFC tracker's pages, which reproduce each pull request's description
and timeline (`_workbench/2026-11-23-m18-probe/rfcs/rfc-1163.txt`, `rfc-1208.txt`). A learner's notes should go
further: into the spec diff itself (the "Files changed" tab of the pull request).

## RFC 1: Error behaviors (#1163)

Stage RFC 1 (Proposal), champion Benjie (@benjie), opened 2025-04-30; replaces #1050, #1145 and #1153.
graphql-js implementation in graphql-js#4364.

- **Problem.** Error propagation ("null bubbling") keeps the non-null promise by nulling the nearest nullable
  parent. It destroys sibling data, makes errored responses unsafe for normalized caches, and pushed schema
  design towards nullable-by-default.
- **Change.** A request property `onError` with `PROPAGATE` (today's behaviour), `NULL` (null at the error
  position only) and `HALT` (stop; the former name was `ABORT`). The working group decided on 2026-08-06 that
  `PROPAGATE` stays the default forever, so the "default error behaviour" capability was removed; the proposal
  suggests the `onError` value can be stored with a trusted document. A `pathNonNull` addition (2026-08-03) lets
  clients re-create propagation locally.
- **Example.** `{"query": "{ book(id: \"b1\") { title averageRating } }", "onError": "NULL"}` where
  `averageRating: Float!` fails: `{"data": {"book": {"title": "...", "averageRating": null}}, "errors": [...]}`
  instead of `"book": null`.
- **For graphql-java.** It has an experimental, operation-level `@experimental_disableErrorPropagation`
  directive behind a flag; the request property and the HALT mode would be new.

## RFC 2: Service capabilities (#1208)

Stage RFC 1 (Proposal, advanced 2026-04-02), champion Benjie; split out of #1163 in January 2026 because it is
useful alone, and named as part of the "Golden Path" work on client auto-configuration.

- **Change.** A `__service` meta-field returning `__Service { capabilities: [__Capability!]! }`, each capability
  an `identifier` (a qualified, reverse-domain name; `graphql.` reserved for the Foundation's specs), an optional
  `description` and an optional string `value`. Clients probe support with `{ __type(name: "__Service") { name } }`.
  SDL gains `service { "description" capability graphql.operationDescriptions }` and `extend service`.
- **Why "capabilities", not "metadata".** Every entry should be actionable by tooling; the name discourages a
  dumping ground.
- **Uses.** Executable descriptions (Module 17: graphql-java 26.1 still rejects them), fragment arguments,
  incremental delivery and its version, WebSocket endpoints, document persistence.

## How they relate

#1208 is a prerequisite for using #1163 safely: a client should only send `onError: "NULL"` to a server that
advertises `graphql.onError`. Whichever merges second adds that capability. Both touch introspection
(Section 4), and #1163 also changes the execution algorithm's error handling (Section 6) and the request
(Section 7). For a Java server, #1208 is mostly schema and introspection work; #1163 reaches into the executor.
