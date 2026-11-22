# Module 17 probe findings (2026-11-23)

Spec baseline: September 2025 edition (`spec/September2025.html`, fetched 2026-11-23; text dump `spec/September2025.txt`).
graphql-java: 26.1 (course-lib), source tag `v26.1` cloned into `gj/` (Gradle wrapper 9.4.0, toolchain Java 25,
release 11).

## Versions and tools

1. **Jazzer 0.30.0** (2026-02-24, Apache-2.0; `jazzer-junit`). Runs in the course image on JUnit 6.1.3 only with the
   JUnit BOM imported: without it, jazzer-junit's JUnit 5.9 / platform 1.9 dependencies win and discovery fails with
   `NoClassDefFoundError: org/junit/platform/engine/support/discovery/DiscoveryIssueReporter` (`jazzer/run1.txt`).
   `jazzer.instrument=dev.shelfie.**,graphql.**` in `junit-platform.properties` is needed: without it coverage
   stays at 2 edges (`jazzer/run3.txt` vs `run4.txt`: 2 vs 2,651). A `String` parameter is nullable; use
   `FuzzedDataProvider`. Memory: about 1.4 to 1.9 GB RSS; one core.
2. **graphql-http 1.23.1** (2026-09-28, MIT), graphql 16.15.0: installs and runs on the course's Node 24 (`http/`).
3. **RFC stages** (rfcs.graphql.org, `rfcs/*.html`): #849 full Unicode RFC 3 Accepted (merged 2022-06-03); #1058
   list coercion RFC 2 Draft; #1179 CoerceInputValue RFC 1 Proposal; #1177 meta-fields in ExecuteCollectedFields
   RFC 1 Proposal; #1227 empty selection sets RFC 3 Accepted, spec PR merged 2026-09-17 (after the September 2025
   edition). Stage names from graphql-spec CONTRIBUTING.md: 0 Strawman, 1 Proposal, 2 Draft, 3 Accepted, X Rejected.
4. **Spec structure**: top-level sections 1 Overview, 2 Language, 3 Type System, 4 Introspection, 5 Validation,
   6 Execution, 7 Response, appendices A-E (181 numbered sections with ids).
5. **September 2025 grammar** (from the HTML productions): `OperationDefinition`, `FragmentDefinition` and
   `VariableDefinition` take an optional `Description`. `SourceCharacter` is any Unicode scalar value.
   `FieldsDefinition`, `EnumValuesDefinition`, `InputFieldsDefinition` and `ArgumentsDefinition` need at least one
   item. Built-in directives include `@oneOf`; `@deprecated(reason: String! = "No longer supported")`;
   `includeDeprecated: Boolean! = false` on `__Type.fields/enumValues/inputFields`, `__Field.args`,
   `__Directive.args`.

## graphql-java 26.1 against the September 2025 spec

Each probed directly (`gjparse/`), and found again by the differential tests and the fuzzer.

6. **Descriptions on operations, fragments and variables are rejected** (`InvalidSyntaxException`), and the AST
   has no field for them (`OperationDefinition.getDescription()` does not exist) (`gjparse/edge-cases.txt`).
7. **Block strings keep CR**: `removeIndentation` splits on `\n` only, so `"""one\r\ntwo"""` is `one\r\ntwo` and
   a CRLF block string keeps a leading `\r` line (`gjparse/crlf.txt`). The spec splits on CRLF, CR and LF and joins
   with LF.
8. **Block strings: short whitespace-only lines keep their spaces**: the common indent is removed only from lines
   longer than it (`line.length() > commonIndent`); the spec removes it from every line but the first
   (`StringValueParsing.java:28-63`).
9. **A lone CR does not start a new line** in locations: `{\ra }` puts `a` at 1:3; LF and CRLF give 2:1
   (`gjparse/lone-cr.txt`; ANTLR counts LF only).
10. **U+2028 and U+2029 are ignored as line terminators** (`GraphqlCommon.g4:146`, `LineTerminator: [  ]
    -> channel(3)`): `{ a }` parses. The spec's LineTerminator is LF, CRLF or CR only, and U+2028 is not
    ignored, so it is a lexical error.
11. **Empty `{}` in SDL is accepted** for fields, enum values and input fields (`GraphqlSDL.g4`:
    `fieldDefinition*`, `enumValueDefinition*`, `inputValueDefinition*`); the spec needs at least one.
