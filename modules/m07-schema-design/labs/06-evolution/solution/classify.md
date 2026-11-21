# Lab 7.6 classification

Each change is `changes/NN.graphqls` against `changes/base.graphqls`. Fill **Guess** before running
anything (BREAKING, DANGEROUS or SAFE). Then run `bin/course design diff` for the **SchemaDiff** column
and `bin/course inspector diff` for the **inspector** column (breaking, dangerous or safe).

| # | Change | Guess | SchemaDiff | inspector |
|---|---|---|---|---|
| 01 | add Club.memberCount: Int | SAFE | SAFE | safe |
| 02 | remove Club.description | BREAKING | BREAKING | breaking |
| 03 | rename Book.coverUrl to coverImageUrl | BREAKING | BREAKING | breaking |
| 04 | add ShelfStatus.ABANDONED | DANGEROUS | DANGEROUS | dangerous |
| 05 | Club.members(first:) becomes Int! with no default | BREAKING | BREAKING | breaking |
| 06 | add an optional argument books(author: String) | SAFE | SAFE | dangerous |
| 07 | deprecate Club.description | DANGEROUS | DANGEROUS | safe |
| 08 | Club.currentBook becomes non-null (Book!) | SAFE | SAFE | safe |
| 09 | Club.name becomes nullable (String) | BREAKING | BREAKING | breaking |
| 10 | add a required input field CreateReviewInput.language: String! | BREAKING | BREAKING | breaking |
| 11 | add an optional input field CreateReviewInput.spoiler: Boolean | SAFE | SAFE | safe |
| 12 | Book.publishedYear changes from Int to String | BREAKING | BREAKING | breaking |
| 13 | add User to the SearchResult union | DANGEROUS | DANGEROUS | dangerous |
| 14 | remove ShelfStatus.WANT_TO_READ | BREAKING | BREAKING | breaking |
| 15 | change the default of books(first:) from 10 to 20 | DANGEROUS | DANGEROUS | dangerous |
