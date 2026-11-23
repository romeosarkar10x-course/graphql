-- Toolshed lending. Recreated with the seed at every start, like inventory (docs/adr/0004).
DROP SCHEMA IF EXISTS toolshed_lending CASCADE;
CREATE SCHEMA toolshed_lending;

CREATE TABLE toolshed_lending.member (
  id           text PRIMARY KEY,
  -- the token's preferred_username
  handle       text NOT NULL UNIQUE,
  display_name text NOT NULL,
  email        text NOT NULL
);

-- The tools lending may lend, by inventory's id. Lending knows nothing else about a tool (docs/adr/0003).
CREATE TABLE toolshed_lending.tool_stock (
  tool_id text PRIMARY KEY
);

CREATE SEQUENCE toolshed_lending.loan_seq START 100;

CREATE TABLE toolshed_lending.loan (
  id          text PRIMARY KEY DEFAULT 'l' || nextval('toolshed_lending.loan_seq'),
  tool_id     text NOT NULL REFERENCES toolshed_lending.tool_stock,
  member_id   text NOT NULL REFERENCES toolshed_lending.member,
  borrowed_at timestamptz NOT NULL,
  due_at      timestamptz NOT NULL,
  returned_at timestamptz,
  CHECK (due_at > borrowed_at)
);
-- At most one open loan per tool, whatever the application does: two members racing for the last drill
-- cannot both win (docs/DESIGN.md, "Concurrency").
CREATE UNIQUE INDEX loan_one_open_per_tool ON toolshed_lending.loan (tool_id) WHERE returned_at IS NULL;
CREATE INDEX loan_member_idx ON toolshed_lending.loan (member_id, borrowed_at DESC, id DESC);
CREATE INDEX loan_due_idx ON toolshed_lending.loan (due_at, id) WHERE returned_at IS NULL;

-- Idempotency: the first answer to (member, key) is stored and replayed (docs/adr/0005).
CREATE TABLE toolshed_lending.borrow_request (
  member_id       text NOT NULL REFERENCES toolshed_lending.member,
  idempotency_key text NOT NULL,
  result_type     text NOT NULL,
  result          jsonb NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (member_id, idempotency_key)
);
