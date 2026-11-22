# Lab 14.2: Locales, time zones and money

Lecture: [14.2 Locales, time zones, money](../../lectures/14.2-locales.md). Grading: auto + rubric. Needs nothing.

Readers of one Shelfie live in different languages and zones. The schema's comments and descriptions are the
rules:

- **Language** from `Accept-Language` (every range, in preference order; a region falls back to its language;
  no match, no header or a malformed header means English). `blurb` is in the best language Shelfie has, and
  `blurbLanguage` says which. Numbers and dates are written in the reader's **first** locale.
- **Time**: instants stored in UTC; `startsAt(timeZone:)`, `endsAt`, `localDate` and `startsAtText` render them
  in an IANA zone (extended-scalars `DateTime` and `Date`). An unknown zone is a `BAD_REQUEST` error.
- **Money**: `Money { amount: String! currency: String! formatted: String! }`, never a Float. `setPrice` validates
  the amount against the currency's decimals (yen has none), positivity and an ISO 4217 code.

The starter works for an English reader in London only. The expected strings come from the JDK's CLDR data and
contain no-break spaces (`\u{00A0}`) and narrow no-break spaces (`\u{202F}`): look at the bytes, not the screen.

```bash
bin/course start m14/02
cd workspace/m14-patterns/02-locales
bin/course shell -c 'mvn -q package && java -jar target/shelfie-locales.jar'
curl -s localhost:4000/graphql -H 'Accept-Language: de-DE' -H 'Content-Type: application/json' \
  -d '{"query":"{ book(id: \"b4\") { blurbLanguage price { formatted } } }"}'
bin/course test m14/02
```

The grader runs 19 checks, then prints the experiment: one meetup for three readers (en-US, de-DE, ja-JP) in three
zones (New York, Berlin, Tokyo).

In `notes.md`: the experiment's table and what it shows, where the language comes from in Spring, and money.

## Done when

`bin/course test m14/02` passes and `notes.md` passes the rubric (`bin/course grade m14/02`).
