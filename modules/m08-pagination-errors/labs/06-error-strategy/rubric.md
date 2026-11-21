# Rubric, lab 8.6 error-contract.md (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | The codes table is complete and actionable: for each code a meaning, a real example from this server, whether a retry can help, and what the client should do. NOT_FOUND explains why private clubs answer NOT_FOUND rather than FORBIDDEN. |
| 2 | Messages and partial data: clients switch on `code`, never parse messages; INTERNAL messages are generic and the details are only in the log; partial `data` is real data, located by `errors[].path`; no `data` key means nothing ran. |
| 3 | The status table has measured values for both Accept headers, compared with the September 2026 edition, with an honest conclusion about which rules this server meets (MUSTs vs SHOULDs). Invented numbers score 0. |
| 2 | Request ids and change rules: how `requestId`/`errorId` connect a client report to a log line, why the header is validated, and which changes to codes are breaking. |
