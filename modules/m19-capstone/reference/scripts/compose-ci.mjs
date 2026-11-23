// A copy of the course composer (modules/m15-federation/contract/compose/compose.mjs) for CI, outside the course image.
// Compose a supergraph from a Rover-style supergraph.yaml (the subset: subgraphs.<name>.routing_url and
// subgraphs.<name>.schema.file), with The Guild's MIT-licensed composition library.
// usage: node compose.mjs supergraph.yaml [out.graphql] [api.graphql]
//   writes the supergraph (and the API schema clients see); exit 1 and errors (with codes) on stderr on failure
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { parse } from 'graphql';
import { composeServices } from '@theguild/federation-composition';

const [config, out, api] = process.argv.slice(2);
if (!config) { console.error('usage: node compose.mjs supergraph.yaml [out.graphql]'); process.exit(2); }
const base = dirname(resolve(config));
const services = [];
let current = null;
for (const raw of readFileSync(config, 'utf8').split('\n')) {
  const line = raw.replace(/\s+#.*$/, '');
  let m;
  if ((m = line.match(/^ {2}([A-Za-z0-9_-]+):\s*$/))) services.push(current = { name: m[1] });
  else if (current && (m = line.match(/^\s+routing_url:\s*(\S+)/))) current.url = m[1];
  else if (current && (m = line.match(/^\s+file:\s*(\S+)/))) current.file = m[1];
}
if (services.length === 0) { console.error(`${config}: no subgraphs found`); process.exit(2); }
for (const s of services) {
  if (!s.url || !s.file) { console.error(`${config}: subgraph ${s.name} needs routing_url and schema.file`); process.exit(2); }
  try { s.typeDefs = parse(readFileSync(resolve(base, s.file), 'utf8')); }
  catch (e) { console.error(`[${s.name}] ${e.message}`); process.exit(1); }
}
const result = composeServices(services.map(({ name, url, typeDefs }) => ({ name, url, typeDefs })));
if (result.errors) {
  for (const e of result.errors) console.error(`${e.extensions?.code ?? 'ERROR'}: ${e.message}`);
  process.exit(1);
}
if (out) writeFileSync(out, result.supergraphSdl); else process.stdout.write(result.supergraphSdl);
if (api) writeFileSync(api, result.publicSdl);
