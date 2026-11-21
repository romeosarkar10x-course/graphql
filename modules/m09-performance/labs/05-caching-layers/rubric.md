# Rubric, lab 9.5 diagnosis (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 5 | One point per scenario: the explanation names the evidence that pointed to the layer (a header, the statement count, inside one request or across requests) and says what was wrong in that layer's terms (missing invalidation, or a key missing a dimension). |
| 3 | The fixes keep every cache working and say what each one costs (a coarse invalidation drops more than it must; a key with the viewer stores one entry per reader; `private` gives up the CDN). |
| 2 | Notices where two layers caused one symptom (scenario 4 also needs the entity cache fixed; scenario 5 also needs the response key fixed) and explains how they found it. |
