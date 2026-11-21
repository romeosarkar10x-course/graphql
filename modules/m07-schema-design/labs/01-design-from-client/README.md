# Lab 7.1 — Three screens first, SDL second

**Lecture:** [7.1 Design from the client](../../lectures/7.1-design-from-client.md) · **Grading:** auto + rubric · **Needs:** nothing

The starter's `shelfie.graphqls` was written from database tables: `getClub(club_id: Int): club_row`,
`owner_user_id`, `author_names: String`. No screen can be built from it without a second request or
string parsing. Throw it away and design from what three screens show.

## The screens

[`contract/screens/`](../../contract/screens/README.md) has the view model of each screen: the JSON the
screen component wants, key for key. `club-page.json`, `book-page.json`, `profile-page.json`.

## Do

1. `shelfie.graphqls`: a schema that serves the three screens.
   - Entry points `club(id: ID!)`, `book(id: ID!)`, `user(handle: String!)`.
   - Naming: PascalCase types, camelCase fields and arguments, UPPER_CASE enum values.
   - A **description on every type, field and enum value**. Say what null means where a field is nullable.
   - References, not keys: `owner: User!`, never `ownerId`.
2. `screens/<name>-page.graphql`: the one operation each screen sends. Its response must have exactly
   the screen's key paths. Use **aliases** where the screen's key and your field name differ
   (`memberPreview: members(first: 5)`); `id` fields are always allowed on top, client caches want them.
3. Check one screen:
   `../../../bin/course design shape shelfie.graphqls screens/club-page.graphql ../../../modules/m07-schema-design/contract/screens/club-page.json`
   (`missing` = the screen shows it, the query does not fetch it; `extra` = the reverse.)
   Lint: `../../../bin/course design lint shelfie.graphqls`.
4. `notes.md`, including the experiment.

## Experiment: the SDL-only test

Hand only `shelfie.graphqls` to a teammate and ask them to build the club page. Count their questions
before they can start. Solo: an AI assistant with only the schema and "build the club page from this
schema", or yourself a day later. Fix what the questions reveal; run it again.

## Done when

`bin/course test m07/01` is green and `bin/course grade m07/01` gives 7/10 or more.
