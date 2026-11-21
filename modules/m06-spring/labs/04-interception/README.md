# Lab 6.4 — A tenant, from header to nested field

**Lecture:** [6.4 Interception and customisation](../../lectures/6.4-interception.md) · **Grading:** auto + rubric · **Needs:** nothing

`lab.graphqls` adds `Meetup.startsAtLocal` (in the tenant's time zone) and `Meetup.headline`, marked
`@uppercase`. The `X-Shelfie-Tenant` header names the tenant: `eu` (Europe/Paris), `in` (Asia/Kolkata),
`us` (America/New_York); anything else is `global` (UTC).

| Build | Checked by `tests/lab.hurl` |
|---|---|
| A `WebGraphQlInterceptor` that puts the tenant into GraphQLContext | `club { meetups { startsAtLocal } } }` is right per tenant (T01 to T03) |
| The same interceptor on the way out: header `X-Shelfie-Tenant-Served` and `extensions.tenant` | T03, T04 |
| `startsAtLocal` reads it with `@ContextValue` | the test also checks that `LabController` does not read HTTP headers itself |
| `@uppercase` applied by wiring: a `GraphQlSourceBuilderCustomizer` or `RuntimeWiringConfigurer` bean, not the controller | T05 |

## Done when

`bin/course test m06/04` is green and `bin/course grade m06/04` gives 7/10 or more.
