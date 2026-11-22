# Authoring rules

How a module is laid out. Module 0 and Module 1 are the exemplars: copy their shape.

```
modules/mNN-slug/
  README.md               overview: lecture/lab table, games table, spec refs, checkpoint
  checkpoint.md           8 quiz questions, answers folded in <details>
  checkpoint.json         {"labs": [...], "files": [...]} for bin/course checkpoint NN
  checkpoint-host.sh      optional: run on the host by bin/course checkpoint NN before the in-image checks (M15: docker compose up)
  contract/               shared .graphql files, expected outputs, view-model specs (optional)
  lectures/N.M-slug.md    Hook / Concept / Java build / Experiment / Check (with folded answers)
  labs/NN-slug/
    README.md             the brief: what, how to run, done-when; group variant for games
    lab.json              {"title","lecture","kind": lab|project|game,"grading": auto|rubric|both,"needs": [ref|toxiproxy|postgres|redis|kafka|keycloak|varnish|internet|docker|node|fed]}
    HINTS.md              exactly three escalating hints; the third is still not the answer
    rubric.md             for rubric/both labs: points table and pass mark
    starter/              copied to workspace/ by bin/course start; must FAIL the tests
    solution/             reference; must PASS the tests; reference notes for rubric parts
    tests/run.sh          sources /course/tools/testlib.sh; uses pass/fail/finish; env in testlib.sh
```

Rules:

- `bin/course check --starters` must be clean before a module is called done.
- Java builds are Maven (`pom.xml`, `./mvnw`). Small client labs may be single-file programs run with
  `bin/course java`, which puts Jackson 3 on the class path.
- Expected outputs come from running the reference solution, never from typing them by hand.
- Tests must not print the answer: hash answer keys (see `m00/06`, `m01/01`).
- Written work in `both` labs is checked with `notes_check <file> 60 mNN/NN` (testlib): the learner's
  file must add 60+ words beyond the starter's copy, so headings and empty tables do not pass.
- Labs that write to the reference server rely on the runner's `/_course/reset-data` before each test;
  reset again at the end of `run.sh` if the lab leaves state behind.
- Facts in lectures are verified (probe output in `_workbench/`) or explicitly marked unverified.
