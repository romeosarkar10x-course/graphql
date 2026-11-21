# Lab 6.8 notes

## Dev mode

Run `mvn quarkus:dev` (inside `bin/course shell`, from your workspace copy), open
`http://localhost:4000/graphql/schema.graphql`, then add a parameter to a `@Query` method and save.
How long until the schema shows it? What did you not have to do?

## Questions

1. Before your annotations the schema said `id: String` and `name: String`. Why are code-first defaults
   looser than what you wrote by hand in Modules 3 and 5, and who notices?
2. `currentBook(@Source List<Club> clubs)` ran once for three clubs. What would the same field cost with
   a plain `@Source Club` parameter and a database behind it? Name the Module 8 tool that does this in
   Spring.
3. Schema-first (Spring, DGS) or code-first (Quarkus): which would you choose for Shelfie's public API,
   and which for an internal service owned by one team?
4. Startup time and memory of this app against lab 6.1's (read both logs, and `ps -o rss`). What explains
   the difference?
