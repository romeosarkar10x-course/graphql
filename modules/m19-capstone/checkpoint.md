# Checkpoint 19: defend your capstone

`bin/course checkpoint 19` runs stages 1 to 4 against your repository and tags `checkpoint-19`. Before demo
day, answer these about your own product, out loud or in `docs/QA.md`. Each folded note says what a good
answer covers, not the answer: the answers are yours.

1. Which field in your schema would you most like to change, and what would it cost each client to change it now?
   <details><summary>A good answer</summary>Names a real field, classifies the change (breaking, dangerous,
   safe), and gives the path: add the new field, deprecate, measure usage (Module 16.2), remove.</details>

2. Stop one subgraph. What does each main screen show?
   <details><summary>A good answer</summary>Has been tried, not reasoned: which fields go null, how far the
   null travels (nullability), what the client does with the error, and whether that was designed.</details>

3. A client sends the same mutation twice because the first response was lost. What happens?
   <details><summary>A good answer</summary>Distinguishes the operations where it matters from those where it
   does not, names the key and its scope, where the first answer is stored, and in which transaction.</details>

4. What is the most expensive request a client can make, and what stops it?
   <details><summary>A good answer</summary>A concrete operation and variables, its estimated cost, and the
   limits in order: trusted documents (which shapes exist at all), cost, page limits, depth.</details>

5. Which user can see which private field, and where is that enforced?
   <details><summary>A good answer</summary>A table of rules, the layer (router, resolver, service), what a
   refused caller sees, and whether a refused caller can tell that the object exists.</details>

6. How do you know you have no N+1, and how would you know if a deploy introduced one?
   <details><summary>A good answer</summary>A statement count per request that does not grow with the page,
   a test that asserts it, and a dashboard panel or alert on statements per request.</details>

7. Your load test: what was the bottleneck, and was the test itself fair?
   <details><summary>A good answer</summary>Where time went (a trace or a profile, not a guess), what the
   generator shared with the server, how many runs, and what the router or a cache did to the numbers.</details>

8. What would you do next, in order, and why that order?
   <details><summary>A good answer</summary>The known limits from the design doc, ranked by risk to users,
   each with its cost: the honest list, not a feature wish list.</details>
