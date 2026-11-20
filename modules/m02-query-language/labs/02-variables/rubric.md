# Rubric, lab 2.2 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| The sent document | 2 | The actual string, showing the payload closed the argument, replaced the limit, added `__schema` and commented out the rest with `#` |
| What the server did | 2 | All books plus the type list came back; explains the server receives one valid document and cannot tell developer text from user text |
| Other attacks | 2 | Two plausible ones: expensive or deep selections, unrequested fields, aliases to multiply work, breaking the query for a denial of service |
| Why variables fix it | 3 | Document constant, values sent separately and type-coerced; a quote is data, not syntax. Bonus insight: constant documents enable persisted queries or allowlists |
| Defaults | 1 | Variable default applies when the key is absent; distinguishes absent from explicit null or mentions the schema argument default |

Pass: 7/10.
