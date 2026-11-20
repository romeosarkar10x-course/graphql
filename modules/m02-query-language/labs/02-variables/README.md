# Lab 2.2 — Arguments and variables

**Lecture:** [2.2 Arguments and variables](../../lectures/2.2-variables.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

`BookSearch.java` searches books by title. It builds its query with `String.format`. Break it first,
then fix it with variables.

## Do

1. `bin/course start m02/02`, then work in `workspace/m02-query-language/02-variables/`.
2. **Break it.** Run the starter with an ordinary search, with `'say "hi"'`, and with the payload from the
   lecture:

   ```bash
   ../../../bin/course java BookSearch.java the
   ../../../bin/course java BookSearch.java 'say "hi"'
   ../../../bin/course java BookSearch.java '", first: 20) { publishedYear title } __schema { types { name } } } #'
   ```

   Print the document the starter actually sends (add one `System.err.println(query)`) and write up what
   happened in `notes.md`.
3. **Fix it.** Move the document to `book-search.graphql` as a named operation with two variables:
   `$text: String!` and `$first: Int = 5`. Send the user's input in the `variables` map. Accept an
   optional second argument as the limit; when it is missing, leave `first` out of the map so the
   default applies.
4. Run the three commands again. All three must now be harmless: the payload is searched for as a title
   and finds nothing.

Output format (unchanged from the starter): one `year  title` line per book, then `found N`.

## Done when

`bin/course test m02/02` is green and `bin/course grade m02/02` gives 7/10 or more.
