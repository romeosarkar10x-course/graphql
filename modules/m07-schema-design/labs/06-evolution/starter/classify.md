# Lab 7.6 classification

Each change is `changes/NN.graphqls` against `changes/base.graphqls`. Fill **Guess** before running
anything (BREAKING, DANGEROUS or SAFE). Then run `bin/course design diff` for the **SchemaDiff** column
and `bin/course inspector diff` for the **inspector** column (breaking, dangerous or safe).

| # | Change | Guess | SchemaDiff | inspector |
|---|---|---|---|---|
| 01 | add Club.memberCount: Int | | | |
| 02 | remove Club.description | | | |
| 03 | rename Book.coverUrl to coverImageUrl | | | |
| 04 | add ShelfStatus.ABANDONED | | | |
| 05 | Club.members(first:) becomes Int! with no default | | | |
| 06 | add an optional argument books(author: String) | | | |
| 07 | deprecate Club.description | | | |
| 08 | Club.currentBook becomes non-null (Book!) | | | |
| 09 | Club.name becomes nullable (String) | | | |
| 10 | add a required input field CreateReviewInput.language: String! | | | |
| 11 | add an optional input field CreateReviewInput.spoiler: Boolean | | | |
| 12 | Book.publishedYear changes from Int to String | | | |
| 13 | add User to the SearchResult union | | | |
| 14 | remove ShelfStatus.WANT_TO_READ | | | |
| 15 | change the default of books(first:) from 10 to 20 | | | |
