cd /course/modules/m08-pagination-errors/labs/04-filter-sort/${1:-solution} && mvn -q package -DskipTests 2>&1 | tail; java -jar target/shelfie-filters.jar > /tmp/f.log 2>&1 & for i in $(seq 80); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep .5; done
q() { jq -n --arg q "$1" "{query:\$q}" | curl -s -H "Content-Type: application/json" -d @- localhost:4000/graphql; echo; }
q "{ books(first: 3) { items { id title } totalCount } }"
q "{ books(filter: {genres: [POETRY, MEMOIR], publishedFrom: 1990, publishedTo: 1999, minRating: 4.5}, orderBy: {field: RATING, direction: DESC}, first: 3) { items { id title genre published rating } totalCount } }"
q "{ books(filter: {search: \"dragon lisbon\"}, orderBy: {field: RELEVANCE}, first: 3) { items { id title } totalCount } }"
q "{ books(filter: {genres: []}) { items { id } totalCount } }"
q "{ books(filter: {publishedFrom: 2000, publishedTo: 1990}) { totalCount } }"
q "{ books(first: 0) { items { id } totalCount } }"
q "{ books(filter: {search: \"\\\"stolen map\\\" -accra\"}, first: 2) { items { id title } totalCount } }"
q "{ books(filter: {author: \"Mei Mensah\"}, orderBy: {field: PUBLISHED, direction: DESC}, first: 2) { items { id published } } }"
q "{ books(filter: {search: \"x') or true or (book_matches(title, blurb, 'x\"}) { totalCount } }"
grep -E "^Hibernate" /tmp/f.log | tail -6; grep -E "Exception" /tmp/f.log | head -3; kill %1
