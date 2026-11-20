# Game — Fix the broken query

**Module 2 game** · **Grading:** auto (score out of 12) · **Needs:** `bin/course ref up`

Twelve documents, twelve different real errors from graphql-java's validator. Each file starts with an
`# Intent:` line saying what it is supposed to return. Fix it so it does exactly that, with the
smallest change you can.

```bash
bin/course start m02/11      # broken/01.graphql ... broken/12.graphql, plus variables for 08 and 12
bin/course test m02/11       # score; a fix must be valid AND return what the Intent asks for
```

Read the error first: send the document from GraphiQL (put the `.json` file's content in the Variables
pane) and find the rule name in brackets, such as `FieldUndefined` or `VariableTypeMismatch`. The
variables are part of the puzzle: do not change them.

**Solo:** 12/12 in under 10 minutes. Then, for each file, write the name of the rule it broke.
**Group:** a timer on the wall; first to 12/12 on `bin/course test` wins. Second round: each player
writes one new broken query for the next player.
