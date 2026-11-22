# Lab 14.3 notes

## The six paths

By id, by node id, search, a shared book's `clubsReading`, `joinClub`, and the `X-Tenant` header. The starter
filtered by tenant on one of them (`club(id:)`) and still leaked there, through a cache keyed by id alone that
globex had warmed. Each path is a separate SQL statement, and every new one is a new chance to forget the
`WHERE tenant_id = ?`. The shared catalogue is the trap: books have no tenant, so a book's clubs looked harmless.

## Defence in depth

The tenant comes from the verified token only; a header is something the attacker types. Every transaction runs
`SET LOCAL ROLE m14_tenant` and `set_config('app.tenant', ..., true)`, and row-level security filters every table
with a policy, forced so even the owner obeys. LOCAL matters with a pool: the setting dies with the transaction.
Fail closed: with no tenant set, the policy compares with NULL and no row is visible. Two things RLS does not
cover: a foreign key check ignores policies (the composite key `(tenant_id, club_id)` closes it), and anything
cached outside the database must have the tenant in its key.

## Visibility per plan

`Instrumentation.instrumentSchema` returns a schema per execution; basic tenants get one without
`Club.analytics`, built once and cached. It is hidden from introspection and fails validation like a field that
never existed. This is not security on its own: the resolver must still refuse if the schema ever leaked it.
Cost: two schemas to test, and clients that cache the schema per endpoint must cache it per tenant.
