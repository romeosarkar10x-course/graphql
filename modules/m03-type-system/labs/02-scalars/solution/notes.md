# Lab 3.2 notes (reference; probe output in _workbench/2026-11-21-m03-probe/probe.txt)

## Experiment 1: Long.MAX_VALUE as Int

Execution, output coercion: `Can't serialize value (/stats/totalPageViews) : Expected a value that can
be converted to type 'Int' but it was a 'Long'`, classification `DataFetchingException`. The field is
`Int!`, so null bubbles: `stats` is `Stats!` too, so `data` is `null` for the whole response.

## Experiment 2: the same number on the way in

Validation, before anything runs: `Validation error (WrongType@[review]) : argument 'id' with value
'IntValue{value=3000000000}' is not a valid 'Int' - Expected value to be in the integer range`.
Literals are checked against the schema during validation; output values only exist during execution.
(Sent as a variable, it fails in variable coercion instead, also before execution.)

## Experiment 3: JavaScript

`9007199254740992`: off by one. JavaScript numbers are IEEE doubles, exact only to 2^53. A client
storing ids as numbers silently fetches, caches and updates the wrong row. IDs as strings avoid it.

## Question

Either, argued. `Long`: dashboards in Java or Python read it exactly; it is a count, so arithmetic on it
is the point; precision loss in JS starts at 9e15 views. `String`: browsers are the main reader and the
value must be exact everywhere. Weak answers ignore who reads it.
