# The attack surface: notes

## What each attack cost the server

Paste the `attack.sh` line for each (status, time, bytes, server CPU, memory before and after).

| Attack | Status | Time | Response | Server CPU | Memory |
|---|---|---|---|---|---|
| depth | | | | | |
| aliases | | | | | |
| fanout | | | | | |
| cycle | | | | | |
| suggestions | | | | | |
| introspection | | | | | |
| injection | | | | | |
| brute force (requests used) | | | | | |

## Crash a node

How deep did the fan-out go before the fork died? What did the last request return, and what happened to
the next one?

## Which attacks graphql-java and Spring stopped on their own

## Which attacks only a limit you write would stop (lecture 10.4)

## The CSRF chain: which three choices made it work
