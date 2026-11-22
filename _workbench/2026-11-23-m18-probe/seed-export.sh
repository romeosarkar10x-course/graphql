#!/usr/bin/env bash
# Export the reviews subgraph's seed from the running Java subgraphs (contract/port/seed.json).
source /course/tools/testlib.sh; source /course/modules/m15-federation/contract/fed.sh
sub_start accounts 4001 && sub_start catalog 4002 && sub_start reviews 4003 || exit 1
U=$(gql http://localhost:4001/graphql '{ users { id handle } }')
B=$(gql http://localhost:4002/graphql '{ books(first: 100) { id } }')
R=$(gql http://localhost:4003/graphql '{ latestReviews(first: 100) { id rating body author { id } book { id } } }')
jq -n --argjson u "$U" --argjson b "$B" --argjson r "$R" '{
  "_": "Seed of the reviews subgraph, exported from Module 15'"'"'s Java subgraphs. Reviews in id order; new ones get r16, r17, ...",
  users: $u.data.users, books: [$b.data.books[].id],
  reviews: ($r.data.latestReviews | reverse | map({id, bookId: .book.id, authorId: .author.id, rating, body}))}'
