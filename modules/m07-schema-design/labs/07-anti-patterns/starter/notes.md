# Lab 7.7 notes

## 1. The worst five

The five anti-patterns in `bad.graphqls` that cost clients the most, worst first. For each: where
(coordinates), what it costs, and what replaced it in `good.graphqls`.

## 2. The kitchen sink

How you split `updateUser`, and the rule you used to decide which fields go together.

## 3. Bounded contexts

Map every type in `good.graphqls` to one bounded context (Clubs, Catalogue, Reviews, Profiles,
Notifications, or one you name). Which types sit on a border, and which context should own them?
This map is the input to federation in Module 15.

| Context | Types | Owns the data for |
|---|---|---|

## 4. What you kept on purpose

One thing from `bad.graphqls` that the lint would not flag but you changed anyway, or one thing the lint
flags that you would defend in a real API.
