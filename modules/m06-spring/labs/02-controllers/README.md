# Lab 6.2 — Beyond @QueryMapping

**Lecture:** [6.2 Annotated controllers](../../lectures/6.2-controllers.md) · **Grading:** auto + rubric · **Needs:** nothing

Lab 6.1's app, plus `lab.graphqls`. Three fields, three annotations (in `LabController`):

| Field | Annotation | Behaviour (`tests/lab.hurl`) |
|---|---|---|
| `User.roleInClub` | `@LocalContextValue` | `OWNER` / `MEMBER` under `Club.owner` and `Club.members`, null elsewhere (Module 5.2's local context, the Spring way) |
| `Query.bookSearch` | `@Arguments` | both arguments in one record; the same answer as `books` |
| `Mutation.updateClub` | `ArgumentValue<T>` | a patch: a field left out keeps its value, `description: null` clears it |

`Clubs.java` (provided) keeps edited clubs; `club` and `clubs` already read from it. The 14 Shelfie checks
must still pass.

The plan also lists **projected interfaces**: they bind Spring Data projections and arrive with
`@GraphQlRepository` in lab 6.5.

## Done when

`bin/course test m06/02` is green and `bin/course grade m06/02` gives 7/10 or more.
