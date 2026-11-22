# Generates modules/m12-clients/contract/cache/scenarios/*.json. Kept for reference: the JSON files are the spec.
import json, os, sys
out = sys.argv[1]
os.makedirs(out, exist_ok=True)
S = {}
def sc(name, description, ops, policies=None):
    S[name] = {"description": description, "policies": policies or {}, "ops": ops}
def W(q, data, v=None): return {"op": "write", "query": q, "variables": v or {}, "data": data}
def R(q, expect, v=None): return {"op": "read", "query": q, "variables": v or {}, "expect": expect}
def X(expect): return {"op": "extract", "expect": expect}
def ref(i): return {"__ref": i}

LHD = "The Left Hand of Darkness"
b1 = {"__typename": "Book", "id": "b1", "title": LHD}
Q_B1 = '{ book(id: "b1") { __typename id title } }'

sc("01-write-read-hit", "Write a response, read the same document back.", [
    W(Q_B1, {"book": b1}),
    R(Q_B1, {"book": b1}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1")}, "Book:b1": b1}),
])

sc("02-miss-unwritten", "Nothing written is a miss; so is a different argument.", [
    R('{ book(id: "b2") { __typename id title } }', None),
    W(Q_B1, {"book": b1}),
    R('{ book(id: "b2") { __typename id title } }', None),
    R(Q_B1, {"book": b1}),
])

sc("03-miss-missing-field", "Asking for one field the cache never saw is a miss; a subset is a hit.", [
    W(Q_B1, {"book": b1}),
    R('{ book(id: "b1") { __typename id title pageCount } }', None),
    R('{ book(id: "b1") { title } }', {"book": {"title": LHD}}),
])

anniv = LHD + " (50th anniversary)"
sc("04-entity-shared-across-queries", "Two queries show book b1; a mutation renames it once; both reads see the new title (the lecture's experiment).", [
    W('{ club(id: "c1") { __typename id name currentBook { __typename id title } } }',
      {"club": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers", "currentBook": b1}}),
    W('{ books(first: 2) { __typename id title } }',
      {"books": [b1, {"__typename": "Book", "id": "b2", "title": "Piranesi"}]}),
    W('mutation { renameBook(id: "b1", title: "%s") { __typename id title } }' % anniv,
      {"renameBook": {"__typename": "Book", "id": "b1", "title": anniv}}),
    R('{ club(id: "c1") { __typename id name currentBook { __typename id title } } }',
      {"club": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers", "currentBook": {"__typename": "Book", "id": "b1", "title": anniv}}}),
    R('{ books(first: 2) { __typename id title } }',
      {"books": [{"__typename": "Book", "id": "b1", "title": anniv}, {"__typename": "Book", "id": "b2", "title": "Piranesi"}]}),
    X({"ROOT_QUERY": {'club({"id":"c1"})': ref("Club:c1"), 'books({"first":2})': [ref("Book:b1"), ref("Book:b2")]},
       "Club:c1": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers", "currentBook": ref("Book:b1")},
       "Book:b1": {"__typename": "Book", "id": "b1", "title": anniv},
       "Book:b2": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
])

sc("05-arguments-and-variables", "Field keys: arguments sorted, variables replaced by values, absent variables left out.", [
    W('query ($id: ID!) { book(id: $id) { __typename id title } }', {"book": b1}, {"id": "b1"}),
    R(Q_B1, {"book": b1}),
    R('query ($id: ID!) { book(id: $id) { __typename id title } }', None, {"id": "b2"}),
    W('{ books(titleContains: "the", first: 2) { __typename id } }',
      {"books": [{"__typename": "Book", "id": "b1"}, {"__typename": "Book", "id": "b3"}]}),
    W('query ($t: String) { books(titleContains: $t, first: 1) { __typename id } }', {"books": [{"__typename": "Book", "id": "b1"}]}),
    R('query ($n: Int) { books(first: $n, titleContains: "the") { id } }', {"books": [{"id": "b1"}, {"id": "b3"}]}, {"n": 2}),
    R('{ books(first: 1) { id } }', {"books": [{"id": "b1"}]}),
    R('{ books(first: 3) { id } }', None),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1"),
                      'books({"first":2,"titleContains":"the"})': [ref("Book:b1"), ref("Book:b3")],
                      'books({"first":1})': [ref("Book:b1")]},
       "Book:b1": b1, "Book:b3": {"__typename": "Book", "id": "b3"}}),
])

sc("06-aliases", "Aliases are response keys, not storage keys.", [
    W('{ a: book(id: "b1") { __typename id t: title } b: book(id: "b2") { __typename id title } }',
      {"a": {"__typename": "Book", "id": "b1", "t": LHD}, "b": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
    R('{ x: book(id: "b1") { __typename id title } }', {"x": b1}),
    R('{ book(id: "b2") { name: title } }', {"book": {"name": "Piranesi"}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1"), 'book({"id":"b2"})': ref("Book:b2")},
       "Book:b1": b1, "Book:b2": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
])

hugo = {"__typename": "User", "id": "u5", "name": "Hugo Silva"}
sc("07-lists-of-entities", "A list of entities becomes a list of references; an author shared by two reviews is one record.", [
    W('{ book(id: "b1") { __typename id title reviews(last: 2) { __typename id rating author { __typename id name } } } }',
      {"book": {"__typename": "Book", "id": "b1", "title": LHD, "reviews": [
          {"__typename": "Review", "id": "v1", "rating": 4, "author": hugo},
          {"__typename": "Review", "id": "v7", "rating": 5, "author": hugo}]}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1")},
       "Book:b1": {"__typename": "Book", "id": "b1", "title": LHD, 'reviews({"last":2})': [ref("Review:v1"), ref("Review:v7")]},
       "Review:v1": {"__typename": "Review", "id": "v1", "rating": 4, "author": ref("User:u5")},
       "Review:v7": {"__typename": "Review", "id": "v7", "rating": 5, "author": ref("User:u5")},
       "User:u5": hugo}),
    R('{ book(id: "b1") { reviews(last: 2) { rating author { name } } } }',
      {"book": {"reviews": [{"rating": 4, "author": {"name": "Hugo Silva"}}, {"rating": 5, "author": {"name": "Hugo Silva"}}]}}),
    R('{ book(id: "b1") { reviews(last: 5) { rating } } }', None),
])

sc("08-embedded-objects", "Objects without an id live inside their parent and are replaced wholesale.", [
    W('{ book(id: "b3") { __typename id stats { pages ratingCount } } settings { theme } }',
      {"book": {"__typename": "Book", "id": "b3", "stats": {"pages": 502, "ratingCount": 2}}, "settings": {"theme": "dark"}}),
    X({"ROOT_QUERY": {'book({"id":"b3"})': ref("Book:b3"), "settings": {"theme": "dark"}},
       "Book:b3": {"__typename": "Book", "id": "b3", "stats": {"pages": 502, "ratingCount": 2}}}),
    R('{ book(id: "b3") { stats { ratingCount } } settings { theme } }', {"book": {"stats": {"ratingCount": 2}}, "settings": {"theme": "dark"}}),
    W('{ book(id: "b3") { __typename id stats { pages } } }', {"book": {"__typename": "Book", "id": "b3", "stats": {"pages": 512}}}),
    R('{ book(id: "b3") { stats { pages } } }', {"book": {"stats": {"pages": 512}}}),
    R('{ book(id: "b3") { stats { pages ratingCount } } }', None),
])

sc("09-nulls", "A stored null is a value: reading it is a hit.", [
    W('{ club(id: "c3") { __typename id name currentBook { __typename id title } } }',
      {"club": {"__typename": "Club", "id": "c3", "name": "Sunday Sci-Fi", "currentBook": None}}),
    R('{ club(id: "c3") { name currentBook { title } } }', {"club": {"name": "Sunday Sci-Fi", "currentBook": None}}),
    W('{ book(id: "nope") { __typename id title } }', {"book": None}),
    R('{ book(id: "nope") { title } }', {"book": None}),
    X({"ROOT_QUERY": {'club({"id":"c3"})': ref("Club:c3"), 'book({"id":"nope"})': None},
       "Club:c3": {"__typename": "Club", "id": "c3", "name": "Sunday Sci-Fi", "currentBook": None}}),
])

sc("10-merge-fields-across-writes", "Writes to one entity add fields; a later write overwrites only what it carries.", [
    W(Q_B1, {"book": b1}),
    W('{ books(first: 1) { __typename id authors publishedYear } }',
      {"books": [{"__typename": "Book", "id": "b1", "authors": ["Ursula K. Le Guin"], "publishedYear": 1969}]}),
    R('{ book(id: "b1") { __typename id title authors publishedYear } }',
      {"book": {"__typename": "Book", "id": "b1", "title": LHD, "authors": ["Ursula K. Le Guin"], "publishedYear": 1969}}),
    W('{ book(id: "b1") { __typename id title } }', {"book": {"__typename": "Book", "id": "b1", "title": "LHoD"}}),
    R('{ book(id: "b1") { title publishedYear } }', {"book": {"title": "LHoD", "publishedYear": 1969}}),
])

# ---------- mini-project ----------
sc("11-fragments-named", "Named fragments on the object's own type apply; a fragment on another type does not.", [
    W('query { book(id: "b1") { ...Card } } fragment Card on Book { __typename id title authors }',
      {"book": {"__typename": "Book", "id": "b1", "title": LHD, "authors": ["Ursula K. Le Guin"]}}),
    R('{ book(id: "b1") { __typename id title } }', {"book": b1}),
    R('query { book(id: "b1") { ...Names } } fragment Names on Book { authors t: title }', {"book": {"authors": ["Ursula K. Le Guin"], "t": LHD}}),
    R('query { book(id: "b1") { title ...U } } fragment U on User { handle }', {"book": {"title": LHD}}),
])

search_data = {"search": [
    {"__typename": "Book", "id": "b1", "title": LHD},
    {"__typename": "Club", "id": "c1", "name": "The Midnight Readers"},
    {"__typename": "User", "id": "u3", "handle": "theo"}]}
Q_SEARCH = '{ search(text: "the") { __typename ... on Book { id title } ... on Club { id name } ... on User { id handle } } }'
sc("12-inline-fragments-union", "Union members are told apart by __typename; possibleTypes lets a fragment on an interface apply.", [
    W(Q_SEARCH, search_data),
    R(Q_SEARCH, search_data),
    R('{ search(text: "the") { __typename ... on Node { id } } }', {"search": [
        {"__typename": "Book", "id": "b1"}, {"__typename": "Club", "id": "c1"}, {"__typename": "User", "id": "u3"}]}),
    R('{ search(text: "the") { ... on Book { title } } }', {"search": [{"title": LHD}, {}, {}]}),
    X({"ROOT_QUERY": {'search({"text":"the"})': [ref("Book:b1"), ref("Club:c1"), ref("User:u3")]},
       "Book:b1": b1, "Club:c1": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers"},
       "User:u3": {"__typename": "User", "id": "u3", "handle": "theo"}}),
], {"possibleTypes": {"Node": ["Book", "Club", "User"]}})

Q_INC = 'query ($full: Boolean!) { book(id: "b1") { __typename id title synopsis @include(if: $full) pageCount @skip(if: true) } }'
syn = "An envoy on a planet whose people have no fixed sex."
sc("13-include-skip", "@include and @skip decide which fields are written and which a read needs.", [
    W(Q_INC, {"book": b1}, {"full": False}),
    R(Q_INC, {"book": b1}, {"full": False}),
    R(Q_INC, None, {"full": True}),
    W(Q_INC, {"book": dict(b1, synopsis=syn)}, {"full": True}),
    R(Q_INC, {"book": dict(b1, synopsis=syn)}, {"full": True}),
])

ed1 = {"__typename": "Edition", "isbn": "9780441478125", "format": "paperback"}
ed2 = {"__typename": "Edition", "isbn": "9780143111597", "format": "hardcover"}
sc("14-key-fields", "keyFields: an entity without an id field is identified by other fields.", [
    W('{ book(id: "b1") { __typename id editions { __typename isbn format } } }', {"book": {"__typename": "Book", "id": "b1", "editions": [ed1, ed2]}}),
    W('{ edition(isbn: "9780441478125") { __typename isbn pages } }', {"edition": {"__typename": "Edition", "isbn": "9780441478125", "pages": 304}}),
    R('{ book(id: "b1") { editions { format pages } } }', None),
    R('{ book(id: "b1") { editions { isbn format } } }', {"book": {"editions": [{"isbn": "9780441478125", "format": "paperback"}, {"isbn": "9780143111597", "format": "hardcover"}]}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1"), 'edition({"isbn":"9780441478125"})': ref('Edition:{"isbn":"9780441478125"}')},
       "Book:b1": {"__typename": "Book", "id": "b1", "editions": [ref('Edition:{"isbn":"9780441478125"}'), ref('Edition:{"isbn":"9780143111597"}')]},
       'Edition:{"isbn":"9780441478125"}': dict(ed1, pages=304),
       'Edition:{"isbn":"9780143111597"}': ed2}),
], {"types": {"Edition": {"keyFields": ["isbn"]}}})

sc("15-key-args", "keyArgs: only the listed arguments are part of the storage key; without merge the last write wins.", [
    W('{ books(titleContains: "the", first: 2) { __typename id } }', {"books": [{"__typename": "Book", "id": "b1"}, {"__typename": "Book", "id": "b3"}]}),
    R('{ books(titleContains: "the", first: 5) { id } }', {"books": [{"id": "b1"}, {"id": "b3"}]}),
    W('{ books(titleContains: "the", first: 1) { __typename id } }', {"books": [{"__typename": "Book", "id": "b3"}]}),
    R('{ books(titleContains: "the", first: 2) { id } }', {"books": [{"id": "b3"}]}),
    R('{ books(titleContains: "rose", first: 2) { id } }', None),
    X({"ROOT_QUERY": {'books({"titleContains":"the"})': [ref("Book:b3")]},
       "Book:b1": {"__typename": "Book", "id": "b1"}, "Book:b3": {"__typename": "Book", "id": "b3"}}),
], {"fields": {"Query.books": {"keyArgs": ["titleContains"]}}})

def rv(i, body): return {"__typename": "Review", "id": i, "body": body}
sc("16-pagination-merge", "merge: append concatenates pages under one key; a read returns the merged list.", [
    W('{ feed(first: 2) { __typename id body } }', {"feed": [rv("v1", "one"), rv("v2", "two")]}),
    W('{ feed(first: 2, after: "v2") { __typename id body } }', {"feed": [rv("v3", "three"), rv("v4", "four")]}),
    R('{ feed(first: 2) { id body } }', {"feed": [{"id": "v1", "body": "one"}, {"id": "v2", "body": "two"}, {"id": "v3", "body": "three"}, {"id": "v4", "body": "four"}]}),
    W('{ book(id: "b1") { __typename id reviews(last: 1) { __typename id } } }', {"book": {"__typename": "Book", "id": "b1", "reviews": [{"__typename": "Review", "id": "v9"}]}}),
    W('{ book(id: "b1") { __typename id reviews(last: 2) { __typename id } } }', {"book": {"__typename": "Book", "id": "b1", "reviews": [{"__typename": "Review", "id": "v8"}, {"__typename": "Review", "id": "v9"}]}}),
    X({"ROOT_QUERY": {"feed": [ref("Review:v1"), ref("Review:v2"), ref("Review:v3"), ref("Review:v4")], 'book({"id":"b1"})': ref("Book:b1")},
       "Review:v1": rv("v1", "one"), "Review:v2": rv("v2", "two"), "Review:v3": rv("v3", "three"), "Review:v4": rv("v4", "four"),
       "Book:b1": {"__typename": "Book", "id": "b1", 'reviews({"last":1})': [ref("Review:v9")], 'reviews({"last":2})': [ref("Review:v8"), ref("Review:v9")]},
       "Review:v8": {"__typename": "Review", "id": "v8"}, "Review:v9": {"__typename": "Review", "id": "v9"}}),
], {"fields": {"Query.feed": {"keyArgs": [], "merge": "append"}}})

sc("17-read-redirect", "read: toReference makes book(id:) a hit for a book cached by a list query.", [
    W('{ books(first: 2) { __typename id title } }', {"books": [b1, {"__typename": "Book", "id": "b2", "title": "Piranesi"}]}),
    R('{ book(id: "b2") { __typename id title } }', {"book": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
    R('query ($id: ID!) { book(id: $id) { title } }', {"book": {"title": LHD}}, {"id": "b1"}),
    R('{ book(id: "b9") { title } }', None),
    R('{ book(id: "b2") { pageCount } }', None),
    X({"ROOT_QUERY": {'books({"first":2})': [ref("Book:b1"), ref("Book:b2")]},
       "Book:b1": b1, "Book:b2": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
], {"fields": {"Query.book": {"read": "toReference", "typename": "Book", "idArg": "id"}}})

Q_REV = '{ book(id: "b1") { __typename id averageRating reviews(last: 5) { __typename id rating } } }'
MUT_REV = 'mutation ($input: CreateReviewInput!) { createReview(input: $input) { review { __typename id rating book { __typename id averageRating } } } }'
mut_vars = {"input": {"bookId": "b1", "rating": 5, "body": "Better on a second read."}}
mut_data = {"createReview": {"review": {"__typename": "Review", "id": "v16", "rating": 5, "book": {"__typename": "Book", "id": "b1", "averageRating": 4.33}}}}
before = {"book": {"__typename": "Book", "id": "b1", "averageRating": 4.0, "reviews": [{"__typename": "Review", "id": "v1", "rating": 4}]}}
sc("18-mutation-updates-entities-not-lists", "A mutation result updates the entities it returns; lists it does not mention stay stale.", [
    W(Q_REV, before),
    W(MUT_REV, mut_data, mut_vars),
    R(Q_REV, {"book": {"__typename": "Book", "id": "b1", "averageRating": 4.33, "reviews": [{"__typename": "Review", "id": "v1", "rating": 4}]}}),
    R('{ book(id: "b1") { reviews(last: 5) { id } } }', {"book": {"reviews": [{"id": "v1"}]}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1")},
       "Book:b1": {"__typename": "Book", "id": "b1", "averageRating": 4.33, 'reviews({"last":5})': [ref("Review:v1")]},
       "Review:v1": {"__typename": "Review", "id": "v1", "rating": 4},
       "Review:v16": {"__typename": "Review", "id": "v16", "rating": 5, "book": ref("Book:b1")}}),
])

sc("19-update-list-after-mutation", "The app's update step: read the list, add the new review, write it back. Every query on that list sees it.", [
    W(Q_REV, before),
    W(MUT_REV, mut_data, mut_vars),
    W('{ book(id: "b1") { __typename id reviews(last: 5) { __typename id } } }',
      {"book": {"__typename": "Book", "id": "b1", "reviews": [{"__typename": "Review", "id": "v16"}, {"__typename": "Review", "id": "v1"}]}}),
    R(Q_REV, {"book": {"__typename": "Book", "id": "b1", "averageRating": 4.33, "reviews": [
        {"__typename": "Review", "id": "v16", "rating": 5}, {"__typename": "Review", "id": "v1", "rating": 4}]}}),
])

Q_CLUB = '{ club(id: "c1") { __typename id name currentBook { __typename id title } } }'
club_data = {"club": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers", "currentBook": b1}}
sc("20-evict-entity", "Evicting an entity makes every read through it a miss; other reads still hit.", [
    W(Q_CLUB, club_data),
    {"op": "evict", "id": "Book:b1"},
    R(Q_CLUB, None),
    R('{ club(id: "c1") { name } }', {"club": {"name": "The Midnight Readers"}}),
    X({"ROOT_QUERY": {'club({"id":"c1"})': ref("Club:c1")},
       "Club:c1": {"__typename": "Club", "id": "c1", "name": "The Midnight Readers", "currentBook": ref("Book:b1")}}),
])

sc("21-evict-field", "Evicting one field: the record stays, only reads of that field miss.", [
    W(Q_B1, {"book": b1}),
    W('{ books(first: 1) { __typename id title } }', {"books": [b1]}),
    {"op": "evict", "id": "ROOT_QUERY", "field": 'book({"id":"b1"})'},
    R(Q_B1, None),
    R('{ books(first: 1) { title } }', {"books": [{"title": LHD}]}),
    {"op": "evict", "id": "Book:b1", "field": "title"},
    R('{ books(first: 1) { title } }', None),
    R('{ books(first: 1) { id } }', {"books": [{"id": "b1"}]}),
])

sc("22-gc", "gc removes what ROOT_QUERY can no longer reach, transitively.", [
    W(Q_CLUB, club_data),
    W('{ book(id: "b2") { __typename id title reviews(last: 1) { __typename id author { __typename id name } } } }',
      {"book": {"__typename": "Book", "id": "b2", "title": "Piranesi", "reviews": [{"__typename": "Review", "id": "v3", "author": hugo}]}}),
    {"op": "gc", "expect": []},
    {"op": "evict", "id": "ROOT_QUERY", "field": 'book({"id":"b2"})'},
    {"op": "gc", "expect": ["Book:b2", "Review:v3", "User:u5"]},
    {"op": "gc", "expect": []},
    R(Q_CLUB, club_data),
])

Q_TITLE = '{ book(id: "b1") { title } }'
REN = 'mutation { renameBook(id: "b1", title: "Working title") { __typename id title } }'
def ren(t): return {"renameBook": {"__typename": "Book", "id": "b1", "title": t}}
sc("23-optimistic-layer", "An optimistic write is visible to reads at once, absent from the base store, and gone when removed.", [
    W(Q_B1, {"book": b1}),
    {"op": "writeOptimistic", "layer": "rename-1", "query": REN, "variables": {}, "data": ren("Working title")},
    R(Q_TITLE, {"book": {"title": "Working title"}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1")}, "Book:b1": b1}),
    {"op": "removeOptimistic", "layer": "rename-1"},
    R(Q_TITLE, {"book": {"title": LHD}}),
])

sc("24-optimistic-rebase", "Layers stack in order; the server's answer lands in the base under a layer and shows once the layer goes.", [
    W(Q_B1, {"book": b1}),
    {"op": "writeOptimistic", "layer": "a", "query": REN, "variables": {}, "data": ren("Draft A")},
    {"op": "writeOptimistic", "layer": "b", "query": REN, "variables": {}, "data": ren("Draft B")},
    R(Q_TITLE, {"book": {"title": "Draft B"}}),
    W(REN, ren("Final title")),
    R(Q_TITLE, {"book": {"title": "Draft B"}}),
    {"op": "removeOptimistic", "layer": "b"},
    R(Q_TITLE, {"book": {"title": "Draft A"}}),
    {"op": "removeOptimistic", "layer": "a"},
    R(Q_TITLE, {"book": {"title": "Final title"}}),
    X({"ROOT_QUERY": {'book({"id":"b1"})': ref("Book:b1")}, "Book:b1": {"__typename": "Book", "id": "b1", "title": "Final title"}}),
])

sc("25-watch", "A watch hears about changes to its own result only, and nothing after it is cancelled.", [
    {"op": "watch", "name": "w", "query": Q_TITLE, "variables": {}},
    {"op": "notified", "name": "w", "count": 0},
    W(Q_B1, {"book": b1}),
    {"op": "notified", "name": "w", "count": 1, "last": {"book": {"title": LHD}}},
    W(Q_B1, {"book": b1}),
    W('{ book(id: "b2") { __typename id title } }', {"book": {"__typename": "Book", "id": "b2", "title": "Piranesi"}}),
    {"op": "notified", "name": "w", "count": 1},
    {"op": "writeOptimistic", "layer": "l", "query": REN, "variables": {}, "data": ren("Working title")},
    {"op": "notified", "name": "w", "count": 2, "last": {"book": {"title": "Working title"}}},
    {"op": "removeOptimistic", "layer": "l"},
    {"op": "notified", "name": "w", "count": 3, "last": {"book": {"title": LHD}}},
    {"op": "evict", "id": "Book:b1"},
    {"op": "notified", "name": "w", "count": 4, "last": None},
    {"op": "unwatch", "name": "w"},
    W(Q_B1, {"book": b1}),
    {"op": "notified", "name": "w", "count": 4},
])

for name, body in S.items():
    with open(os.path.join(out, name + ".json"), "w") as f:
        json.dump(body, f, indent=2, ensure_ascii=False)
        f.write("\n")
print(len(S), "scenarios")
