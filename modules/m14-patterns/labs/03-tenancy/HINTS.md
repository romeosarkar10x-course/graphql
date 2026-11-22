# Hints: m14/03

1. Fix the source first: the interceptor should read the tenant from the `JwtAuthenticationToken` only. Then list
   every SQL statement in the controller and ask which ones filter by tenant; the shared `books` table is where a
   filter is easiest to forget. A cache keyed by id alone serves whoever asked first.
2. Row-level security never applies to a superuser (the course's `shelfie` user is one) and applies to a table's
   owner only with `FORCE ROW LEVEL SECURITY`. So the app connects as `shelfie` and, inside each transaction,
   runs `SET LOCAL ROLE m14_tenant` and `SELECT set_config('app.tenant', ?, true)`: both end with the transaction.
3. Foreign key checks do not see policies: a single-column foreign key from `members.club_id` lets acme insert a
   row pointing at globex's club. Make the key cover the tenant too. For visibility, graphql-java asks an
   `Instrumentation` for the schema of each execution (`instrumentSchema`), before validation.
