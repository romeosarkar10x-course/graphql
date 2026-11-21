# Checkpoint 10 quiz

Answer without notes, then check.

1. A request has no token; another has an expired one. What should each get, and from which layer?
2. ben cannot see club c1 through `club(id:)`. Name two other paths you must check.
3. A query has depth 7 and 8 fields and is under every default limit. How can it still take a server down?
4. A rate limit allows 10 requests a minute. Why does it not stop an invite-code brute force?
5. Why is APQ not trust, and what is?
6. How should a public API's client know how long to wait before its next request?
7. Your CORS config allows every origin. Which non-HTTP-request risk does that open in Spring?
8. graphql-cop finds nothing. Name two kinds of vulnerability it would not have found anyway.

<details><summary>Answers</summary>

1. No token: answered anonymously (`viewer: null`). Expired: 401 with `WWW-Authenticate: Bearer
   error="invalid_token"`, from the HTTP layer, before GraphQL parses anything.
2. Any two: `node(id: "c1")`, `node(id: "v1")` for one of its reviews, `book { reviews }`,
   `node(id: "r4") { ... on Reader { clubs } }`, `reviewAdded(clubId: "c1")`.
3. Fan-out: lists inside lists multiply (12 × 10 × 12 ...): lab 10.3's 8-level query made 241,230 fetches and
   one more level ran the fork out of memory. Only a cost limit that multiplies by `first` catches it.
4. One request carries about 1,000 aliased attempts; the limit must count attempts (resolver calls) or cost.
5. APQ stores any document that arrives with its own hash, so an attacker can register theirs. Trusted
   documents are a manifest from your own client build; anything else is refused.
6. From the response: `extensions.cost` with the query's cost, the points available and the restore rate;
   wait (cost − available) / restoreRate.
7. Cross-site WebSocket hijacking: Boot uses the CORS origins to decide which origins may open the GraphQL
   WebSocket, so every origin could open one.
8. Authorization (`node(id:)` leaks, field rules), business logic (mass assignment, attempt limits), token
   handling (`alg: none`), injection behind a specific argument.
</details>

## What `bin/course checkpoint 10` checks

Labs 10.1 to 10.8 pass (the hardened Shelfie: authentication, authorization, demand control, trusted
documents, a public API, transport, scans and regression tests), the capture the flag with 8 or more flags,
and the blue team's patched fork. Files: your `scans/after.json` and `flags.txt` (your CTF score). Record
the score in `games/leaderboards/capture-the-flag.md`. Tag: `checkpoint-10`.
