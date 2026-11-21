# Guess the query count: the arithmetic

1. **1**: one `findById` for the club; `name` is a column of the club row.
2. **3**: the club, then one statement for the owner and one for the current book.
3. **6**: 1 for the five clubs, then one owner per club: 1 + 5.
4. **10**: 1 + 9, not 1 + 10. Club 10 has no current book, and the resolver returns null without a query.
5. **41**: 1 + 4 (members per club) + 4 x 3 (reviews per member) + 4 x 3 x 2 (book per review).
6. **2**: aliases are separate fields, and the naive app has no per-request memo: the same club is loaded twice.
7. **7**: 1 + 3 counts + 3 member lists. `first: 30` asks for more members than exist (25); still one statement.
8. **8**: the dashboard on the batched app, lab 9.3: one per field per level, with the owner's reviews and the
   members' reviews sharing a batch.
9. **4**: clubs, owners (one batch), the owners' reviews (one batch), their books (one batch).
10. **3**: two root fields each load their club (root fields aren't batched in this app), and the two
    `members` loads share one batch.
