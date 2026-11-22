# Lab 16.7: GraphQL as a public product: a dated version and its paperwork

Lecture: [16.7 GraphQL as a public product](../../lectures/16.7-public-product.md). Grading: auto + rubric.

Shelfie's API opens to partners and adopts Shopify-style versions:

- dated names (`YYYY-MM`);
- a new version each quarter;
- each version supported for at least 12 months.

The version in production is `contract/versions/2026-10.graphqls`. The next is
`contract/versions/2027-01.graphqls`, released 2027-01-01. Diff them first:

```bash
bin/course design diff modules/m16-production/contract/versions/2026-10.graphqls modules/m16-production/contract/versions/2027-01.graphqls
```

Write what a partner needs:

1. **`CHANGELOG.md`**: a `## 2027-01` section. Name every breaking change under its own heading, then every
   addition (`Book.subtitle`, `Club.memberTotal`, `ReviewVisibility`).
2. **`MIGRATION.md`**: for each breaking change, the operation before and after, in ```` ```graphql ```` blocks.
   The test validates every block. Each "before" must be valid on 2026-10 only, each "after" valid on 2027-01,
   and every block valid on one of the two.
3. **`ANNOUNCEMENT.md`**: the breaking-change announcement. It starts with three lines:
   - `Published: YYYY-MM-DD`, at least 30 days before the release;
   - `Release: 2027-01-01`;
   - `2026-10 supported until: YYYY-MM-DD`, at least nine months after the release.

   Then: what changes, who is affected, what to do (link `MIGRATION.md`), and the dates.
4. **`PEER-REVIEW.md`**: the review of your announcement and what it changed.

```bash
bin/course start m16/07
cd workspace/m16-production/07-public-product
bin/course test m16/07
```

## Group variant

Everyone writes an announcement. Swap with a partner and review theirs:

- Can a reader tell in ten seconds whether they are affected?
- Is every date concrete?
- Does every removal name its replacement?
- Can the reader test before the release?
- Does it say what happens after the support date?

Put the review you received, and what you changed, in `PEER-REVIEW.md`.

**Solo:** review your own draft a day later against the same questions.

## Done when

`bin/course test m16/07` passes and the documents pass the rubric (`bin/course grade m16/07`).
