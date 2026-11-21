# Hints, lab 8.4

1. A field spec looks like `(root, query, cb) -> cb.ge(root.get("published"), from)`. Copy the value
   into a local variable first, so the lambda doesn't keep calling `f.publishedFrom()` and you can
   unbox it once. For `genres: []`, `cb.disjunction()` is a predicate that is always false.
2. `cb.literal("dragon")` renders as `'dragon'` inside the SQL. Hibernate escapes it, but each search is
   a new SQL text and the value is visible in every log. Hibernate's own builder has a method that
   binds instead: cast `cb` to `org.hibernate.query.criteria.HibernateCriteriaBuilder` and look at
   `value(...)`. For RELEVANCE, the order isn't a property, so `Sort` can't express it. Set it in a
   spec, `query.orderBy(cb.desc(cb.function("book_rank", Float.class, ...)), cb.asc(root.get("id")))`,
   and return `cb.conjunction()` as that spec's predicate.
3. In jqwik, `Arbitraries.of(Genre.class).list().uniqueElements().ofMaxSize(3).injectNull(0.5)` gives
   genres, and `Combinators.combine(a, b, c).as(...)` builds a `BookFilter`. Swap `from` and `to` when
   they come out reversed, so every generated filter is valid. In the model, sort with
   `key.thenComparing(Book::getId)`, but apply `reversed()` to the key before adding the id, because the
   tie-breaker stays ascending.
