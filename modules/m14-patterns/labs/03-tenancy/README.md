# Lab 14.3: Multi-tenancy

Lecture: [14.3 Multi-tenancy](../../lectures/14.3-tenancy.md). Grading: auto + rubric. Needs PostgreSQL
(`bin/course ref up`).

Shelfie as a service for two bookshop chains, `acme` (basic plan) and `globex` (premium). Clubs belong to a
tenant; the book catalogue is shared. The starter works, and leaks. Make it isolate tenants:

1. **The tenant comes from the token** (`tenant` claim, Module 10's offline-verified JWTs:
   `bin/course token ada --claim tenant=acme`). Nothing the client types (a header, an argument) chooses it. No
   tenant: `UNAUTHORIZED`.
2. **PostgreSQL row-level security** as the floor under every query: a role `m14_tenant`, policies on
   `m14_03.clubs` and `m14_03.members` that compare `tenant_id` with `current_setting('app.tenant', true)`,
   enabled and **forced**; every request's transaction becomes that role and sets `app.tenant` locally. With no
   tenant set, nothing is visible.
3. **Per-tenant schema visibility**: `Club.analytics` exists for premium tenants only, in introspection and in
   validation.

The grader attacks as ada (acme) through **six paths**: by id, by node id, search, a shared book's
`clubsReading`, `joinClub`, and an `X-Tenant: globex` header, after globex has warmed any caches. Then it checks
the database **directly** (no app in between): the policies, fail-closed, and that acme cannot insert a membership
into globex's club even with a correct `tenant_id` of its own.

```bash
bin/course start m14/03
cd workspace/m14-patterns/03-tenancy
bin/course shell -c 'mvn -q package && java -jar target/shelfie-tenancy.jar'
TOKEN=$(bin/course token ada --claim tenant=acme)
curl -s localhost:4000/graphql -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"query":"{ clubs { id } }"}'
bin/course test m14/03
```

In `notes.md`: the six paths and how each leaked, defence in depth, and visibility per plan.

## Done when

`bin/course test m14/03` passes and `notes.md` passes the rubric (`bin/course grade m14/03`).
