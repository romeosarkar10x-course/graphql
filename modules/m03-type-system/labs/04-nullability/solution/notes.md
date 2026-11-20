# Lab 3.4 notes (reference; predictions/ holds the real outputs)

## Outcomes

| # | data | Rule |
|---|---|---|
| 01 | club with `currentBook: null` | `averageRating: Float!` fails; nearest nullable ancestor is `currentBook: Book` |
| 02 | `{"club": null}` | `currentBook: Book!` passes it up; `club: Club` is nullable |
| 03 | `null` | `club: Club!` passes it to the root: the whole `data` is null |
| 04 | `{"club": null}` | `[User!]!`: item non-null, list non-null, so the club goes |
| 05 | members `[Kemi, Asha, null]` | `[User]!`: the item is nullable, so only Ben's entry is null |
| 06 | `{"club": {"name": ..., "members": null}}` | `[User!]`: the list is nullable, so the whole list goes |
| 07 | `a.currentBook: null`, `b` intact | `Review.author: User!` and `[Review!]!` pass it up to `currentBook`; root fields are independent |
| 08 | `null` | A non-null field returning null is the same as an error; `[Club!]!` at the root blanks everything |

## The fix

`Book.averageRating: Float`: a rating is optional data from another service; "unknown" is honest.
`Club.members: [User]!`: one corrupt profile becomes one null entry, the list and the club survive. The
alternative, `User.name: String`, also works but weakens every User everywhere for one bad row; the list
item is the smaller blast radius. Clients must now handle null items.

## Question

One bad row (or one timeout) in any club erases the whole response, including unrelated root fields'
data, because `data` itself becomes null. Use `[Club]!` (or `[Club!]` plus a separate count), or keep
`[Club!]!` but make the fields that can fail nullable, so a failure never reaches the list.
