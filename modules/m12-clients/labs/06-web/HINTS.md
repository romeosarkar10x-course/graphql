# Hints: m12/06

1. Relay builds the query from fragments: each component file declares one (`fragment BookPanel_book on Book
   { ... }`, the name must start with the file's name) and reads it with `useFragment(fragment, ref)`; the
   parent's query spreads them (`currentBook { id ...BookPanel_book }`) and passes the reference down as a prop.
2. `bin/course web run measure` runs `relay-compiler` first; read its errors, they name the file and the
   rule. Each `graphql` tag must sit in a file under `src/relay/`. `useLazyLoadQuery` suspends, so the harness
   wraps the Relay card in `<Suspense>` already.
3. The mutation is `commitMutation(environment, { mutation: graphql\`mutation addReviewMutation(...) { ... }\`,
   variables, onCompleted, onError })` wrapped in a Promise, in `src/relay/addReview.js` exporting
   `addReview(environment, input)`. Select the book's `id` and `averageRating` in its response, or nothing in
   the store changes.
