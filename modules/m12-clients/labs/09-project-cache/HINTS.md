# Hints: m12/09

1. Work in scenario order; each adds one idea. Fragments (11, 12) and directives (13) change only *which
   selections apply*: handle `InlineFragment` and `FragmentSpread` by recursing into their selection sets with
   the same record, when the type condition matches the object's `__typename` (or `possibleTypes` lists it).
2. Policies are keyed by `ParentType.field`, so your walk must know the parent's type name (`Query` at the root,
   the record's `__typename` below). `keyArgs` filters the arguments before building the key; `merge: append`
   reads what is already stored under that key before writing; `read: toReference` is a fallback when a read
   misses.
3. For optimistic layers keep a list of partial stores on top of the base and make every field lookup search
   top layer first, then down to the base; writes go to one store. Watches are cheap if you simply re-run each
   watch's read after any change and compare with its previous result.
