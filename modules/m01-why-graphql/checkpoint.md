# Module 1 checkpoint

**1. Facebook started GraphQL in 2012. What did News Feed lack that native apps needed?**

<details><summary>Answer</summary>A data API: News Feed was only delivered as HTML.</details>

**2. A screen needs A, then B and C (both need A), then D (needs C). RTT 500 ms. Fastest REST time? GraphQL time?**

<details><summary>Answer</summary>3 waves (A; B, C; D) = 1.5 s. GraphQL: one round trip, 0.5 s, plus server time.</details>

**3. Name two things GraphQL loses compared with REST.**

<details><summary>Answer</summary>URL-based HTTP/CDN caching; meaningful per-endpoint status codes and metrics; simplicity. Any two.</details>

**4. The gRPC service has both `GetClub` and `GetClubPage`. What problem does the second one show?**

<details><summary>Answer</summary>Endpoint sprawl: a server-side endpoint per screen, written and versioned by the backend team for each client need.</details>

**5. Without added latency, REST was faster than GraphQL in the reference measurement. Why?**

<details><summary>Answer</summary>Round trips on localhost cost almost nothing, while GraphQL adds parsing, validation and execution per request.</details>

**6. Which style for: an internal 20k req/s Java-to-Java call with streaming? A partner ping when a meetup is created? A public, identical-for-everyone catalogue page?**

<details><summary>Answer</summary>gRPC; a webhook; REST behind a CDN (or a static file).</details>

**7. What did GitHub measure about its REST API that drove the switch?**

<details><summary>Answer</summary>REST was responsible for over 60% of requests to its database tier; integrators needed 2–3 calls to assemble one view.</details>

**8. Does the GraphQL spec say queries are sent as HTTP POST to `/graphql`?**

<details><summary>Answer</summary>No. The core spec is transport-agnostic; the separate GraphQL-over-HTTP spec covers HTTP.</details>
