# Hints: m15/05

1. In CI, composition is a gate: the artifact is only replaced when it is valid. Write to a temporary file and
   move it into place only on success. `node compose.mjs` already exits 1 and prints `CODE: message` lines.
2. A plan starts at the subgraph that owns the root field. A new step starts whenever the next subgraph needs a
   key (or a `@requires` field) that only an earlier step can provide; subgraphs that need only keys the same
   earlier step returned run in parallel.
3. Look for the directives that move steps: `@provides` can make a step unnecessary, `@requires` can add one, and
   the router collects every representation at one path into a single `_entities` request, so a list is one
   request, not one per item.
