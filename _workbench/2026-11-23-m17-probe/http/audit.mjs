// Run graphql-http's server audit against a URL; print one line per audit and a JSON summary.
import { auditServer } from 'graphql-http';
const url = process.argv[2] ?? 'http://localhost:4000/graphql';
const results = await auditServer({ url });
let ok = 0, warn = 0, error = 0;
for (const r of results) {
  if (r.status === 'ok') ok++; else if (r.status === 'warn') warn++; else error++;
  console.log(`${r.status.padEnd(5)} ${r.id} ${r.name}${r.status === 'ok' ? '' : '  -- ' + r.reason}`);
}
console.log(JSON.stringify({ total: results.length, ok, warn, error }));
