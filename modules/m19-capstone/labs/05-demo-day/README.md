# m19/05 Stage 5: demo day

About 1 hour of preparation and 10 minutes of demo, then questions.

## Do

1. **Write `docs/DEMO.md`:** a minute-by-minute script, the questions you expect, and what you do if something
   does not start. Rehearse once against a freshly started stack.
2. **Show it working**, not slides: a client using the API, a typed failure, the real-time feature, one
   security refusal, one trace, and the outage of one subgraph.
3. **Say what you would do next**, in order, and what each costs. Honest trade-offs score more than polish.
4. **Group:** demo live; the others ask the questions (each prepares two from your design doc). **Solo:**
   record the screen and your voice, transcribe it (any tool), commit `docs/demo-transcript.md`, then
   `bin/course grade m19/05`: the grader asks you five questions from your documents, and your written answers
   go into `docs/QA.md`.

## Grading

`bin/course grade m19/05` scores the whole capstone against [rubric.md](rubric.md): the syllabus's seven areas.
The grader reads every stage's documents, your code and the checker's output for stages 1 to 4.

## Done when

`bin/course checkpoint 19` passes and tags your workspace.
