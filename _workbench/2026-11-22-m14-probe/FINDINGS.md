# Module 14 probe findings (2026-11-22)

Everything below was run in this folder or by the labs' tests; files are named per finding.

1. **Directive order** (graphql-java 26.1 through Spring for GraphQL 2.1): `SchemaDirectiveWiring.onField` is called
   in the order the directives are written on the field; each wraps the fetcher it finds, so the first written
   transforms first. `@currency(code: "GBP") @mask(keep: 2)` gives `*******62`, the reverse order gives a field
   error. `spring/probe-out.txt` (WIRING/RUN lines), lab 14.1. Spec 2.13 "Directive Order Is Significant"
   (`spec/directive-order.txt`).
2. A `GraphQLException` thrown from a data fetcher reaches the client as `INTERNAL_ERROR for <executionId>` (Spring's
   unresolved-exception handling); a `DataFetcherResult` with an error keeps the message. Throwing in `onField` stops
   the start-up with the message, which names the field.
3. **Spring's `Locale` argument** is the first `Accept-Language` range (`de-DE,de;q=0.9` gives `de_DE`), and the JVM
   default (`en_US` in the course image) with no header. `spring/probe-out.txt`.
4. **CLDR in JDK 25**: `12,50 €` with U+00A0 (de), `1 234,56 €` with U+202F thousands and U+00A0 before € (fr),
   `3:30 AM` with U+202F (en-US), `¥1,500` for JPY in English. `locales-solution.txt` (cat -A form).
5. extended-scalars 24.0 `DateTime` on graphql-java 26.1 serialises with milliseconds:
   `2026-12-03T19:30:00.000+01:00`.
6. **`Instrumentation.instrumentSchema`** is consulted per execution, before validation: a transformed schema per
   tenant hides a field from introspection and validation (`FieldUndefined`). `spring/probe-out.txt`, lab 14.3.
7. **PostgreSQL 18.7 row-level security**: the course's `shelfie` user is a superuser and sees every row regardless
   of policies; `SET LOCAL ROLE` to a NOLOGIN role works inside a superuser's transaction; with no `app.tenant`
   set, `current_setting(..., true)` is NULL and no row matches. **A single-column foreign key lets a tenant insert
   a row pointing at another tenant's row** (FK checks ignore policies); a composite key `(tenant_id, club_id)`
   refuses it. `rls/fk-probe.txt`, lab 14.3's `RlsCheck`.
8. **Open Library** (recorded 2026-11-22, `openlibrary/`): `/api/books?bibkeys=...&jscmd=data` answers one object
   keyed by bibkey, `{}` with 200 for an unknown ISBN; `9780000000002` is a real record ("The three voices of
   poetry"). No cache headers (date, content-type, `x-served-by: FastAPI` only). 0.7–0.9 s per call. Data quirks:
   Piranesi has no cover, The Remains of the Day no page count, "The dispossessed", search gives Klara and the Sun
   first published 2019 and The Buried Giant 2001.
9. **Boot 4.2 has no `RestClient.Builder` bean without `spring-boot-starter-restclient`**: start-up failed with
   "required a bean of type 'org.springframework.web.client.RestClient$Builder'". `RestClient.builder()` works.
10. **`@SchemaMapping` infers the GraphQL type from the first parameter's class name**: a method taking `BookDto`
    registered `BookDto.reviews`, silently unused; `Book.reviews` returned null (a non-null error). `typeName = "Book"`
    fixes it. `bff-solution.txt` history in the session; lab 14.4.
11. Resilience4j 2.4.0 breaker (window 10, minimum 4 calls, 50%, open 3 s, 2 half-open calls): with the fake down for
    20 pages, at most 8 calls reached it (0 in the probe run, the breaker already open); the field returned 3.5 s
    after recovery. `bff-solution.txt`.
12. Spring's DataLoader batch for three root aliases, a list and a repeated ISBN sent **one** `/api/books` call with
    the four distinct keys (lab 14.10).
13. **graphql-java's good-faith introspection** (on by default through Spring for GraphQL 2.1): two `__type` fields
    in one request, or `__Type.fields` under both `queryType` and `mutationType`, are refused with
    `BadFaithIntrospection` ("Query.__type is present too often!"). `good-faith-introspection.txt`.
14. A `GraphQlSource` bean of your own replaces Boot's; Spring asks it for `graphQl()` per request, so swapping an
    `AtomicReference<GraphQL>` changes the schema without a restart: 150 queries during six swaps, none failed (lab
    14.7). A second endpoint built from `DefaultExecutionGraphQlService`, `WebGraphQlHandler` and webmvc
    `GraphQlHttpHandler` serves its own schema at another route in the same JVM.
15. **Outbox + Kafka 4.4.0**: the mutation's payload is PENDING deterministically (the projection cannot run before
    commit); PUBLISHED about 0.5 s later with a 500 ms relay. Clearing `published_at` republished the event (two
    records on the topic) and the projection's `processed_events` kept the count at 3 (lab 14.5).
16. Hurl 7: a JSONPath that matches one element returns the element, not a list (`count` fails); headers must come
    before `[Options]`; `includes` is deprecated in favour of `contains`.
17. RFC #472 (repeatable directives) was merged 2020-01-10; wg#1096 Metadata Directives is RFC 0 (strawman, opened
    2022-08-04, updated 2024-11-27). `spec/rfc-472.txt`, `spec/rfc-wg1096.txt`.
18. The Module 10 token tool now takes `--claim k=v` (string claims), used for the tenant claim.
