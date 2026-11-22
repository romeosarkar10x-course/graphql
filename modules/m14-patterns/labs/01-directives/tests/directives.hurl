# Lab 14.1: the three directives, as the schema's descriptions define them.

# D01 @uppercase on a String field
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { genre title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.genre" == "SCIENCE FICTION"
jsonpath "$.data.book.title" == "The Left Hand of Darkness"

# D02 @uppercase on a [String] field: every item
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b5\") { spineAuthors } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.spineAuthors[0]" == "TERRY PRATCHETT"
jsonpath "$.data.book.spineAuthors[1]" == "NEIL GAIMAN"

# D03 @uppercase is Locale.ROOT: ß becomes SS, umlauts stay
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b6\") { genre spineAuthors } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.genre" == "STRASSE"
jsonpath "$.data.book.spineAuthors[0]" == "JÖRG WEISS"

# D04 @currency converts and rounds: 11.99 EUR x 1.08 = 12.9492
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { priceEur priceUsd } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.priceEur" == 11.99
jsonpath "$.data.book.priceUsd" == "USD 12.95"

# D05 @currency rounds half even: 12.50 x 0.85 = 10.625
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { priceGbp priceUsd } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.priceGbp" == "GBP 10.62"
jsonpath "$.data.book.priceUsd" == "USD 13.50"

# D06 @currency uses the currency's own decimals: yen has none
POST {{url}}
Content-Type: application/json
{"query": "{ books { priceJpy } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books[0].priceJpy" == "JPY 1942"
jsonpath "$.data.books[1].priceJpy" == "JPY 2025"
jsonpath "$.data.books[5].priceJpy" == "JPY 2"

# D07 @mask keeps the last 4 by default
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u1\") { email handle } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.email" == "********************.com"
jsonpath "$.data.user.handle" == "ada"

# D08 @mask(keep: 2), and null stays null
POST {{url}}
Content-Type: application/json
{"query": "{ a: user(id: \"u1\") { phone } b: user(id: \"u2\") { phone } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a.phone" == "**************58"
jsonpath "$.data.b.phone" == null
jsonpath "$.errors" not exists

# D09 a value no longer than keep is shown whole; shorter than the default is partly starred
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u3\") { phone email } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.phone" == "12"
jsonpath "$.data.user.email" == "**x.io"

# D10 @mask(keep: 0) stars everything
POST {{url}}
Content-Type: application/json
{"query": "{ user(id: \"u2\") { libraryCard } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.libraryCard" == "************"

# D11 stacked, converted first: "GBP 10.62" then masked
POST {{url}}
Content-Type: application/json
{"query": "{ experiment { currencyThenMask } }"}
HTTP 200
[Asserts]
jsonpath "$.data.experiment.currencyThenMask" == "*******62"

# D12 stacked, masked first: the conversion gets "***50", which is no amount: a field error, the rest survives
POST {{url}}
Content-Type: application/json
{"query": "{ experiment { maskThenCurrency currencyThenMask } }"}
HTTP 200
[Asserts]
jsonpath "$.data.experiment.maskThenCurrency" == null
jsonpath "$.data.experiment.currencyThenMask" == "*******62"
jsonpath "$.errors[0].path[1]" == "maskThenCurrency"
jsonpath "$.errors[0].message" contains "@currency"

# D13 the directives are part of the schema clients can introspect
POST {{url}}
Content-Type: application/json
{"query": "{ __schema { directives { name args { name defaultValue } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__schema.directives[*].name" contains "mask"
jsonpath "$..args[?(@.name == 'keep')].defaultValue" == "4"
jsonpath "$.data.__schema.directives[*].name" contains "currency"
