-- Toolshed inventory. Recreated with the seed at every start (spring.sql.init.mode=always): the reference
-- product is a demo whose tests depend on the seed. A real deployment would use migrations (docs/adr/0004).
DROP SCHEMA IF EXISTS toolshed_inventory CASCADE;
CREATE SCHEMA toolshed_inventory;

CREATE TABLE toolshed_inventory.category (
  id   text PRIMARY KEY,
  name text NOT NULL UNIQUE
);

CREATE TABLE toolshed_inventory.branch (
  id            text PRIMARY KEY,
  name          text NOT NULL UNIQUE,
  address       text NOT NULL,
  opening_hours text NOT NULL
);

CREATE TABLE toolshed_inventory.tool (
  id           text PRIMARY KEY,
  barcode      text NOT NULL UNIQUE,
  name         text NOT NULL,
  condition    text NOT NULL CHECK (condition IN ('NEW', 'GOOD', 'WORN', 'NEEDS_REPAIR')),
  category_id  text NOT NULL REFERENCES toolshed_inventory.category,
  branch_id    text NOT NULL REFERENCES toolshed_inventory.branch,
  added_at     timestamptz NOT NULL,
  -- one row per tool; the kind decides which spec columns are set
  kind         text NOT NULL CHECK (kind IN ('POWER', 'HAND')),
  volts        int,
  cordless     boolean,
  weight_grams int,
  CHECK ((kind = 'POWER' AND volts IS NOT NULL AND cordless IS NOT NULL AND weight_grams IS NULL)
      OR (kind = 'HAND' AND weight_grams IS NOT NULL AND volts IS NULL AND cordless IS NULL))
);

-- keyset pagination: one index per sort order (docs/DESIGN.md, "Pagination")
CREATE INDEX tool_name_idx ON toolshed_inventory.tool (lower(name), id);
CREATE INDEX tool_added_idx ON toolshed_inventory.tool (added_at DESC, id DESC);
CREATE INDEX tool_category_idx ON toolshed_inventory.tool (category_id);
CREATE INDEX tool_branch_idx ON toolshed_inventory.tool (branch_id);
