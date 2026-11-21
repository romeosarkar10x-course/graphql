# Hints, lab 7.3

1. A field that returns an object needs a fetcher that does the lookup the client used to do:
   `owner` is `Data.user(club.ownerId())`. The SDL change and the fetcher change come in pairs.
2. The connection is three records (`Edge`, `PageInfo`, `Connection`) returned from one `members`
   fetcher; graphql-java's default fetcher reads record components, so `edges`, `cursor`, `node`, `role`
   need no wiring of their own. `role` can be the enum's `name()`.
3. A cursor only needs to tell you where to start: encode the edge's index (base64 of `"member:3"`
   keeps it opaque), and `after` means "index + 1". `hasNextPage` is "is there anything after the last
   edge of this page?". `first` defaults to 10 in the SDL, so the argument is never null.
