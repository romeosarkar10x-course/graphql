// graphql-http's server audit against a URL: one line per audit ("ok|warn|notice|error ID name -- reason"),
// then a JSON summary. Used by lab 17.8: bin/course node node audit.mjs http://localhost:4000/graphql
import { auditServer } from 'graphql-http';
const url = process.argv[2] ?? 'http://localhost:4000/graphql';
const results = await auditServer({ url });
const count = { ok: 0, warn: 0, notice: 0, error: 0 };
for (const r of results) {
  count[r.status] = (count[r.status] ?? 0) + 1;
  console.log(`${r.status.padEnd(6)} ${r.id} ${r.name}${r.status === 'ok' ? '' : '  -- ' + r.reason}`);
}
console.log(JSON.stringify({ total: results.length, ...count }));
