# Rubric, lab 8.4 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | Q1 compares the named filter input with a generic `where` input on both sides: what clients can express, and what the server must then support (indexes for every combination, injection surface, unbounded cost, schema evolution, documentation). It uses a measured or observed example, such as a filter plus a sort that no single index serves. |
| 2 | Q2 explains when `count` runs (only when `totalCount` is selected, as a second statement over every match), roughly what it costs, and gives a concrete plan for 10 million rows (estimates, caps such as "1,000+", caching, or dropping it), with what the client loses. |
| 3 | Q3 reports the break-it experiment honestly: which mutations the properties caught, how fast, and what jqwik shrank the failure to, and names one bug class that example tests miss (tie order, boundary inclusivity, null vs empty). |
| 2 | Q4 takes a position on one of the two decisions, with a client-side consequence. |
