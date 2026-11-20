# Lab 0.3 — First contact

**Lecture:** [0.3 First contact](../../lectures/0.3-first-contact.md) · **Grading:** auto + rubric · **Needs:** internet; GitHub part needs `GITHUB_TOKEN` (optional)

Write three operations against three real public APIs, using only each API's explorer and docs pane.

| File | Endpoint (open it in a browser: each serves an explorer) | Fetch |
|---|---|---|
| `operations/swapi-film.graphql` | https://swapi-graphql.netlify.app/graphql | film `"ZmlsbXM6MQ=="`: `title`, `director`, `releaseDate` |
| `operations/countries-india.graphql` | https://countries.trevorblades.com/graphql | country `"IN"`: `name`, `capital`, `currency`, each language's `name` |
| `operations/github-viewer.graphql` | https://docs.github.com/en/graphql/overview/explorer | your `login` and the `name` of your first 3 repositories |

Name every operation (`query FirstFilm { ... }`): unnamed operations are fine for one-offs, but
named ones show up in server logs and traces.

## Experiment

Ask SWAPI for 1 field of a film, then for 20. Record both response sizes in `notes.md` and answer its
questions.

## Done when

`bin/course test m00/03` is green (the GitHub check is skipped without a token) and the rubric gives 7/10+.
