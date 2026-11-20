# Rubric, game 3a (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Coverage | 3 | Every visible value on the four screens is reachable in one request per screen; nothing fetched that is not shown |
| Types | 2 | Ownership is sensible (counts and viewer-relative state as fields, not client-side work); unions/interfaces where types share or do not share fields |
| Nullability | 2 | Each nullable field has a reason tied to what the screen shows when it is missing; no non-null field one failure can break a screen with |
| Growth | 1 | Lists that grow take an argument |
| Decisions | 2 | Three real uncertainties with trade-offs, and an honest comparison with the model answer |

Pass: 7/10. Different designs from the model answer earn full marks when argued.
