# Hints, lab 7.4

1. Go row by row through the facts table. "Can it be absent?" yes means plain nullable and you're
   done. Only if the answer is no, ask whether it comes from a different service than its parent.
2. `owner` is the tricky one: the owner's **id** is on the club row, so the `User` object always
   exists; what can fail is the profile behind it, which is `User.name`'s problem, not `Club.owner`'s.
   Make `User.name` nullable and `owner` can keep its `!`.
3. `@semanticNonNull` goes after the type: `name: String @semanticNonNull`. It only makes sense on a
   nullable field. A field that is absent in correct data (`avatarUrl`) must not have it: null there is
   an answer, not an error.
