# Lab 13.6: Linting and architecture rules

Lecture: [13.6 Linting and architecture rules](../../lectures/13.6-lint-arch.md). Grading: auto + rubric. Needs
Node.js, which `bin/course` copies once from the official `node:24.22.1-slim` image into `.cache/node` (about
140 MB) so it runs inside the course container.

Three tools, three layers of the same Shelfie:

1. **graphql-eslint** on the schema (`eslint.config.mjs` at the project root, `package.json` pins eslint 10.14.0,
   `@graphql-eslint/eslint-plugin` 4.4.2 and graphql 16.15.0). Start from `flat/schema-recommended`, run it, and
   turn it into Shelfie's house style: naming conventions, a description on every type, field and enum value,
   deprecations with a reason. It reads `.graphqls` files, so the same config serves a server in any language.
2. **Spotless** in `pom.xml` (`spotless-maven-plugin` 3.11.0), bound to `verify`: at least unused imports,
   trailing whitespace and tabs.
3. **ArchUnit** rules as a JUnit test (`archunit-junit5` 1.5.1): graph, service and store are layers;
   **controllers never touch repositories**; services know nothing of GraphQL.

The clean project must pass all three. Then the experiment: the grader introduces **five violations**, one at a
time, runs all three tools on each, and prints who caught what. Each must be caught by the tool meant for it.

```bash
bin/course start m13/06
cd workspace/m13-testing/06-lint-arch
bin/course node npm ci
bin/course node npx eslint src/main/resources/graphql
bin/course shell -c 'mvn -q spotless:check test'
bin/course test m13/06
```

In `notes.md`: the who-caught-what table from the grader, why a layered rule alone is not enough, your formatter
choice, and what graphql-eslint's recommended config said about a schema that already had descriptions.

## Done when

`bin/course test m13/06` passes and `notes.md` passes the rubric (`bin/course grade m13/06`).
