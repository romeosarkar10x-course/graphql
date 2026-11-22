import { PostGraphileAmberPreset } from "postgraphile/presets/amber";
import { makePgService } from "postgraphile/adaptors/pg";
export default {
  extends: [PostGraphileAmberPreset],
  pgServices: [makePgService({ connectionString: "postgres://shelfie:shelfie@postgres:5432/shelfie", schemas: ["m18_probe"] })],
  grafserv: { port: 5678, host: "0.0.0.0" },
};
