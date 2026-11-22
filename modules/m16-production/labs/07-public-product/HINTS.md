# Hints: 16.7 public product

1. Let the diff write your list. Each `BREAKING` line is one entry in the changelog, one section of the guide
   and one bullet of the announcement. Look in the 2027-01 schema for the replacement of each removed field: it
   is there.
2. A "before" query that fails on 2027-01 proves the change breaks it. Validate yours with
   `bin/course gql validate q.graphql --schema .../2027-01.graphqls`. For a new required argument, the "after"
   needs a variable of the new enum type, or a literal value.
3. Readers skim. Put the three changes first as a list. Then add a table of dates: announcement, release,
   reminder and retirement. Then say what happens to a request for a retired version. Shopify answers it with
   the oldest supported version; say what Shelfie does.
