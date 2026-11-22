// Shelfie's reviews subgraph in TypeScript: GraphQL Yoga + Pothos (code-first).
//   npm ci && npm run build && node dist/server.js [port]      (default 4003); serve.sh does all three
// The contract is Module 15's Java subgraph:
//   modules/m15-federation/contract/subgraphs/src/main/resources/graphql/reviews/reviews.graphqls
// This starter serves one field and is not federated yet.
import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';
import SchemaBuilder from '@pothos/core';
import { createYoga } from 'graphql-yoga';

type Review = { id: string; bookId: string; authorId: string; rating: number; body: string };
type Seed = { users: { id: string; handle: string }[]; books: string[]; reviews: Review[] };

const seed: Seed = JSON.parse(readFileSync(new URL('../seed.json', import.meta.url), 'utf8'));
const reviews = new Map(seed.reviews.map((r) => [r.id, { ...r }]));

const builder = new SchemaBuilder({});

const ReviewRef = builder.objectRef<Review>('Review').implement({
  fields: (t) => ({
    id: t.exposeID('id'),
    rating: t.exposeInt('rating'),
    body: t.exposeString('body'),
  }),
});

builder.queryType({
  fields: (t) => ({
    latestReviews: t.field({
      type: [ReviewRef],
      args: { first: t.arg.int({ defaultValue: 5 }) },
      resolve: (_root, args) => [...reviews.values()].reverse().slice(0, args.first ?? 5),
    }),
  }),
});

const yoga = createYoga({ schema: builder.toSchema() });
const port = Number(process.argv[2] ?? process.env.PORT ?? 4003);
createServer(yoga).listen(port, () => console.log(`reviews (TypeScript) on :${port}/graphql`));
