# Lab 2.3 notes (reference)

## The collision

The exact error message: `Validation error (FieldsConflict) : 'club' : fields have different arguments`,
with locations of both fields.

Why the rule exists: a response is a JSON object keyed by response name (the alias, or else the field
name). Two `club` entries with different arguments would need two values under one key. The spec rule
"Field Selection Merging" rejects it during validation, before any resolver runs.

## Same key, same arguments

`{"data":{"x":{"name":"The Midnight Readers","memberCount":8}}}`. Fields with the same response key,
the same field and the same arguments are merged: their selection sets combine into one object. This
is also what happens when two fragments select overlapping fields (lecture 2.4).

## Where aliases earn their keep

- A comparison screen: `left: club(id: $a)` and `right: club(id: $b)`, as in this lab.
- A profile header: `avatarSmall: avatarUrl(size: 40)` and `avatarLarge: avatarUrl(size: 400)`; on
  Shelfie, `recent: reviews(last: 3)` beside `all: reviews(last: 50)`.
- Renaming for the client: `rating: averageRating` so the JSON matches an existing view model.
