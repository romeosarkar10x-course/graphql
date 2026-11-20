# Hints, lab 0.3

1. Open each endpoint in a browser (SWAPI and Countries serve GraphiQL at their URL). Type `{` and press Ctrl+Space: the explorer lists every root field. Open the docs pane to see each field's arguments.
2. A field with arguments takes them in parentheses right after its name, and every object-typed field needs its own `{ ... }` selection. `languages` is a list of objects, so it needs a selection too.
3. Shape: `query Name { rootField(argument: "value") { scalarField anotherField listField { name } } }`. The GitHub one starts at the root field that returns the authenticated user, and repositories are a *connection*: the names live under `nodes`.
