# Lab 1.3 notes (reference, measured 2026-11-21)

## 1. REST vs GraphQL, same page (club c1)
| | Requests | Bytes | ms, no latency | ms, 700 ms latency |
|---|---|---|---|---|
| REST (lab 1.2) | 11 | 4,917 | 75 | 2,182 |
| GraphQL | 1 | 824 | 125 | 834 |

Without latency REST was *faster* here (warm JVM, localhost, tiny data): GraphQL parsing and
validation cost something. The win appears only when round trips are expensive, as on mobile.

## 2. Where did the work go?
The joins moved from the client (across the network, 3 sequential waves) to the server (in-process
calls, microseconds apart). The server team now owns resolving arbitrary shapes efficiently: N+1
queries (Module 9), cost limits for expensive shapes (Module 10).

## 3. What did you lose?
HTTP caching: `GET /api/books/b1` is cacheable by URL in browsers and CDNs; a POST to `/graphql` is not.
Also: per-endpoint status codes and metrics, and simple `curl` debugging.
