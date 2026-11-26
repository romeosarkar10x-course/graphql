// What Apollo Client 4.2.12 sends for an operation with @defer: a local server records the Accept header and
// answers one multipart response in the deferSpec=20220824 format.
import http from "node:http";
import { createRequire } from "node:module";
const require = createRequire(process.env.WEB + "/package.json");
const { ApolloClient, InMemoryCache, HttpLink, gql } = require("@apollo/client");
const { Defer20220824Handler } = require("@apollo/client/incremental");
const seen = [];
const server = http.createServer((req, res) => {
  seen.push(req.headers.accept);
  res.writeHead(200, { "Content-Type": 'multipart/mixed; boundary="-"; deferSpec=20220824' });
  res.write('\r\n---\r\nContent-Type: application/json; charset=utf-8\r\n\r\n{"data":{"book":{"__typename":"Book","id":"b1","title":"Kindred"}},"hasNext":true}\r\n---');
  res.end('\r\nContent-Type: application/json; charset=utf-8\r\n\r\n{"incremental":[{"data":{"reviews":[]},"path":["book"]}],"hasNext":false}\r\n-----\r\n');
});
server.listen(0, async () => {
  const uri = `http://127.0.0.1:${server.address().port}/graphql`;
  const client = new ApolloClient({ link: new HttpLink({ uri }), cache: new InMemoryCache(), incrementalHandler: new Defer20220824Handler() });
  const plain = await client.query({ query: gql`query P { book { id title } }`, fetchPolicy: "no-cache" }).catch(e => e.message);
  const r = await client.query({ query: gql`query D { book { id title ... @defer { reviews { id } } } }`, fetchPolicy: "no-cache" });
  console.log("accept (plain):", seen[0]);
  console.log("accept (@defer):", seen[1]);
  console.log("result:", JSON.stringify(r.data));
  server.close();
});
