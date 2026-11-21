# Guess the query count: the rounds

Schema: `modules/m09-performance/contract/club.graphqls`. Rounds 1-7: the naive app (one repository call per field per parent, lab 9.1). Rounds 8-10: the batched app (lab 9.3).

## Round 1 (naive app)

```graphql
{ club(id: 1) { name } }
```

## Round 2 (naive app)

```graphql
{ club(id: 1) { owner { name } currentBook { title } } }
```

## Round 3 (naive app)

```graphql
{ clubs(first: 5) { owner { name } } }
```

## Round 4 (naive app)

```graphql
{ clubs(first: 10) { currentBook { title } } }
```

## Round 5 (naive app)

```graphql
{ clubs(first: 4) { members(first: 3) { reviews(first: 2) { book { title } } } } }
```

## Round 6 (naive app)

```graphql
{ a: club(id: 1) { name } b: club(id: 1) { name } }
```

## Round 7 (naive app)

```graphql
{ clubs(first: 3) { memberCount members(first: 30) { handle } } }
```

## Round 8 (batched app)

```graphql
the club dashboard, contract/dashboard.graphql
```

## Round 9 (batched app)

```graphql
{ clubs(first: 5) { owner { reviews(first: 3) { book { title } } } } }
```

## Round 10 (batched app)

```graphql
{ c1: club(id: 1) { members(first: 2) { handle } } c2: club(id: 2) { members(first: 2) { handle } } }
```

