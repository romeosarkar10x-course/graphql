# Rubric, lab 0.4 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Codes explained | 4 | 405, 415 and 400 each explained in one line; recognises 405 for GET is spec-compliant (POST MUST, GET MAY) |
| Media types | 4 | Records 200 vs 400 for the invalid query, ties the difference to the `Accept` header and explains why a meaningful status code helps intermediaries while legacy clients keep working |
| Spec use | 2 | Cites or paraphrases the GraphQL-over-HTTP draft rather than guessing |

Pass: 7/10. Bonus mention (not required): `text/plain` matters for CSRF.
