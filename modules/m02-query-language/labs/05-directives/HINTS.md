# Hints, lab 2.5

1. Directives go after the thing they apply to: `teaser: members(first: 1) @skip(if: $detailed) { name }`. The `if` argument takes a Boolean literal or variable.
2. A directive on a spread applies to everything the fragment selects: `...ClubDetails @include(if: $detailed)`. A variable used in `if:` must be a non-null Boolean, which is why `$detailed` is `Boolean!`; the default makes it optional to send.
3. Why do `teaser: members(first: 1)` and `members(first: 3)` not collide? Different response keys. If you drop the alias, the two `members` fields clash (different arguments, lab 2.3), even though only one of them is ever included.