12. **`type T { u @s }` is read as a fieldless type plus a query**: when the braces do not parse as fields, ANTLR
    falls back to `type T` followed by the shorthand query `{ u @s }`. The spec's `[lookahead != {]` on the
    fieldless form forbids that, so it is a syntax error.
13. **Numbers lose their spelling** in the AST (BigInteger / BigDecimal): `1e10` → `1E+10`, `-0.0` → `0.0`,
    `-0` → `0`. Not a spec issue; the differential tests compare numbers by value.
14. **Introspection, built-ins**: on the reference schema every user type matches our engine's introspection
    exactly (`introspect-diff-ref.txt`); differences are graphql-java's built-in descriptions ("Built-in Int"),
    `@deprecated` locations in another order, two extra directives (`@defer`,
    `@experimental_disableErrorPropagation`), and `includeDeprecated: Boolean = false` (nullable; spec `Boolean!`).
15. **Validation error counts are implementation-defined**: graphql-java 26's `OperationValidator` reports one
    NoFragmentCycles error per fragment at either end of a back edge (A→B→C→A marks A and C, not B), and
    OverlappingFieldsCanBeMerged once per conflicting group and response path; graphql-js counts differently. The
    spec states only that the document is invalid. graphql-java 26 can run a single rule:
    `Validator.validateDocument(schema, doc, rule -> rule == OperationValidationRule.X, locale)`.
16. **graphql-http 1.23.1 audits an older draft than the September 2026 HTTP edition** (published 2026-11-12, copy in
    `_workbench/2026-11-21-m05-probe/`). Against Module 5's lab 5.4 solution: 37 ok, 6 warn, 18 notices
    (`http/m05-04-solution.txt`). Four warnings contradict the edition: 572B, FDE2, 7B9B (SHOULD 200 for parse,
    validation and coercion failures when accepting `application/json`; the edition gives the same 4xx as for
    `application/graphql-response+json`) and 86EE (SHOULD 400 for variable coercion failures; the edition says
    422). The 18 MAY notices want 400 for malformed parameters where the edition says 422. 47DE and 80D8 (use
    `application/json` for `*/*` and for a missing Accept) are allowed by the edition. Module 5's mini-project 5.9
    server: 3 MUST errors (null `variables`, `operationName`, `extensions` with `application/json`) and 9 warnings
    (`http/m05-09-solution.txt`).

## Our engine (reference, `modules/m17-under-the-hood/contract/engine`)

17. 1,490 counted lines at probe time, 1,525 as committed (Module 5's counter: no blanks, comments, package or import lines).
18. Module 2's twelve operations over the reference server's own Store: identical responses from our engine and
    graphql-java (`Smoke`).
19. Token differential, corpus of 1,000 (300 course files + 700 generated, seed 17): 954 agree, 0 unexplained,
    46 known (lone CR). Block-string values in the expected tokens come from the spec's BlockStringValue() applied
    to graphql-java's raw token, because of findings 7 and 8.
20. Parser differential, same corpus: 767 agree, 0 unexplained, 233 known (findings 7-10), after comparing numbers
    by value (finding 13).
21. Differential fuzzing (Jazzer, 60 s per run, one core), six runs (`fuzzdiff/run1-6.txt`): run 1 found 2,625
    disagreeing inputs. Classified: lone CR (9), block strings (7, 8), descriptions on operations (6), empty braces
    (11), the lookahead (12), and **a bug in our parser**: `extend scalar S` with no directives was accepted (the
    spec's extension productions need at least one part; fixed). Run 3 stopped on a stack overflow that was the
    harness's own regex (Java regex recursion on a long block string), replaced by graphql-java's raw tokens. Run 6:
    488,901 inputs, 0 unexplained disagreements.

## Housekeeping

The `gj/` clone (61 MB) and the fuzz runs' disagreement files (3,601 in all) were removed after writing. Counts
are in `fuzzdiff/COUNTS.md`, five samples per run in `fuzzdiff/samples/`. Reproduce the clone with
`git clone --depth 1 --branch v26.1 https://github.com/graphql-java/graphql-java.git gj`.

22. **Not run: graphql-java's Gradle build** (`./gradlew publishToMavenLocal`, lab 17.7). Running its wrapper was
    not authorised in the authoring session. Lab 17.7's build commands (`RELEASE_VERSION`, `-PpublishToMavenLocal`)
    are read from `build.gradle` at v26.1 (`getDevelopmentVersion`, line 177; the `signing` block) and unverified.
