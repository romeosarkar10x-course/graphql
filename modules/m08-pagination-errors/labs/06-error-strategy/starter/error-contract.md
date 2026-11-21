# Shelfie error contract

Written for client developers. Every statement here should be true of your server; the test checks
the codes, the grader reads the rest.

## Codes

| `extensions.code` | Meaning | Example | Retry? | What the client does |
|---|---|---|---|---|
| UNAUTHENTICATED | | | | |
| FORBIDDEN | | | | |
| NOT_FOUND | | | | |
| CONFLICT | | | | |
| INVALID_REQUEST | | | | |
| INTERNAL | | | | |

## Messages

## Partial data

## HTTP status codes (measured)

| Response | `Accept: application/json` | `Accept: application/graphql-response+json` | GraphQL over HTTP (September 2026) says |
|---|---|---|---|
| data, no errors | | | |
| data and a field error (`coverUrl`) | | | |
| `data: null` and an error (`myClubs` signed out) | | | |
| validation error (no data) | | | |
| syntax error (no data) | | | |

## Request ids and support

## Changing this contract
