# Lab 0.1 — First red test

**Lecture:** [0.1 Welcome and course map](../../lectures/0.1-welcome.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

Every lab in this course is graded the same way: a black-box test talks to your work over HTTP and
checks the JSON. This lab makes you read one failing test, fix it, and prove the fix is not a trick.

## Do

```bash
bin/course ref up              # start the reference Shelfie server
bin/course start m00/01        # copies the starter to workspace/m00-orientation/01-first-red-test/
cd workspace/m00-orientation/01-first-red-test
../../../bin/course hurl hello.hurl --test     # red
```

1. Read the failure. Fix `hello.hurl` so it passes against the reference server.
2. Make the test stronger: it must also assert that the response has **no `errors` key**.
   (Why? Open GraphiQL at http://localhost:8080/graphiql and run `{ nope }`. Look at the status code
   in your browser's network tab.)
3. Run it against nothing: `../../../bin/course hurl hello.hurl --test --variable ref=http://localhost:1`.
4. Answer the three questions in `notes.md`.

## Done when

`bin/course test m00/01` is green, and `bin/course grade m00/01` gets 7/10 or more from your AI grader.

## Language-neutral

`hello.hurl` is the whole contract. It would grade a Go or TypeScript `hello` server unchanged.
