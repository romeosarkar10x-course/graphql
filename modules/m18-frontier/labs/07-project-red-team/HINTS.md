# Hints: 18.7 Agent safety lab

1. Diff the starter against your lab 18.3 (or against lab 18.3's README rules, line by line): the seven
   weaknesses are each a small change in one of `QueryGuard`, `Approvals`, `HumanController`, `WriteTools` or
   `application.yaml`. Look for checks that are missing, not code that is wrong.
2. Two are in the guard (what counts as a list size, what counts as introspection), three in the approval state
   machine (who, when, how often), one in the human's routes, one in the defaults.
3. For each weakness, write the attack before the fix and watch it print `BYPASS`; then fix and watch it print
   `blocked`. An attack that cannot fail is not a test: check both the tool's answer and Shelfie's data.
