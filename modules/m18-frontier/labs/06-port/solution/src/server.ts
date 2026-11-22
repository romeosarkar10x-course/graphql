// Shelfie's reviews subgraph in TypeScript: the same contract as Module 15's Java one
// (modules/m15-federation/contract/subgraphs/src/main/resources/graphql/reviews/reviews.graphqls).
//   npm ci && npm run build && node dist/server.js [port]      (default 4003)
//   POST /graphql   GraphQL over HTTP (GraphQL Yoga)
//   POST /_reset    put the reviews back to the seed (tests call it between checks)
import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';
import SchemaBuilder from '@pothos/core';
import DirectivePlugin from '@pothos/plugin-directives';
import FederationPlugin from '@pothos/plugin-federation';
import { GraphQLError } from 'graphql';
import { createYoga } from 'graphql-yoga';

// ---- data: the contract's seed, shared by every port -------------------------------------------------------------
type Review = { id: string; bookId: string; authorId: string; rating: number; body: string };
type Seed = { users: { id: string; handle: string }[]; books: string[]; reviews: Review[] };

const seedFile = process.env.SHELFIE_SEED ?? new URL('../seed.json', import.meta.url);
const seed: Seed = JSON.parse(readFileSync(seedFile, 'utf8'));
const handles = new Map(seed.users.map((u) => [u.id, u.handle]));
const books = new Set(seed.books);

let reviews = new Map<string, Review>();
let nextReview = 0;
function resetReviews(): void {
  reviews = new Map(seed.reviews.map((r) => [r.id, { ...r }]));
  nextReview = seed.reviews.length + 1;
}
resetReviews();

const number = (id: string) => Number.parseInt(id.slice(1), 10);
/** Reviews in id order (r1, r2, ... r10), as the Java subgraph and the reference server list them. */
const inIdOrder = () => [...reviews.values()].sort((a, b) => number(a.id) - number(b.id));

// ---- schema: code-first with Pothos, federated with its federation plugin -----------------------------------------
type Context = { userId: string | null };
// Pothos makes every field nullable unless told otherwise; the contract is non-null by default (ID!, [Review!]!), so
// flip the default and mark the nullable fields (review, addReview, averageRating) one by one.
const builder = new SchemaBuilder<{ Context: Context; DefaultFieldNullability: false }>({
  plugins: [DirectivePlugin, FederationPlugin],
  defaultFieldNullability: false,
});

const BookRef = builder.externalRef('Book', builder.selection<{ id: string }>('id'), (book) =>
  books.has(book.id) ? { id: book.id } : null,
);
const UserRef = builder.externalRef('User', builder.selection<{ id: string }>('id'), (user) =>
  handles.has(user.id) ? { id: user.id } : null,
);
const ReviewRef = builder.objectRef<Review>('Review');

ReviewRef.implement({
  fields: (t) => ({
    id: t.exposeID('id'),
    rating: t.exposeInt('rating'),
    body: t.exposeString('body'),
    author: t.field({
      description: "Reviews keeps a copy of each author's handle, so it can answer that without accounts.",
      type: UserRef.provides<{ handle: string }>('handle'),
      resolve: (review) => ({ id: review.authorId, handle: handles.get(review.authorId) ?? '' }),
    }),
    book: t.field({ type: BookRef, resolve: (review) => ({ id: review.bookId }) }),
  }),
});
builder.asEntity(ReviewRef, {
  key: builder.selection<{ id: string }>('id'),
  resolveReference: (ref) => reviews.get(ref.id) ?? null,
});

const ofBook = (bookId: string) => inIdOrder().filter((r) => r.bookId === bookId);
BookRef.implement({
  fields: (t) => ({
    id: t.exposeID('id'),
    reviews: t.field({ description: 'Oldest first.', type: [ReviewRef], resolve: (book) => ofBook(book.id) }),
    reviewCount: t.int({ resolve: (book) => ofBook(book.id).length }),
    averageRating: t.float({
      description: 'Null without reviews.',
      nullable: true,
      resolve: (book) => {
        const rs = ofBook(book.id);
        if (rs.length === 0) return null;
        return Math.round((rs.reduce((sum, r) => sum + r.rating, 0) / rs.length) * 100) / 100;
      },
    }),
  }),
});

UserRef.implement({
  externalFields: (t) => ({ handle: t.string() }),
  fields: (t) => ({
    id: t.exposeID('id'),
    reviews: t.field({
      type: [ReviewRef],
      resolve: (user) => inIdOrder().filter((r) => r.authorId === user.id),
    }),
  }),
});

builder.queryType({
  fields: (t) => ({
    review: t.field({
      type: ReviewRef,
      nullable: true,
      args: { id: t.arg.id({ required: true }) },
      resolve: (_root, args) => reviews.get(String(args.id)) ?? null,
    }),
    latestReviews: t.field({
      description: 'Newest first.',
      type: [ReviewRef],
      args: { first: t.arg.int({ defaultValue: 5 }) },
      resolve: (_root, args) => inIdOrder().reverse().slice(0, Math.max(0, args.first ?? 5)),
    }),
  }),
});

builder.mutationType({
  fields: (t) => ({
    addReview: t.field({
      description: 'Signed in only (X-Shelfie-User); null and an error otherwise.',
      type: ReviewRef,
      nullable: true,
      args: {
        bookId: t.arg.id({ required: true }),
        rating: t.arg.int({ required: true }),
        body: t.arg.string({ required: true }),
      },
      resolve: (_root, args, ctx) => {
        // Same error as the Java subgraph: Spring's ErrorType.UNAUTHORIZED is extensions.classification.
        if (ctx.userId === null) throw new GraphQLError('sign in to review', { extensions: { classification: 'UNAUTHORIZED' } });
        const review: Review = {
          id: `r${nextReview++}`,
          bookId: String(args.bookId),
          authorId: ctx.userId,
          rating: args.rating,
          body: args.body,
        };
        reviews.set(review.id, review);
        return review;
      },
    }),
  }),
});

const schema = builder.toSubGraphSchema({
  // The course's composer (Module 15) knows Federation up to v2.9; Pothos defaults to v2.6.
  // The import list defaults to the directives the schema uses (@key, @external, @provides and, for externalRef
  // types, @extends).
  linkUrl: 'https://specs.apollo.dev/federation/v2.9',
});

// ---- HTTP: Yoga for /graphql, a plain handler for /_reset -------------------------------------------------------
const yoga = createYoga<{}, Context>({
  schema,
  graphiql: false,
  landingPage: false,
  // Unexpected errors stay masked; GraphQLErrors thrown on purpose (above) pass through with their extensions.
  context: ({ request }) => {
    const user = request.headers.get('x-shelfie-user');
    return { userId: user !== null && handles.has(user) ? user : null };
  },
});

const port = Number(process.argv[2] ?? process.env.PORT ?? 4003);
createServer((req, res) => {
  if (req.url === '/_reset' && req.method === 'POST') {
    resetReviews();
    res.writeHead(200, { 'content-type': 'application/json' }).end('{"reset":true}');
    return;
  }
  yoga(req, res);
}).listen(port, () => console.log(`reviews (TypeScript) on :${port}/graphql`));
