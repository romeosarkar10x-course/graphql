// Lab 13.7's check of graphql.config.yml, run from the project directory with the project's own node_modules:
//   node /course/modules/m13-testing/contract/tooling-check.mjs
// Loads the config the way IDE plugins and graphql-eslint do (graphql-config), builds the schema from it,
// finds the documents and validates each against the schema. Prints one line per finding; exit 1 on any.
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const here = createRequire(process.cwd() + '/');
const load = async (name) => import(pathToFileURL(here.resolve(name)).href);
const { loadConfig } = await load('graphql-config');
const { validate, parse } = await load('graphql');

let failed = false;
const fail = (msg) => { console.log('FAIL ' + msg); failed = true; };
const config = await loadConfig({ rootDir: process.cwd(), throwOnMissing: false, throwOnEmpty: false });
if (!config) { fail('no graphql.config.yml (or .graphqlrc) found'); process.exit(1); }
const project = config.getDefault();
let schema;
try { schema = await project.getSchema(); } catch (e) { fail('schema: ' + e.message); process.exit(1); }
const types = Object.keys(schema.getTypeMap()).filter((t) => !t.startsWith('__'));
console.log(`schema: ${types.length} types from ${JSON.stringify(project.schema)}`);
const docs = await project.getDocuments();
if (docs.length === 0) fail('documents: no files match ' + JSON.stringify(project.documents));
for (const d of docs) {
  const errors = validate(schema, d.document ?? parse(d.rawSDL));
  const name = d.location.replace(process.cwd() + '/', '');
  if (errors.length) errors.forEach((e) => fail(`${name}: ${e.message}`));
  else console.log(`ok ${name}`);
}
const endpoints = project.extensions?.endpoints;
console.log('endpoints: ' + (endpoints ? Object.keys(endpoints).join(', ') : 'none'));
process.exit(failed ? 1 : 0);
