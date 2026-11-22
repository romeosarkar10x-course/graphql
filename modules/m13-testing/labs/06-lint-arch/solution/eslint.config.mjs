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
      ...graphql.configs['flat/schema-recommended'].rules,
      // House style: one-line "descriptions" are fine.
      '@graphql-eslint/description-style': ['error', { style: 'inline' }],
      // Payloads and errors are values, not entities: they need no id.
      '@graphql-eslint/strict-id-in-types': ['error', { exceptions: { suffixes: ['Payload', 'Error'] } }],
      // Every type, field and enum value says what it is (arguments are described in their field's text).
      '@graphql-eslint/require-description': ['error', {
        types: true, FieldDefinition: true, EnumValueDefinition: true,
        rootField: true,
      }],
      // Deprecations must say what to use instead.
      '@graphql-eslint/require-deprecation-reason': 'error',
    },
  },
];
