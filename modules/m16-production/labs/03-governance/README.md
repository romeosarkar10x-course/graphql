# Lab 16.3: Governance: owners per type, a review template, and a removal with proof

Lecture: [16.3 Governance](../../lectures/16.3-governance.md). Grading: auto + rubric.

Shelfie has grown past one team. Four teams own parts of the schema: platform (`Query`, `Mutation`), live
(`Subscription`), catalog (`Book`), community (`Club`, `User`) and reviews (`Review`). Reviews today happen in
chat, so make the rules part of the repository.

1. **One file per type.** Split `src/main/resources/graphql/shelfie.graphqls` into
   `src/main/resources/graphql/types/<Type>.graphqls`. Spring for GraphQL loads `graphql/**`, so nothing else
   changes.
2. **CODEOWNERS.** In `.github/CODEOWNERS`, give every type file an owner team (`@shelfie/catalog`, ...). Then a
   pull request touching `Book.graphqls` asks catalog for a review.
3. **A schema-review template.** `.github/pull_request_template.md` covers:
   - breaking changes;
   - deprecations;
   - usage evidence;
   - owners;
   - the changelog.

   Write it as checks a reviewer can tick, at least five `- [ ]` lines.
4. **Deprecate, measure, remove.** Two fields are deprecated. `Book.isbn10` was deprecated on 2026-06-01, and
   iOS 2.7.0 is below the minimum supported iOS version. `Club.memberCount` was deprecated on 2026-09-01.
   - Use lab 16.2's usage data. Replay the week with `contract/Traffic.java` into `contract/UsageCollector.java`
     and look at the reports.
   - Decide which field can go. Remove that one and keep the other.
   - Write the evidence in `REMOVAL.md`: windows, counts, clients and versions, what you checked, and how you would
     roll back.
   - Add a dated entry to `CHANGELOG.md`.

The test:

- diffs your schema against the contract's with graphql-java's SchemaDiff (`bin/course design diff`). Exactly one
  breaking change is allowed.
- replays the week against your app. Only the 60 operations from iOS 2.7.0 may fail.

```bash
bin/course start m16/03
cd workspace/m16-production/03-governance
bin/course test m16/03
```

## Group variant

Each person owns one or two types. A teammate opens a pull request for the removal using your template. Every
owner whose file it touches reviews it, and they record their decision in the pull request. Paste the thread
into `REMOVAL.md`.

## Done when

`bin/course test m16/03` passes and `REMOVAL.md` passes the rubric (`bin/course grade m16/03`).
