// Two import fixes for running Babel's output in Node:
// - babel-plugin-relay imports artifacts as "./__generated__/X.graphql"; the file is X.graphql.js.
// - source files import each other as "./BookPanel.jsx"; Babel wrote dist/BookPanel.js.
// Used as: node --import ./graphql-artifacts.mjs dist/measure.js
import { registerHooks } from "node:module";

registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.endsWith(".graphql")) return nextResolve(specifier + ".js", context);
    if (specifier.startsWith(".") && specifier.endsWith(".jsx")) return nextResolve(specifier.slice(0, -1), context);
    return nextResolve(specifier, context);
  },
});
