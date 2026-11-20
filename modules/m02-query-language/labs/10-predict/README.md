# Game — Predict the response

**Module 2 game** · **Grading:** auto (score out of 10) · **Needs:** `bin/course ref up`

Ten small documents. For each one, write the exact JSON response **before** you run it. They test the
rules from lectures 2.1 to 2.5: what null means, argument defaults, aliases, merging, directives,
unions and validation.

```bash
bin/course start m02/10      # cards.md has the 10 documents; write predictions/01.json ... 10.json
bin/course test m02/10       # score
```

Allowed: the schema (GraphiQL docs pane) and the seed data, through REST
(`curl localhost:8080/api/clubs/c2`) or `reference/shelfie-ref/src/main/java/dev/shelfie/ref/data/Store.java`.
Not allowed: running a card's document before you have written its prediction.

Scoring compares your `data` with the real `data` exactly (key order ignored), and whether you predicted
`errors`. Error messages are not scored.

**Solo:** 10/10, then explain each card you missed in one sentence.
**Group:** everyone writes predictions on paper or in their own workspace, then reveal card by card in
GraphiQL. One point per exact prediction; argue about the misses.
