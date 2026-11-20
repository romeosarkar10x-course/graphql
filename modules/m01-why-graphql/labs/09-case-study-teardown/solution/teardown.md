# Case-study teardown (reference: GitHub)

## 1. The architecture
```
third-party integrators ─┐                 ┌─ REST API v3 (kept)
GitHub's own front end ──┼── HTTPS ──┬─────┤
                         │           └── GraphQL API v4 ── Rails app (graphql-ruby) ── databases
                         └── one schema for internal and public use; cost-based rate limits
```

## 2. The problem that drove it
REST was "responsible for over 60% of the requests made to our database tier", because hypermedia
navigation forces clients to call back repeatedly, and integrators needed "two or three separate calls to
assemble a complete view of a resource". They also wanted scopes, pagination and type-safety expressed
per field, plus generated docs and clients.

## 3. Copy for Shelfie
- **Cost-based limits for a public API** (Module 10.6): Shelfie friends' apps are third parties too.
- **One schema used by Shelfie's own UI and by outside clients**, so the public API never lags the product (Module 7).

## 4. Do not copy
Running REST and GraphQL as two full public APIs side by side: GitHub had a decade of REST integrators to
keep; Shelfie has none, so maintaining two would be pure cost.

## 5. The most transferable lesson
Start from a measured pain (their 60% database load), not from the technology.
