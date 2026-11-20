# Mini-project — GitHub profile CLI

**Module 2 mini-project** · **Grading:** auto + rubric · **Needs:** nothing for the tests; a `GITHUB_TOKEN` to use it for real

A Maven + picocli command that prints a GitHub profile card from exactly **one** GraphQL request.
The tests run it against a stub GitHub on localhost, so you can finish it without a token; with one,
the test also runs it against the real API.

```bash
bin/course start m02/12
cd workspace/m02-query-language/12-project-github-cli
../../../bin/course shell -c 'mvn -q package && java -jar target/gh-profile.jar torvalds'
```

## The card

```
Linus Torvalds (@torvalds)
Portland, OR · Linux Foundation
followers 250000 · following 0 · public repos 8 · contributions 2400 this year
pinned
  linux                        ★ 200000  C
  subsurface-for-dirk          ★ 350     C++
```

- Line 1: `Name (@login)`, or `@login` when the name is null or blank.
- Line 2: location and company that are not blank, joined by ` · `; no line if both are blank.
- Line 3: followers, following, public repositories owned by the user, contributions in the last year.
- Pinned repositories, up to `--pinned` (default 6): `String.format("  %-28s ★ %-7d %s%n", name, stars, language)`,
  language `-` when null. No `pinned` line when there are none.

## The rules

- `src/main/resources/profile.graphql`: one operation `Profile($login: String!, $pinned: Int = 6)`. It
  must be valid against GitHub's schema; check with
  `../../../bin/course gql validate src/main/resources/profile.graphql --schema ../../../tools/schemas/github-2026-11-21.graphql`.
- Values in `variables`, never in the document. Token from `GITHUB_TOKEN`, sent as `Authorization: Bearer`.
- Exit codes: 0 card, 2 no token (and no request), 3 unknown login (`no GitHub user <login>` on stderr),
  4 any other HTTP or GraphQL failure. `--endpoint` overrides the URL (the tests use it).

## Done when

`bin/course test m02/12` is green and `bin/course grade m02/12` gives 7/10 or more. Then try it on
your friends' logins.
