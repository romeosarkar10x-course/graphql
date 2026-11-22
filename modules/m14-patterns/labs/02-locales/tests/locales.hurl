# Lab 14.2: languages, time zones and money. Expected strings come from the JDK 25's CLDR data, as the reference
# solution prints them: \u{00A0} is a no-break space, \u{202F} a narrow no-break space.

# L01 no Accept-Language: English
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { blurbLanguage blurb price { amount currency formatted } pagesText } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "en"
jsonpath "$.data.book.blurb" startsWith "An envoy"
jsonpath "$.data.book.price.amount" == "11.99"
jsonpath "$.data.book.price.currency" == "EUR"
jsonpath "$.data.book.price.formatted" == "€11.99"
jsonpath "$.data.book.pagesText" == "304"

# L02 German: content and formats
POST {{url}}
Content-Type: application/json
Accept-Language: de-DE,de;q=0.9
{"query": "{ book(id: \"b4\") { blurbLanguage price { formatted } pagesText } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "de"
jsonpath "$.data.book.price.formatted" == "1.234,56\u{00A0}€"
jsonpath "$.data.book.pagesText" == "1.234"

# L03 a region Shelfie has no text for falls back to the language: fr-CH reads French
POST {{url}}
Content-Type: application/json
Accept-Language: fr-CH
{"query": "{ book(id: \"b1\") { blurbLanguage blurb } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "fr"
jsonpath "$.data.book.blurb" startsWith "Un envoyé"

# L04 every range counts: no Spanish blurb, so the second choice; numbers still in Spanish
POST {{url}}
Content-Type: application/json
Accept-Language: es, de;q=0.8, en;q=0.5
{"query": "{ book(id: \"b1\") { blurbLanguage price { formatted } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "de"
jsonpath "$.data.book.price.formatted" == "11,99\u{00A0}€"

# L05 a language with no blurb at all: English text, Japanese formats
POST {{url}}
Content-Type: application/json
Accept-Language: ja-JP
{"query": "{ book(id: \"b2\") { blurbLanguage price { formatted } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "en"
jsonpath "$.data.book.price.formatted" == "€12.50"

# L06 French groups thousands with a narrow no-break space
POST {{url}}
Content-Type: application/json
Accept-Language: fr-FR
{"query": "{ book(id: \"b4\") { price { amount formatted } pagesText } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.price.amount" == "1234.56"
jsonpath "$.data.book.price.formatted" == "1\u{202F}234,56\u{00A0}€"
jsonpath "$.data.book.pagesText" == "1\u{202F}234"

# L07 a malformed header is English, not an error
POST {{url}}
Content-Type: application/json
Accept-Language: ;;;q=x
{"query": "{ book(id: \"b1\") { blurbLanguage } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.blurbLanguage" == "en"
jsonpath "$.errors" not exists

# T01 one instant, written at the offset of the zone asked for; UTC by default
POST {{url}}
Content-Type: application/json
{"query": "{ meetup(id: \"m1\") { utc: startsAt berlin: startsAt(timeZone: \"Europe/Berlin\") ny: startsAt(timeZone: \"America/New_York\") kolkata: startsAt(timeZone: \"Asia/Kolkata\") } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetup.utc" == "2026-12-03T18:30:00.000Z"
jsonpath "$.data.meetup.berlin" == "2026-12-03T19:30:00.000+01:00"
jsonpath "$.data.meetup.ny" == "2026-12-03T13:30:00.000-05:00"
jsonpath "$.data.meetup.kolkata" == "2026-12-04T00:00:00.000+05:30"

# T02 the night the clocks go forward: two hours of real time end at 04:30 on Berlin's clocks
POST {{url}}
Content-Type: application/json
{"query": "{ meetup(id: \"m2\") { durationMinutes startsAt(timeZone: \"Europe/Berlin\") endsAt(timeZone: \"Europe/Berlin\") } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetup.durationMinutes" == 120
jsonpath "$.data.meetup.startsAt" == "2027-03-28T01:30:00.000+01:00"
jsonpath "$.data.meetup.endsAt" == "2027-03-28T04:30:00.000+02:00"

# T03 the calendar day depends on where you are
POST {{url}}
Content-Type: application/json
{"query": "{ meetup(id: \"m3\") { ny: localDate(timeZone: \"America/New_York\") tokyo: localDate(timeZone: \"Asia/Tokyo\") utc: localDate } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetup.ny" == "2026-12-31"
jsonpath "$.data.meetup.tokyo" == "2027-01-01"
jsonpath "$.data.meetup.utc" == "2026-12-31"

# T04 meetups come soonest first, by instant
POST {{url}}
Content-Type: application/json
{"query": "{ meetups { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetups[*].id" count == 3
jsonpath "$.data.meetups[0].id" == "m1"
jsonpath "$.data.meetups[1].id" == "m3"
jsonpath "$.data.meetups[2].id" == "m2"

# T05 an unknown zone is the client's mistake
POST {{url}}
Content-Type: application/json
{"query": "{ meetup(id: \"m1\") { title startsAt(timeZone: \"Mars/Olympus\") } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"
jsonpath "$.errors[0].path[1]" == "startsAt"
jsonpath "$.errors[0].message" contains "Mars/Olympus"

# T06 written for a person: locale and zone together
POST {{url}}
Content-Type: application/json
Accept-Language: en-US
{"query": "{ meetup(id: \"m1\") { startsAtText(timeZone: \"Asia/Tokyo\") } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetup.startsAtText" == "Friday, December 4, 2026, 3:30\u{202F}AM"

# T07 the same instant for a German reader in Berlin
POST {{url}}
Content-Type: application/json
Accept-Language: de-DE
{"query": "{ meetup(id: \"m1\") { startsAtText(timeZone: \"Europe/Berlin\") } }"}
HTTP 200
[Asserts]
jsonpath "$.data.meetup.startsAtText" == "Donnerstag, 3. Dezember 2026, 19:30"

# M01 money is a decimal string, never a Float
POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"Money\") { fields { name type { ofType { name } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[?(@.name == 'amount')].type.ofType.name" contains "String"

# M02 more decimals than the currency has is refused, the price unchanged
POST {{url}}
Content-Type: application/json
{"query": "mutation { setPrice(bookId: \"b1\", price: {amount: \"12.345\", currency: \"EUR\"}) { book { price { amount } } userErrors { field message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.setPrice.userErrors[0].field[0]" == "price"
jsonpath "$.data.setPrice.userErrors[0].field[1]" == "amount"
jsonpath "$.data.setPrice.book.price.amount" == "11.99"

# M03 yen has no decimals; an unknown code and a negative or exponent amount are refused
POST {{url}}
Content-Type: application/json
{"query": "mutation { a: setPrice(bookId: \"b1\", price: {amount: \"12.5\", currency: \"JPY\"}) { userErrors { field } } b: setPrice(bookId: \"b1\", price: {amount: \"10\", currency: \"EURO\"}) { userErrors { field } } c: setPrice(bookId: \"b1\", price: {amount: \"-3\", currency: \"EUR\"}) { userErrors { field } } d: setPrice(bookId: \"b1\", price: {amount: \"1E+3\", currency: \"EUR\"}) { userErrors { field } } e: setPrice(bookId: \"b1\", price: {amount: \"0.00\", currency: \"EUR\"}) { userErrors { field } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.userErrors[0].field[1]" == "amount"
jsonpath "$.data.b.userErrors[0].field[1]" == "currency"
jsonpath "$.data.c.userErrors[0].field[1]" == "amount"
jsonpath "$.data.d.userErrors[0].field[1]" == "amount"
jsonpath "$.data.e.userErrors[0].field[1]" == "amount"

# M04 a valid price in another currency: stored exactly, written with that currency's decimals
POST {{url}}
Content-Type: application/json
{"query": "mutation { setPrice(bookId: \"b2\", price: {amount: \"1500\", currency: \"JPY\"}) { book { price { amount currency formatted } } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.setPrice.userErrors" count == 0
jsonpath "$.data.setPrice.book.price.amount" == "1500"
jsonpath "$.data.setPrice.book.price.currency" == "JPY"
jsonpath "$.data.setPrice.book.price.formatted" == "¥1,500"

# M05 trailing zeros within the currency's decimals are fine: 9.50 EUR, shown as 9.50
POST {{url}}
Content-Type: application/json
{"query": "mutation { setPrice(bookId: \"b1\", price: {amount: \"9.5\", currency: \"EUR\"}) { book { price { amount } } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.setPrice.userErrors" count == 0
jsonpath "$.data.setPrice.book.price.amount" == "9.50"
