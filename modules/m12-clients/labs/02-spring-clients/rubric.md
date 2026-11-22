# Rubric, lab 12.2 notes (pass: 6 of 9)

| Points | Criterion |
|---|---|
| 3 | Sync vs reactive: what the request thread does in each, a sensible pick for this service, and virtual threads mentioned. |
| 3 | `toEntity` behaviour from a real run: null field, invalid field (the real exception class, `FieldAccessException`, and message), and userErrors being data. Invented output scores 0. |
| 3 | The restart: what was logged, that messages during the outage are lost, and a gap-filling plan (query after the last id, holding live events). |
