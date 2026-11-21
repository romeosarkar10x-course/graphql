# Rubric, mini-project 5 DESIGN.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Structure and omissions | 3 | Four or more real omissions, each tied to the module that adds it |
| Status codes | 2 | Quotes the code; names a genuinely tricky rule (legacy clients and 294, 400 vs 422) |
| Errors | 2 | Client sees a masked message with path; log has the details |
| Threads | 2 | Names the executor and the real limit under load (downstream, sync fetchers) |
| Reflection | 1 | A considered answer on separating HTTP from wiring |

Pass: 7/10. Auto part: the 40 Hurl tests, the line budget and the dependency rule.
