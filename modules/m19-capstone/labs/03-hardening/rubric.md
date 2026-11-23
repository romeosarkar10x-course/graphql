# Rubric, stage 3 (15 points)

| Criterion | Points | Full marks |
| --- | --- | --- |
| Authorization | 4 | Every private field has a rule, tested; nothing leaks existence (someone else's object is "not found") |
| Demand control and trusted documents | 3 | Limits sized from the real operations (numbers stated); only known documents run; the client sends ids |
| Performance | 3 | No N+1, proven with a counter; the load test's numbers measured and explained |
| Real time | 2 | Events after the commit only; what happens with two instances is stated |
| Client and UI | 3 | A typed Java client that handles typed failures; a UI that uses the same operations |

Pass: 10/15. The attack report counts in the final rubric's security area.
