# Hints: 17.4 validation

1. Walk each operation with its parent type: the type a selection set is on decides which fields exist. Fragments
   and inline fragments change it to their type condition. Collect variable usages with the type their position
   expects (argument, input field or list item) as you go.
2. Cycles: a depth-first search over fragment spreads with an "on the current path" set; a spread to a fragment on
   the path is a cycle. Do not follow a fragment twice from the same start.
3. Overlaps: collect the fields of a selection set by response name (through fragments), compare every pair with
   the same name (different field or arguments is a conflict only when the parents can be the same object), then
   compare their sub-selections merged together. Remember which pairs of fragments you compared, or cycles will
   loop forever.
