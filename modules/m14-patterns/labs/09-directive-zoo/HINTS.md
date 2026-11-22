# Hints: m14/09

1. Lab 14.1's wiring pattern works unchanged: one `SchemaDirectiveWiring` per directive, each wrapping the field's
   data fetcher, all registered in one `RuntimeWiringConfigurer`. Refuse non-String fields at start-up.
2. "Whole words" means a regex over letters (`[A-Za-z]+`) with `Matcher.appendReplacement`, not
   `String.replace`, which turns "this" into "thbe". `Matcher.quoteReplacement` matters for "th'".
3. The day of the week depends on the zone: `LocalDate.now(clock)` uses the clock's zone, so inject the `Clock`
   bean into the configuration and build the wiring with it. 23:30 UTC on 9 July 2026 is already Friday in London.
