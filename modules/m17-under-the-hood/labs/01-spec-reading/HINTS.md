# Hints: 17.1 reading the spec

1. Decide first which part of the spec each behaviour belongs to: a document rejected before anything runs is
   validation (5), something that happens while running is execution (6), the range of a value is the type system
   (3). Then read that part's table of contents.
2. Validation rules have an informal paragraph and a formal specification in algorithm steps; the behaviour is in
   the steps. Two of the five are decided inside an algorithm (`GetOperation`, `CollectSubscriptionFields`):
   search for their names.
3. Null propagation and the Int range are each defined once, in a subsection that is only a few paragraphs long;
   other sections refer to them. If you found a section that says "see", follow it.
