# Lab 6.3 notes

## The starter's report

Paste the "GraphQL schema inspection" block from the starter's startup log.

## Questions

1. The starter's report has four argument nullness errors. Explain one in terms of the schema and the
   Java signature, and show the request that crashes the starter because of it.
2. Which of your three injected mismatches would have been a runtime bug without the inspection, and
   what would the user have seen?
3. The nullness checks need JSpecify annotations. What does `@NullMarked` change, and why did
   `@Nullable Data.Club` not compile?
4. Should a mismatch fail startup in production, or only in CI? Argue one side.
