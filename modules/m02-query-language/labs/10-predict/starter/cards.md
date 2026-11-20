# Predict the response: the 10 cards

Write the **whole response** each card returns into `predictions/NN.json`. For a card that fails,
write `{"errors": ["anything"]}`: the error message is not scored, only whether there is `data`, what it is, and whether there are `errors`.

## Card 01

```graphql
{ club(id: "c9") { name } }
```

## Card 02

```graphql
{ club(id: "c2") { members { name } } }
```

## Card 03

```graphql
{ book(id: "b3") { reviews { rating } } }
```

## Card 04

```graphql
{ a: hello b: hello }
```

## Card 05

```graphql
{ x: club(id: "c1") { name } x: club(id: "c1") { memberCount } }
```

## Card 06

```graphql
{ hello @skip(if: false) @include(if: false) }
```

## Card 07

```graphql
{ search(text: "kemi") { ... on Club { name } } }
```

## Card 08

```graphql
{ __typename club(id: "c3") { __typename visibility } }
```

## Card 09

```graphql
{ club(id: "c1") { owner } }
```

## Card 10

```graphql
query Two($n: Int = 2) { club(id: "c1") { members(first: $n) { name } } }
```

