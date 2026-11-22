import { PostGraphileAmberPreset } from "postgraphile/presets/amber";
import { makePgService } from "postgraphile/adaptors/pg";
import { makeJSONPgSmartTagsPlugin } from "postgraphile/utils";
export default {
  extends: [PostGraphileAmberPreset],
  plugins: [makeJSONPgSmartTagsPlugin({ version: 1, config: { procedure: { pick: { tags: { behavior: "-*" } } } } })],
  schema: { defaultBehavior: "-insert -update -delete" },
  pgServices: [makePgService({ connectionString: "postgres://shelfie:shelfie@postgres:5432/shelfie", schemas: ["m18_probe"] })],
  grafserv: { port: 5678, host: "0.0.0.0" },
};
