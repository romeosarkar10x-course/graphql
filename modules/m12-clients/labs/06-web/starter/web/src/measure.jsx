// Renders the same club card with three client setups in jsdom, adds one review through each client's own
// mutation API, and counts how often each component rendered because of it.
//   SHELFIE=http://shelfie-ref:8080 node dist/measure.js
import { JSDOM } from "jsdom";

const dom = new JSDOM("<!doctype html><div id=root></div>");
globalThis.window = dom.window;
globalThis.document = dom.window.document;
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

const SHELFIE = process.env.SHELFIE ?? "http://shelfie-ref:8080";
const URL = `${SHELFIE}/graphql`;
const { act, Suspense } = await import("react");
const { createRoot } = await import("react-dom/client");
const { ApolloClient, InMemoryCache, HttpLink, gql } = await import("@apollo/client");
const { ApolloProvider } = await import("@apollo/client/react");
const { Environment, Network, RecordSource, Store } = (await import("relay-runtime")).default;
const { RelayEnvironmentProvider } = (await import("react-relay")).default;
const { renders, resetRenders } = await import("./renders.js");

let requests = 0;
const countingFetch = (url, init) => { requests++; return fetch(url, init); };
const input = { bookId: "b1", rating: 1, body: "Lab 12.6: one review, three clients." };
const ADD = gql`
  mutation AddReview($input: CreateReviewInput!) {
    createReview(input: $input) { review { id book { id averageRating } } userErrors { code } }
  }
`;

async function waitFor(container, text) {
  for (let i = 0; i < 100; i++) {
    if (container.textContent.includes(text)) return;
    await act(() => new Promise((r) => setTimeout(r, 20)));
  }
  throw new Error(`never saw "${text}"; page: ${container.textContent}`);
}

async function measure(name, element, mutate) {
  await fetch(`${SHELFIE}/_course/reset-data`, { method: "POST" });
  requests = 0;
  resetRenders();
  const container = document.createElement("div");
  document.body.appendChild(container);
  const root = createRoot(container);
  await act(async () => root.render(element));
  await waitFor(container, "The Midnight Readers");
  const initial = Object.fromEntries(renders);
  const before = container.querySelector(".book").textContent;
  resetRenders();
  await act(async () => { await mutate(); });
  await waitFor(container, ": 3.67");
  const after = Object.fromEntries(renders);
  const shown = container.querySelector(".book").textContent;
  await act(async () => root.unmount());
  container.remove();
  return { name, initial, after, before, shown, requests };
}

function apollo(dataMasking) {
  return new ApolloClient({
    cache: new InMemoryCache(),
    dataMasking,
    link: new HttpLink({ uri: URL, fetch: countingFetch, headers: { "X-Shelfie-User": "u11" } }),
  });
}

function relay() {
  return new Environment({
    network: Network.create(async (operation, variables) => {
      const r = await countingFetch(URL, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Shelfie-User": "u11" },
        body: JSON.stringify({ query: operation.text, variables }),
      });
      return r.json();
    }),
    store: new Store(new RecordSource()),
  });
}

// A variant whose files are not written yet is skipped.
async function load(path) {
  try {
    return await import(path);
  } catch (e) {
    if (e.code === "ERR_MODULE_NOT_FOUND") return null;
    throw e;
  }
}

const results = [];
for (const [name, path, masking] of [
  ["Apollo Client, props", "./apollo/ClubCard.js", false],
  ["Apollo Client, props + React.memo", "./memo/ClubCard.js", false],
  ["Apollo Client, data masking", "./masked/ClubCard.js", true],
]) {
  const m = await load(path);
  if (!m) { console.log(`${name}: not written yet (${path.replace("./", "src/").replace(".js", ".jsx")})`); continue; }
  const client = apollo(masking);
  results.push(await measure(name,
    <ApolloProvider client={client}><m.ClubCard id="c1" /></ApolloProvider>,
    () => client.mutate({ mutation: ADD, variables: { input } })));
}
{
  const card = await load("./relay/ClubCard.js");
  const mutation = await load("./relay/addReview.js");
  if (!card || !mutation) console.log("Relay: not written yet (src/relay/ClubCard.jsx and src/relay/addReview.js)");
  else {
    const env = relay();
    results.push(await measure("Relay",
      <RelayEnvironmentProvider environment={env}><Suspense fallback={<p>loading</p>}><card.ClubCard id="c1" /></Suspense></RelayEnvironmentProvider>,
      () => mutation.addReview(env, input)));
  }
}
await fetch(`${SHELFIE}/_course/reset-data`, { method: "POST" });

const fmt = (r) => ["ClubCard", "BookPanel", "MemberRow"].map((c) => `${c} ${r[c] ?? 0}`).join(", ");
for (const r of results) {
  console.log(`${r.name}`);
  console.log(`  first load:     ${fmt(r.initial)}`);
  console.log(`  after addReview: ${fmt(r.after)}   ("${r.before}" -> "${r.shown}", ${r.requests} requests)`);
}
