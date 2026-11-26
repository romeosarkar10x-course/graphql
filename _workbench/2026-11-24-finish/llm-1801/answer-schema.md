### q01
```graphql
query { clubs { name visibility currentBook { title averageRating } } }
```

### q02
```graphql
query { books(first: 1000) { title authors } }
```

### q03
```graphql
query { clubs { name owner { name handle location } } }
```

### q04
```graphql
query { clubs { name memberCount } }
```

### q05
```graphql
query { books(first: 1, titleContains: "Project Hail Mary") { title reviews(last: 1000) { body rating createdAt author { name handle } } } }
```

### q06
```graphql
query { books(first: 1000) { title reviews(last: 1) { id } } }
```

### q07
```graphql
query { books(first: 1000) { title authors publishedYear } }
```

### q08
```graphql
query { search(text: "Asha") { ... on User { name handle clubs { name } } } }
```

### q09
```graphql
query { clubs { name meetups(upcomingOnly: false) { title startsAt location } } }
```

### q10
```graphql
query { books(first: 1000) { title pageCount } }
```
