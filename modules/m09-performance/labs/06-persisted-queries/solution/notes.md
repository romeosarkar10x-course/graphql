# Persisted queries: notes

## Bytes and hit rate (Wire.java, 50 dashboards each, through Varnish)

| Mode | Request bytes | Hit rate | Median ms |
|---|---|---|---|
| POST | 281 | 0% | 17.3 |
| GET (full text) | 357 | 98% | 0.4 |
| APQ GET (hash) | 187 | 98% | 0.3 |

(Reference run, 2026-11-21. The first request of each GET mode is the miss.)

## What the numbers say

POST is never cached by Varnish, so every dashboard runs on the app (8 statements each). Any GET with
a public Cache-Control is a cache hit after the first, so the latency win comes from GET plus
Cache-Control, not from APQ itself. What APQ adds is the size: the URL carries a 64-character hash
instead of the whole document, which matters for big documents (URL length limits are around 8 KB in
many proxies) and on mobile uplinks. It also skips parsing and validation on the server when the
document is in the Caffeine cache.

## Why APQ is not security

Anyone can register any document: send the text with its hash and the server stores it. So APQ does not
limit which operations run. An allow-list does: trusted documents (Module 10) are registered at build
time and the server refuses everything else. APQ is a bandwidth and caching optimisation.

## The traps

Spring sends no Cache-Control on GET, and Varnish's built-in VCL then keeps every GET for 120 s,
including a PersistedQueryNotFound: registration would not take effect for two minutes. And graphql-java
checks the hash only on a cache miss: a known hash sent with other text ran the cached document.
