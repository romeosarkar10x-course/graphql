# Lab 2.7 notes

## Two operations, no operationName

The exact error:

Now try `{ hello } query B { hello }` with operationName `B`. What is the rule this time?

## Operation descriptions

The September 2025 spec lets you write `"Shows a club card" query ClubCard { ... }`. What did the
reference server say when you tried it? What did you use instead in `ops/`?

## Request extensions

Send `{"query": "{ hello }", "extensions": {"clientName": "lab-2.7"}}`. What happened? Then send
`"extensions": "x"`. What is the field for, and who reads it?

## Why a folder of named operations?

Two things a team gets from keeping every operation as a named file:
