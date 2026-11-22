# Hints: 18.4 An API you did not write

1. PostGraphile 5 decides what to generate from "behaviours": strings such as `insert`, `update`, `delete`,
   `queryField` attached to every table, column and function. Find the option in a preset that sets the default
   behaviour for everything.
2. A behaviour string can take things away: a leading `-` negates one, and `-*` removes them all. The global
   default handles the mutations; the function needs something that targets one entity by name.
3. Smart tags attach behaviours to one table or function. You can set them without touching the database:
   `postgraphile/utils` exports a plugin factory that takes smart tags as JSON, keyed by kind (`class`,
   `attribute`, `procedure`) and name.
