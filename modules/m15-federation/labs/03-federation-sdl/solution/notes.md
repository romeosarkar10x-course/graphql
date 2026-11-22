# Lab 15.3 notes

## Remove a @key: the composition error

With both `@key` directives removed from catalog's `Book`, composition fails with
`INTERFACE_KEY_NOT_ON_IMPLEMENTATION`: "Key @key(fields: "id") on interface type "Media" is missing on implementation
type "Book"". Not the "Book is not an entity" message I expected: the first rule to trip is the interface's, because
an entity interface promises that every implementation can be looked up by that key, and reviews relies on it
(`Media @interfaceObject` gets a `Media` id and must reach the concrete `Book`). In the smaller contract graph
without `Media` the same edit fails differently, `INVALID_FIELD_SHARING` on `Book.id`: without the key catalog's
`Book` is a plain value type, and reviews and clubs still define `Book.id`, so three subgraphs claim one
non-shareable field. Either way the key is what makes the duplicated `id` legal: it is the join column, not a
shared field. Composition reports the first stage that fails, so one missing directive can surface as an error
about something else.

## Which directive solves which team problem

- `@key`: two teams talk about the same thing (a book) and need a way to point at it; catalog offers two
  (`id` for most, `isbn` for partners that only know ISBNs).
- `@shareable`: a field (`User.handle`) that more than one team can answer, so the router may pick either.
- `@external` + `@provides`: reviews stores a copy of the handle with each review; when the router reaches a
  user through `Review.author` it can skip accounts.
- `@external` + `@requires`: clubs needs catalog's `pages` to compute `weeksToRead`; the router fetches
  `pages` first and sends it in the representation.
- `@override(label: "percent(25)")`: clubs takes `User.clubCount` over from accounts, a quarter of the
  traffic at a time.
- `@inaccessible`: `email` stays usable between subgraphs, hidden from clients.
- `@interfaceObject`: reviews attaches `reviews` and `averageRating` to every `Media` without knowing that
  `Book` and `Audiobook` exist; catalog can add `Podcast` without a reviews deploy.
- `@tag`: marks the public contract (a filtered schema for partners) without a second schema.

## What composition checked that a code review would miss

That a field defined twice is shareable, that every `@external` field is provided or required somewhere,
that `@override` names a subgraph that really declares the field, and that every type reached from a key
can be resolved: the "satisfiability" check finds a query the router could not plan before any user sends it.
