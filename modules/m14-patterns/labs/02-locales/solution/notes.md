# Lab 14.2 notes

## Three readers, three zones

The grader's table for meetup m1 (18:30 UTC on 3 December): New York reads Thursday 1:30 PM, Berlin 19:30 the same
day, Tokyo Friday 3:30 AM, each in the reader's own words ("Donnerstag, 3. Dezember 2026, 19:30", "2026年12月4日金曜日
3:30"). One stored instant, nine renderings; the server never stores a local time. The DST meetup (m2) lasts 120
minutes of real time and ends at 04:30+02:00 in Berlin: adding hours to a wall-clock time would have said 03:30.

## Where the language comes from

Spring gives a controller one `Locale`, the first range of Accept-Language, or the JVM's default when there is no
header; that default is the server's, not the reader's. Negotiation needs every range: "es, de;q=0.8" must read
German when there is no Spanish. The interceptor puts the parsed ranges in the context and `Locale.lookupTag`
does RFC 4647 lookup, so fr-CH falls back to fr. Content language and formatting locale are different questions:
a Spanish reader gets German text but Spanish numbers.

## Money

A Float cannot hold 0.10 exactly and loses cents in sums, so the schema sends a decimal string with an ISO 4217
code, and input is validated against the currency's decimals (yen has none). Formatting belongs to the server
only as a convenience (`formatted`); clients that compute keep `amount` and `currency`. The CLDR output includes
no-break and narrow no-break spaces, which break naive string comparisons and tests.
