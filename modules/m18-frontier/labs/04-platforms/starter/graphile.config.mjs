// PostGraphile 5 (MIT): a GraphQL API generated from the tables, keys and functions of one PostgreSQL schema.
//   npm ci && npm start          (inside the course image; the test loads Module 9's club dataset into m18_04)
import { PostGraphileAmberPreset } from "postgraphile/presets/amber";
import { makePgService } from "postgraphile/adaptors/pg";

/** @type {GraphileConfig.Preset} */
export default {
  extends: [PostGraphileAmberPreset],
  pgServices: [
    makePgService({
      connectionString: process.env.DATABASE_URL ?? "postgres://shelfie:shelfie@postgres:5432/shelfie",
      schemas: ["m18_04"],
    }),
  ],
  grafserv: { port: Number(process.env.PORT ?? 5678), host: "0.0.0.0" },
};
