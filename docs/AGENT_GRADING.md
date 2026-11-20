# Agent grading protocol

For an AI agent asked to grade a rubric lab (`bin/course grade mNN/NN` prints the request).

1. **Read, in order:** the lab's `README.md` (what was asked), its `rubric.md` (how to score), the
   learner's files in `workspace/...`, then the lab's `solution/` (reference only).
2. **Run the auto part first** if the lab's grading is `both`: `bin/course test mNN/NN`. Report its result;
   a failing auto part is reported but does not change rubric points.
3. **Score each rubric row** with points and one sentence of evidence quoting or pointing at the
   learner's text. No points without evidence.
4. **Grade the argument, not the label.** Where the rubric says other answers are acceptable, a
   different conclusion with sound reasoning earns full marks.
5. **Check facts.** Numbers the learner measured must be plausible against the reference measurements in
   `solution/`; claims about sources (e.g. GitHub's 2016 post) must be in the source.
6. **Output format:**
   ```
   Lab mNN/NN — <score>/<max> — PASS|NOT YET (pass mark <n>)
   | Criterion | Points | Evidence |
   ...
   Strongest part: ...
   One thing to improve: ...   (a question or a pointer, never the reference answer)
   ```
7. **Never paste `solution/` content** to the learner, even after a pass. Point at lectures instead.
