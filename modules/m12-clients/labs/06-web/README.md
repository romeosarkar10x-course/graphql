# Lab 12.6: Web clients (optional, overview track)

Lecture: [12.6 Web clients](../../lectures/12.6-web-clients.md). Grading: rubric. Needs the reference server
and internet (npm downloads the packages once).

The same club card (club name, current book with its average rating, eight member rows) in two web clients,
rendered headless in Node with jsdom, then one mutation (`createReview` on the club's current book) through
each client's own API. The harness counts how often each component rendered because of it.

```bash
bin/course start m12/06
cd workspace/m12-clients/06-web/web
bin/course web ci              # once: installs the pinned packages (package-lock.json)
bin/course web run measure     # relay-compiler, Babel, then node dist/measure.js
```

The starter has the Apollo Client version (`src/apollo/ClubCard.jsx`: one `useQuery`, props down). Write:

1. **Relay** (required): `src/relay/ClubCard.jsx`, `BookPanel.jsx`, `MemberRow.jsx` with colocated fragments
   (`useLazyLoadQuery` at the top, `useFragment` in each child) and `src/relay/addReview.js`
   (`commitMutation`). The harness picks them up by name.
2. Optional: `src/memo/ClubCard.jsx` (the Apollo version with `React.memo` children) and
   `src/masked/ClubCard.jsx` (Apollo Client 4 data masking: `useFragment` per child, the harness turns on
   `dataMasking` for it).

Toolchain notes, measured on this setup: the schema file is `schema.graphql` because relay-compiler 20.1.1
panics on `.graphqls`; `graphql-artifacts.mjs` lets Node load Relay's extensionless artifact imports; and
react-relay is CommonJS, so ES modules import its default export and destructure it. urql is covered in the
lecture only.

In `notes.md`: the numbers, why each count is what it is, and what Relay cost compared with Apollo.

## Done when

`notes.md` passes the rubric (`bin/course grade m12/06`). This lab is optional: it is not in the checkpoint.
