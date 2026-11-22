# Hints: m14/02

1. Spring's `Locale` controller argument is the first `Accept-Language` range, or the JVM's default when there is
   none: neither "every range" nor "English when absent". Read the header yourself in a `WebGraphQlInterceptor`
   and put the parsed ranges into the `GraphQLContext`.
2. The JDK already negotiates: `Locale.LanguageRange.parse(header)` gives ranges sorted by weight (it throws on
   garbage), and `Locale.lookupTag(ranges, available)` does RFC 4647 lookup with region fallback. Formatting is a
   different question: `NumberFormat.getCurrencyInstance(locale)` with `setCurrency`, and
   `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(locale)`.
3. Keep the instant and only change how it is written: `instant.atZone(ZoneId.of(id))`. Adding minutes to a
   `LocalDateTime` ignores the night the clocks change; adding them to the instant does not. For money,
   `new BigDecimal(text).stripTrailingZeros().scale()` against `Currency.getDefaultFractionDigits()`.
