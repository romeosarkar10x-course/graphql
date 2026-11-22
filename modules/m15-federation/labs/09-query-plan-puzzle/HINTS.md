# Hints: m15/09

1. Step one is always the subgraph that owns the root field. Every later step is caused by a field the previous
   steps cannot answer, fetched by key.
2. Fields resolvable from the same earlier step's keys run in parallel in one step. `@provides` on the path can
   answer a field without a step; `@requires` can add one, because the required field must be fetched first.
3. A list does not multiply steps: the router collects every representation at one path into one `_entities`
   request. If you are off by one step, look for a field that needs a field another subgraph adds to the same
   entity.
