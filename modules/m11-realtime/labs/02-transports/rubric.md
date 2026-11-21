# Rubric, lab 11.2 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | The same subscription's frames on both transports from a real run: graphql-transport-ws's `connection_init`/`connection_ack`/`subscribe`/`next`/`complete`/`ping` and SSE's `event:`/`data:` lines, and what each costs (one socket for many operations vs one HTTP response per subscription). Invented frames score 0. |
| 3 | The drop: what each transport reported, the client's reconnect log (backoff, refused attempts, the gap it filled), and why "subscribe, then query the gap" cannot lose a message while "query, then subscribe" can. |
| 2 | Keep-alives: why they exist (idle proxies, half-open connections) and how each transport sends one. |
| 2 | Where RSocket (Spring's `GraphQlRSocketHandler`) and multipart HTTP subscriptions fit, and which clients speak each. |
