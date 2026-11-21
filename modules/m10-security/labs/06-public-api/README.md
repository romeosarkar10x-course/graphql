# Lab 10.6: Public API security

Lecture: [10.6 Public API security](../../lectures/10.6-public-api-security.md). Grading: auto + rubric. Needs nothing.

Shelfie opens its API to other people's apps. You cannot allowlist their documents (lab 10.5 works only for
your own clients), so identify each app, limit what it may do and how much, and tell it where it stands.

The starter is lab 10.4's solution, whose budget is per reader. Change it to:

| Key | App | Scopes | Bucket |
|---|---|---|---|
| `pk_reader_1` | bookworm-app | read | 1,000 points, restores 50/s |
| `pk_writer_2` | review-bot | read, write:reviews | 1,000, 50/s |
| `pk_spender_3` | spender | read | 1,000, 1,000/s |
| `pk_admin_4` | club-tool | read, write:reviews, write:clubs | 1,000, 50/s |

- `X-Api-Key` is required: missing or unknown is 401, before GraphQL runs.
- `addReview`/`removeReview` need `write:reviews`; `renameClub`/`joinClub` need `write:clubs`. Lab 10.2's
  rules still apply to the user in the bearer token.
- Every response carries Shopify's shape: `extensions.cost.requestedQueryCost` and
  `throttleStatus {maximumAvailable, currentlyAvailable, restoreRate}`. Over budget: an error with
  `extensions.code: THROTTLED` and the same `cost` block, no `data`.
- `GET /_admin/usage` returns `{"<app>": {"served": n, "throttled": n}}`.

Then the experiment: `client/Spender.java` sends a 140-point query 40 times and must back off correctly.

```bash
bin/course start m10/06
bin/course java client/Spender.java http://localhost:4000/graphql pk_spender_3 40     # inside bin/course shell
bin/course test m10/06
```

## Done when

`bin/course test m10/06` passes: 9 Hurl checks; Spender gets all 40 answers with at most 2 throttled
responses in 15 s or less; and `notes.md` passes the rubric (`bin/course grade m10/06`).
