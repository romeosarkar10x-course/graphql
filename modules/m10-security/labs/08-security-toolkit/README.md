# Lab 10.8: Security testing toolkit

Lecture: [10.8 Security testing toolkit](../../lectures/10.8-security-toolkit.md). Grading: auto + rubric. Needs nothing.

The starter is lab 10.7's solution with lab 10.4's limits added. Scan it, fix every finding, and leave
behind tests that would catch each problem if it came back.

1. **Scan** with graphql-cop: run your server in one terminal, the scanner in another. `bin/course seckit`
   runs in the network of your newest course container, so the target is `localhost`:
   ```bash
   bin/course start m10/08 && cd workspace/m10-security/08-security-toolkit
   bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-secure.jar'     # terminal 1
   bin/course seckit graphql-cop -t http://localhost:4000/graphql -o json > scans/before.json    # terminal 2
   ```
2. **Fix** every HIGH and MEDIUM finding. Real screens from lab 10.4 must keep working.
3. **Rescan** into `scans/after.json`.
4. **Schema recovery**: with introspection off, run Clairvoyance (`bin/course seckit clairvoyance
   http://localhost:4000/graphql -o recovered.json`, a small wordlist with `-w`) and record what it got.
5. **Regression tests**: `src/test/java/.../SecurityRegressionTest.java`, black box over HTTP, anonymous, at
   least 10 tests: one per scanner finding and per lab 10.3 attack. The test runs them against your server
   (all must pass) and against the vulnerable fork on :4100 (8 or more must fail: a test that passes on
   both proves nothing).

## Done when

`bin/course test m10/08` passes: graphql-cop's four HIGH checks re-implemented in `tests/cop.hurl`, your
suite (10+ tests) green on your server and catching the fork, both scans committed, and `notes.md` passes
the rubric (`bin/course grade m10/08`).
