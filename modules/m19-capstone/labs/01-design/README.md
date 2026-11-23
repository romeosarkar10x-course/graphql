# m19/01 Stage 1: pitch, design doc and contract

About 5 hours. Decide what you are building, design it from the screens inward, and write the contract
before any server code. A reviewer reads it before you build.

```bash
bin/course start m19/01          # workspace/m19-capstone/capstone/, a git repository's worth of templates
```

## Do

1. **Pitch** (`docs/PITCH.md`): the problem, who uses it, what it must get right, why GraphQL, what is out of
   scope. One page. Pick a brief from the [capstone README](../../README.md) or pitch your own.
2. **Screens first.** List the screens and jobs (a CLI command, a nightly report) and write one operation per
   screen in `operations/` (named; fragments may live in their own files).
3. **The contract** (`contract/*.graphqls`, one file per subgraph). At least 15 types of your own, an
   interface with two or more implementations, a union, a `@oneOf` input, a cursor connection with a filter
   input and a sort argument, mutations whose results carry expected failures as data, and a subscription or
   an operation with `@defer`. Describe your types.
4. **The design doc** (`docs/DESIGN.md`, 800+ words): the template's headings. Draw the architecture (a mermaid
   block is enough). Say what each subgraph's outage does to each screen.
5. **ADRs** (`docs/adr/`, 3 or more): the decisions with real alternatives, each with Status, Context, Decision
   and Consequences.
6. **The Hurl suite** (`tests/api.hurl`, 10+ requests): what a correct server answers. It cannot pass yet; it
   is the contract's executable half. Use `{{api}}` and `{{token_<role>}}` (see the manifest's variables).
7. **Fill in `capstone.json`**: `product`, `subgraphs` (name and schema; URLs can wait), `tokens`.
8. **The review.** Group: swap repositories with a friend and review each other's contract against
   [contract/design-review.md](../../contract/design-review.md), into `docs/REVIEW.md`. Solo:
   `bin/course grade m19/01` prints a prompt for the AI grader, which reviews as your peer. Answer each point
   under it, and change the contract where you agree.

## Check

```bash
bin/course test m19/01           # the contract composes, the schema has what is required, operations validate,
                                 # documents and ADRs are there
bin/course grade m19/01          # the design review rubric
```

The checker also prints Module 7's design lint as advice; argue or fix each line in the review.

## Done when

The checks pass, the review's verdict is "ready for stage 2", and you have committed (`git -C workspace commit`).
