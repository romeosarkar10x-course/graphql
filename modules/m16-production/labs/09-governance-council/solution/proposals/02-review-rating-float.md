# Proposal 2: half stars, Review.rating becomes Float (reviews team)

**Change.** `Review.rating: Int!` becomes `Review.rating: Float!` (values 0.5 to 5.0 in steps of 0.5), and
`addReview(rating: Int!)` becomes `addReview(rating: Float!)`.

**Why.** Users ask for half stars; the competitor has them.

**Usage today (7 days).** `Review.rating` is selected by web 3.2.0 (`ClubPage`, `BookPage`: 853 operations from
web in total), iOS 2.9.1 does not select it, Android 1.4.0 (`Rate` returns `rating`: part of 149 operations).
`addReview` is called only by Android 1.4.0. `Book.averageRating` (already `Float`) is selected by web and iOS.

**Note from the iOS team.** Their generated Swift types map `Int` to `Int`; a type change is a compile error, and
old app versions stay installed for months.
