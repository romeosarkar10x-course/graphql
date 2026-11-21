# Rubric, lab 7.4 audit.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Checklist applied | 3 | Every row answers both questions; reasons cite the data facts, not "to be safe" |
| `owner` and lists | 2 | Explains why `Club.owner` keeps `!` while `User.name` does not, and decides list vs item nullability for `members` |
| Outage table | 2 | All eight counts recorded; identifies the worst outage with a reason (fan-out of the failing field) |
| Error behaviour | 3 | Records the `--no-propagation` counts, explains that disabling propagation gives the same survival without schema changes, and what `!` and `@semanticNonNull` would then mean for clients and codegen |

Pass: 7/10.
