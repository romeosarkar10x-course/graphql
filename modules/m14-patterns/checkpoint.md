# Checkpoint 14

`bin/course checkpoint 14` checks labs 14.1 to 14.4, 14.6, 14.7 and mini-project 14.10 (directives, tenants and the
Open Library gateway), then tags `checkpoint-14`. Answer these first, without looking.

1. `@mask @currency` and `@currency @mask` on the same field give different results. Why, and what does the spec
   say?
2. With no Accept-Language header, what `Locale` does a Spring for GraphQL controller receive?
3. Every Shelfie table has a row-level security policy and the app connects as `postgres`. Is it isolated?
4. A foreign key from `members.club_id` to `clubs.id`, both tables with policies. What can still go wrong?
5. Open Library hangs. What three things keep the book page fast and Open Library unharmed?
6. Why does the outbox relay deliver at least once, and what makes that safe?
7. An erased user's id is on 40 audit rows. What should happen to them?
8. How can a field appear in a schema without a restart, and why are requests in flight safe?

<details><summary>Answers</summary>

1. graphql-java wires directives in the order written, each wrapping the data fetcher before it, so the first
   written transforms first. The spec: "Directive Order Is Significant".
2. The JVM's default locale (`en_US` in the course image), which is the server's, not the reader's.
3. No: superusers bypass row-level security. Switch to a plain role per transaction (and FORCE for the owner).
4. Foreign key checks ignore policies: a tenant can insert a membership pointing at another tenant's club. Make the
   key include the tenant.
5. A read timeout, a circuit breaker that stops calling while it fails, and a nullable field with an error so only
   that field is lost (plus caching and back-off in the gateway).
6. It marks a row only after the broker acknowledged it, so a crash in between sends it again; the projection
   records each event id and ignores repeats.
7. Keep the events, break the link: rewrite the actor to `erased` (or a pseudonym whose key is destroyed).
8. Build a new `GraphQLSchema` and swap the whole `GraphQL` instance the `GraphQlSource` returns; each request gets
   one instance and finishes on it.

</details>
