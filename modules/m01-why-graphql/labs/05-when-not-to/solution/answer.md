# Lab 1.5 (reference)

## 1. GitHub's four strongest reasons (2016)
1. **Database load from chatty clients.** The REST API made "over 60% of the requests made to our
   database tier", partly because hypermedia navigation makes clients call back repeatedly. A
   measured cost, not a taste.
2. **Responses both too big and not enough.** Bloated payloads full of `*_url` hints, yet "two or three
   separate calls to assemble a complete view of a resource". Over- and under-fetching at once.
3. **Metadata they could not express.** Per-endpoint OAuth scopes, smarter pagination, type-safety for
   user-supplied parameters. A typed schema carries all of it.
4. **Generated docs and clients.** They wanted documentation generated from code and clients generated
   instead of hand-patching Octokit; introspection gives both.

## 2. Which of them apply to Shelfie?
1. Applies: the club page is 11 REST calls (lab 1.2).
2. Applies: about 85% of downloaded bytes were unused on the club page.
3. Partly: Shelfie's roles (member, moderator, owner) need per-field rules (Module 10); scopes less so.
4. Applies once friends build clients: generated types (Module 12) instead of hand-written DTOs.

## 3. Three Shelfie features that should NOT be GraphQL
1. **Book cover uploads:** binary files. Use a pre-signed URL to object storage (Module 4.6); GraphQL
   only records the result.
2. **The public, rarely-changing book catalogue page** behind a CDN: plain `GET /books/{isbn}` caches by
   URL everywhere for free.
3. **Notifying a partner's system when a meetup is created:** a webhook (server-to-server push to a URL
   they own), not a subscription that needs a long-lived connection.
Also acceptable: health checks, OAuth flows, file downloads (exports), simple internal CRUD admin.
