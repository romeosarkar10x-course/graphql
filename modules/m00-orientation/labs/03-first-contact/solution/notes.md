# Lab 0.3 notes (reference)

## 1. One field vs twenty
| Fields asked | Response bytes |
|---|---|
| 1 (`title`) | 40 |
| 20 (scalars, including `openingCrawl`, plus 5 connection `totalCount`s) | about 1,000 |

Measured 2026-11-21: 40 and 1,011 bytes.

## 2. What decided the size?
The client. The server returns exactly the fields selected, no more. A REST `GET /films/1` returns
whatever representation the server designed, the same for every caller; a client that only wants the
title still downloads the opening crawl.

## 3. Docs pane
Example: the Countries API's `country(code: ID!)` argument is required and takes an ISO code; the
docs pane showed the argument, its type and its non-null marker without any other documentation.
