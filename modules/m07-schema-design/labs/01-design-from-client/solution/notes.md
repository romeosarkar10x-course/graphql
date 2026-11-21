# Lab 7.1 notes

## 1. Screens first

- Club page: the owner as a person (name, avatar), not `owner_user_id`; the current book with authors as
  a list and a rating; the next meetup; the first 5 members, not every id; `viewerIsMember`, which
  depends on the caller and cannot come from the club row.
- Book page: authors as objects (`author_names` was a comma-joined string), a review count, the 3
  newest reviews with their authors, and the clubs reading the book now.
- Profile: entry by handle, not by numeric id (profile links are `/@asha`); the member's role in each
  club, which lives on the membership, not on the user or the club; the shelf with a status per book.

## 2. Decisions

- `user(handle:)`, not `getUserById(user_id:)`: the screen arrives with a handle from the URL. Names
  are nouns; `get` adds nothing.
- `Club.currentBook: Book` is nullable: between books there is none, and the screen shows "choosing next
  book". `Club.owner: User!` is non-null: a club without an owner is a data bug.
- `members(first: Int = 10)` with the page asking for 5, aliased to `memberPreview`: the field is
  reusable by a full member list later; the screen's name lives in the query.
- `Membership` as its own type: `role` and `joinedAt` belong to the pair (member, club), not to either.

## 3. The SDL-only test

(Reference notes: this is the shape of a good answer, not a measured run. Your count is yours.)
An answer here names the tester, lists each question verbatim, and sorts them: questions a
description would have answered (is the rating 1 to 5 or 0 to 10? is `members` sorted? null or false
for `viewerIsMember` when signed out? what does a null `currentBook` mean?) and questions that show a
missing field or a wrong entry point (where does the role come from? how do I get from a handle to a
user?). Description gaps are the common case; missing fields mean the design missed a screen need.

## 4. One change after the test

Each description gap becomes a sentence in the schema: the scale and the null case on
`averageRating`, "Null between books" on `currentBook`, "by join date, oldest first" on `members`,
"Null when nobody is signed in" on `viewerIsMember`. A good answer ends with a second run of the same
test and how many questions remain.
