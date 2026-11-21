# Module 10 — Security

About 9 hours, 8 lectures. By the end you can lock a GraphQL API against its own attack surface, not just
generic HTTP threats, and prove it with an attack you ran yourself.

## Before you start

`bin/course ref up` now also starts **Keycloak 26.8.2** (realm `shelfie`, `http://keycloak:8180` inside the
course image, `localhost:8180` from the host, admin console `admin`/`admin`).

- `contract/secure.graphqls` is Module 10's Shelfie: readers, clubs (c1 and c3 private), books, reviews,
  served from memory by `Store.java` (40 readers, 12 clubs, 60 books, about 300 reviews).
- Users: ada (r1, member), ben (r2, member), max (r3, moderator), olu (r4, owns c1), root (r5, admin);
  Keycloak passwords are `<user>-pw`.
- `bin/course token <user> [--expired|--forged|--none|--aud a|--iss i]` mints a JWT signed with the course
  key. The key is in the repository on purpose (`contract/keys/README.md`): never do that with a real one.
  From lab 10.2 on, apps verify tokens offline with `course-public.pem`; only lab 10.1 needs Keycloak.
- `contract/vulnerable-shelfie/` is a deliberately vulnerable fork (labs 10.3, 10.8, 10.9, 10.10).
  `contract/vulnerable.sh start` runs it with a 512 MB heap; `contract/attack.sh` measures one request;
  `contract/Shape.java` prints a document's depth, fields and aliases.
- `bin/course seckit graphql-cop|clairvoyance ...` runs the scanners in their own image (lab 10.8).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 10.1 | [Authentication](lectures/10.1-authentication.md) | [m10/01 Spring Security, Keycloak, viewer](labs/01-authentication/README.md) | auto + rubric |
| 10.2 | [Authorization](lectures/10.2-authorization.md) | [m10/02 Row and field rules, @PreAuthorize, @auth](labs/02-authorization/README.md) | auto |
| 10.3 | [The attack surface](lectures/10.3-attack-surface.md) | [m10/03 Nine attacks on a vulnerable Shelfie](labs/03-attack-surface/README.md) | auto + rubric |
| 10.4 | [Demand control](lectures/10.4-demand-control.md) | [m10/04 Depth, cost, caps, timeout, budget](labs/04-demand-control/README.md) | auto + rubric |
| 10.5 | [Trusted documents](lectures/10.5-trusted-documents.md) | [m10/05 A manifest from the client build](labs/05-trusted-documents/README.md) | auto + rubric |
| 10.6 | [Public API security](lectures/10.6-public-api-security.md) | [m10/06 Keys, scopes, cost budgets](labs/06-public-api/README.md) | auto + rubric |
| 10.7 | [Transport and WebSocket security](lectures/10.7-transport-websocket.md) | [m10/07 connection_init, origins, logs, errors](labs/07-transport-websocket/README.md) | auto + rubric |
| 10.8 | [Security testing toolkit](lectures/10.8-security-toolkit.md) | [m10/08 Scan, fix, regression-test](labs/08-security-toolkit/README.md) | auto + rubric |

Each lab's starter is the previous lab's solution where they build on each other (10.1 → 10.2 → 10.4 →
10.5 and 10.6; 10.2 → 10.7 → 10.8), so the hardened Shelfie grows one layer at a time.

## Games and mini-project

| | Solo | Group |
|---|---|---|
| [m10/09 Capture the flag](labs/09-capture-the-flag/README.md) (graded) | 12 flags in the fork, 8 to pass; steal a private review, enumerate users, crash a node, bypass a role | 2 hours, 3 points to each flag's first finder, 1 after; each first finder explains the fix |
| [m10/10 Red team / blue team](labs/10-red-blue/README.md) | patch the fork until all 13 exploits fail and Shelfie still works | pairs swap patched servers, attack 30 minutes, patch, re-attack |
| [m10/11 Cost meter](labs/11-project-cost-meter/README.md) (mini-project) | a proxy and live dashboard of each client's cost per minute, fed from `extensions` | one operator projects the meter; everyone else spends a key's budget |

## Changes from the plan

- **Keycloak is a course service**, not Testcontainers (no Docker socket in the course container). Its realm
  imports the course signing key, so tests can mint expired, forged and wrong-audience tokens offline.
- **10.3's CPU and memory** come from `/proc` of the fork's process (`attack.sh`), and the fork's heap is
  capped at 512 MB so "crash a node" is a small crash. The plan's "field suggestions" attack is recorded as
  a non-finding: graphql-java 26.1 never suggests.
- **10.4's cost budget** is per reader with Bucket4j; 10.6 moves it to per API client in Shopify's
  `extensions.cost` shape. GitHub's and Shopify's models are described from their documentation.
- **10.5** uses Apollo's persisted query manifest format and `extensions.persistedQuery`, since Spring for
  GraphQL 2.1 has no `documentId` request field.
- **10.7's origin check** needed no code: Spring refuses cross-origin WebSocket handshakes unless CORS
  allows the origin, and Boot hands `spring.graphql.cors` to the GraphQL WebSocket handler. The lab's bug is
  a permissive CORS line.
- **10.8** runs graphql-cop 1.15 and Clairvoyance 2.5.5 in their own image (`bin/course seckit`); InQL, DVGA and
  OWASP ZAP are described, not run (Burp's GUI, large images). The test re-implements graphql-cop's four HIGH
  checks in Hurl so it needs no scanner.
- The capture the flag's flags are computed in the fork's source: the game is black box on your honour.

Findings behind these: `_workbench/2026-11-21-m10-probe/FINDINGS.md` (22 items).

## Spec references

- [GraphQL over HTTP, September 2026 edition](https://http-spec.graphql.org/September2026/): "GET requests
  MUST NOT be used for executing mutation operations" (lab 10.3's CSRF experiment).
- [graphql.org security guide](https://graphql.org/learn/security/), [Golden Path: trusted
  documents](https://goldenpath.graphql.org/practices/trusted-documents/).
- [graphql-transport-ws](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md): `connection_init`,
  close code 4401.

## Checkpoint

`bin/course checkpoint 10` (see [checkpoint.md](checkpoint.md)): the hardened Shelfie (labs 10.1 to 10.8), the
capture the flag (8 of 12 or more) and the blue team's patch. Tag: `checkpoint-10`.
