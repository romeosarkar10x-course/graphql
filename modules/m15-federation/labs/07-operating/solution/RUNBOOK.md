# Runbook: move Review.book from catalog to reviews

Owner: reviews team (driver), catalog team (step 3). Traffic keeps flowing; no step may fail a request.
Accounts and clubs are not touched.

## Before

- Reviews' code already resolves `Review.book` (it has the review's `bookId`); deployed and idle, because the
  schema does not declare the field yet. Check: `{ _entities(representations: [{__typename: "Review", id: "r1"}]) { ... on Review { id } } }`
  answers on the reviews instance.
- Every step below is one team's deploy and one schema publish; CI composes each state before it ships
  (lab 15.5's `ci/compose.sh`). Watch: router error rate by subgraph, reviews' and catalog's request rates, p95.

## Step 1 (states/2): reviews takes the field over

Reviews publishes `book: Book! @override(from: "catalog")` (and imports `@override`). Composition accepts two
definitions of `Review.book` only because one overrides the other; without `@override` it fails with
`INVALID_FIELD_SHARING`. Deploy reviews first (blue/green), then publish the schema.

- Check: the plan for `{ review(id: "r1") { book { title } } }` has no `... on Review` fetch to catalog;
  catalog's entity calls for `Review` drop to zero; reviews' request rate does not change (it was already in the
  plan for `review`).
- Rollback: republish states/1's reviews schema. Catalog still has its resolver, so this is instant.
- Progressive option: `@override(from: "catalog", label: "percent(10)")`, then 50, then 100, watching p95 and
  errors for reviews at each label. Apollo Router 2.19 resolves `percent()` labels itself; Hive Router does too,
  and can also switch a named label by request (a header for internal users). Each label change is a publish.

## Step 2 (states/3): catalog drops the field

Catalog removes `type Review` from its schema and its `Review` resolver from the code, in one deploy. Safe only
after step 1 is live everywhere: the router no longer plans through catalog for `Review.book`.

- Check: catalog's `Review` entity calls are zero before you start (if not, a router still runs the old
  supergraph: stop); after, catalog's error rate is flat.
- Rollback: redeploy catalog from states/2. Nothing routes to it, so no client notices either way.

## Step 3 (states/4): reviews removes @override

Reviews publishes `book: Book!` without `@override`. A no-op for routing (only reviews defines the field now),
but it removes a reference to a field that no longer exists elsewhere and stops the next person from wondering.

- Check: composition passes; the plan is unchanged.
- Rollback: not needed.

## What goes wrong in the other orders

- Catalog first (drop the field, then reviews adds it): between the two publishes `Review.book` is in no
  subgraph, so it disappears from the supergraph and every client query that selects it fails validation. The
  wrong-order run in the workbench failed 193 of 478 requests ("Cannot query field \"book\" on type \"Review\"").
- Reviews adds `book` without `@override`: composition refuses (`INVALID_FIELD_SHARING`), so CI catches it.
- Restarting a subgraph in place instead of blue/green: requests in the restart window fail regardless of the
  schema order. The test deploys a new instance, re-points the supergraph, then stops the old one.

## Around the migration

- Auth: the router forwards `Authorization` and `X-Shelfie-User` (lab 15.6); subgraphs authorise, so a field
  moving between subgraphs takes its authorisation rule with it. Reviews' resolver must check what catalog
  checked.
- Subscriptions and `@defer`: Apollo Router serves `@defer` without a licence but needs a GraphOS plan for
  subscriptions; Hive Router 0.3.9 has subscriptions and rejects `@defer`. A moved field inside a deferred
  fragment follows the same plan rules.
- Contracts: if a public contract filters by `@tag`, the moved field must carry the same tag in reviews, or it
  silently leaves the contract.
