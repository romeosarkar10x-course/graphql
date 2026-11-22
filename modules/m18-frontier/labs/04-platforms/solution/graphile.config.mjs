// PostGraphile 5 (MIT): a GraphQL API generated from the tables, keys and functions of one PostgreSQL schema.
//   npm ci && npm start          (inside the course image; the test loads Module 9's club dataset into m18_04)
import { PostGraphileAmberPreset } from "postgraphile/presets/amber";
import { makePgService } from "postgraphile/adaptors/pg";
import { makeJSONPgSmartTagsPlugin } from "postgraphile/utils";

/** @type {GraphileConfig.Preset} */
export default {
  extends: [PostGraphileAmberPreset],
  plugins: [
    // The loader's helper function pick(words, g, k) is SQL plumbing, not API: "-*" removes every behaviour, so
    // it is no longer a Query field. (A smart tag in a COMMENT ON FUNCTION would do the same from the database.)
    makeJSONPgSmartTagsPlugin({ version: 1, config: { procedure: { pick: { tags: { behavior: "-*" } } } } }),
  ],
  schema: {
    // Read-only: no create, update or delete mutations for any table. Writes go through a hand-built service.
    defaultBehavior: "-insert -update -delete",
  },
  pgServices: [
    makePgService({
      connectionString: process.env.DATABASE_URL ?? "postgres://shelfie:shelfie@postgres:5432/shelfie",
      schemas: ["m18_04"],
    }),
  ],
  grafserv: { port: Number(process.env.PORT ?? 5678), host: "0.0.0.0" },
};
