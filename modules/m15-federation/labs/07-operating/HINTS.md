# Hints: m15/07

1. A field may be resolved by two subgraphs at once only if composition knows which one wins. Federation 2 has a
   directive for exactly this: the new owner claims the field from the old one, and both can be deployed.
2. Removing a field from the old owner before the router stops asking it is a broken page. Count the steps as
   deploys of one team each: claim, then remove, then tidy.
3. The new owner needs the data the field is computed from (reviews has `bookId`) and a way to return a `Book`
   it does not own (a key-only stub). If a step fails the "one subgraph changes" check, you have merged two
   teams' deploys into one.
