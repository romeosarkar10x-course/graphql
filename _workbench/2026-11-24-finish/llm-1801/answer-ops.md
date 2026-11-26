### q01
```graphql
query { clubs { id name visibility currentBook { id title averageRating } } }
```

### q02
```graphql
query { books(first: 1000) { id title authors publishedYear } }
```

### q03
```graphql
query { search(text: "The Midnight Readers") { __typename ... on Club { id name owner { id name handle location } } } }
```

### q04
```graphql
query { search(text: "Booker Shortlist Survivors") { __typename ... on Club { id name memberCount } } }
```

### q05
```graphql
query { search(text: "Jonas Berg") { __typename ... on User { id name handle reviews { rating body createdAt book { id title } } } } }
```

### q06
```graphql
query { books(first: 1000) { id title averageRating reviews(last: 1) { id } } }
```

### q07
```graphql
query { books(first: 1000) { id title authors publishedYear } }
```

### q08
```graphql
query { search(text: "Asha Rao") { __typename ... on User { id name handle clubs { id name } } } }
```

### q09
```graphql
query { search(text: "The Midnight Readers") { __typename ... on Club { id name meetups(upcomingOnly: false) { title startsAt location } } } }
```

### q10
```graphql
query { books(first: 1000) { id title pageCount } }
```
