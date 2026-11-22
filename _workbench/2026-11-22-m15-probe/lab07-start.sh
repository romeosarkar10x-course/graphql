source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
D=$(mktemp -d); cp /course/modules/m15-federation/labs/07-operating/solution/states/1/reviews.graphqls $D/
sub_start reviews 4003 --spring.graphql.schema.locations=file:$D/ && echo "reviews started without Review.book" && gql http://localhost:4003/graphql '{ _service { sdl } }' | jq -r .data._service.sdl | grep -c "book: Book"
grep -i -m3 "warn\|unmapped\|skip" /tmp/sub-reviews.log | cut -c1-300
