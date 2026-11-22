# Hints: m13/08

1. Read the survivors, not the score: `target/pit-reports/index.html` shows each mutant on its line. "No coverage"
   means no test ran that line at all; "survived" means a test ran it and asserted nothing that changed.
2. Conditional-boundary mutants (`>` to `>=`) die only to tests at the exact boundary: rating 5 and 6, body
   length 500 and 501. Return-value mutants (`return null`, `Collections.emptyList()`) die to tests that look at
   what came back, not just that nothing threw.
3. A lambda in a filter (`bookId == null || r.bookId().equals(bookId)`) has both halves mutated: test the feed
   with a book id and without one, and assert on which events arrive, not only how many.
