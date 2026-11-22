// Lab 13.6: schema lint rules, run with `bin/course node npx eslint src/main/resources/graphql`.
// graphql-eslint reads the SDL files directly, so the same config works for a server in any language.
import graphql from '@graphql-eslint/eslint-plugin';

export default [
  {
    files: ['src/main/resources/graphql/**/*.graphqls'],
    languageOptions: {
      parser: graphql.parser,
      // schema-wide rules (unreachable types, ids) need the whole schema, not one file at a time
      parserOptions: { graphQLConfig: { schema: 'src/main/resources/graphql/**/*.graphqls' } },
    },
    plugins: { '@graphql-eslint': graphql },
    rules: {
      // TODO start from graphql.configs['flat/schema-recommended'].rules, then make them Shelfie's house style
    },
  },
];
