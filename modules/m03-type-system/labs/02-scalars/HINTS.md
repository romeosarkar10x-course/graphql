# Hints, lab 3.2

1. graphql-java gives an `ID` argument to your fetcher as a `String`, whatever the client sent (a JSON string or an integer). `Long.parseLong` it inside a `try`.
2. Output: `ID` serialises numbers as strings, but say what you mean: return `Long.toString(id)` from an `AnalyticsReview.id` fetcher. For `Long`, register `ExtendedScalars.GraphQLLong` with `.scalar(...)` on the wiring *and* declare `scalar Long` in the SDL; either alone fails at startup.
3. `@specifiedBy` takes one argument, `url`, and goes after the scalar's name: `scalar Long @specifiedBy(url: "...")`. Any stable URL that defines the format is fine.
