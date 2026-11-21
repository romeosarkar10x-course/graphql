# Rubric, lab 7.7 (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Worst five | 3 | Ranked by cost to clients or risk; includes identity-as-argument and the untyped escape hatches; each with coordinates and its replacement |
| Kitchen sink | 2 | `updateUser` split by intent with a stated rule; staff and security fields moved out, not just renamed |
| Design beyond the lint | 2 | `good.graphqls` uses payloads with the changed object, enums for closed sets, references, a viewer entry point |
| Bounded contexts | 3 | Every type mapped; border fields identified with an owner (e.g. ratings on `Book`); ready to be split into subgraphs |

Pass: 7/10.
