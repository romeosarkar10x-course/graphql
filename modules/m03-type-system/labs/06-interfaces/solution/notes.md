# Lab 3.6 notes (reference; probe in _workbench/2026-11-21-m03-probe/probe.txt)

## The startup error

`SchemaProblem{errors=[There is no type resolver defined for interface / union 'Node' type, ... 'Timestamped'
type, ... 'Reviewable' type]}`. All three. graphql-java checks the wiring when it builds the schema, so a
missing resolver is a deploy failure, not a 3 a.m. query failure; and every interface needs one, used or not.

## Experiment 1: forget one

`There is no type resolver defined for interface / union 'Timestamped' type`. Same check, one name.

## Experiment 2: the transitive rule

`The object type 'Book' must implement 'Node' because it is implemented by 'Reviewable'`. The spec
(since RFC #373) requires the full set to be listed so a reader of one type definition sees every
interface it satisfies, and so validation never has to chase chains to answer "is Book a Node?".

## Question

Either. Yes: clients write `... on Reviewable` once and a second reviewable type (meetups, clubs)
arrives without breaking them; `node`-style generic UI works. No: one implementation is speculative
generality, adds a type resolver and a level of indirection clients must learn; add it when the second
type appears (adding an interface later is non-breaking).
