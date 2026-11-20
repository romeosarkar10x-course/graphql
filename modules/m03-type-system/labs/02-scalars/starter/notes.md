# Lab 3.2 notes

## Experiment 1: Long.MAX_VALUE as Int

In the starter, make `totalPageViews` return `Long.MAX_VALUE`. Paste the error. At which stage does it
happen (validation or execution), and what is `data`?

## Experiment 2: the same number on the way in

Query `{ review(id: 3000000000) { id } }` against the starter schema. Paste the error. Same stage as
experiment 1? Why?

## Experiment 3: JavaScript

In a browser console or `node`, run `JSON.parse('{"id": 9007199254740993}').id`. What do you get, and
what does it mean for a client that stores IDs as numbers?

## Question

`Long` (a JSON number) or `String` for `totalPageViews`? Argue one, naming who reads the field.
