# Lab 7.4 — Every `!` is a promise

**Lecture:** [7.4 Nullability as a decision](../../lectures/7.4-nullability.md) · **Grading:** auto + rubric · **Needs:** nothing

Lab 3.4 showed where a null lands. This lab is the design side: audit a whole schema with a
checklist, then prove the result against four outages. The starter `club.graphqls` marks everything
non-null "because the data is always there". One failing star rating blanks the whole page.

## Facts about Shelfie's data

| Data | Where it comes from | Can it be absent in correct data? |
|---|---|---|
| Club id, name, owner id, description | the club row | description: yes (optional) |
| `memberCount`, `members`, `viewerIsMember` | membership service | `viewerIsMember`: yes, when signed out. A club always has its owner as a member |
| `currentBook` | the club row (a book id) | yes, between books |
| `nextMeetup` | meetup service | yes, when none is planned |
| Meetup id, time, venue | the meetup's row | no |
| User id | the key | no |
| User name, avatar | profile service | avatar: yes (no picture). Every member has a name |
| Book id, title, authors, cover | catalogue row | cover: yes (the publisher sent none) |
| `averageRating` | ratings service | yes, nobody reviewed the book |

## Do

1. `audit.md`: run the checklist on every field and fill the table.
2. `club.graphqls`: apply it. Three outcomes per field: non-null (`!`), nullable, or nullable **plus
   `@semanticNonNull`** (null only ever means "an error happened"; the directive is declared at the top,
   from the [Semantic Nullability RFC](https://rfcs.graphql.org/rfcs/SemanticNullability/), RFC 0).
   Keep every `!` you can: a nullable field costs every client a null check.
3. Don't edit `club-page.graphql`.

## Experiment: kill one dependency

```
../../../bin/course design outage club.graphqls club-page.graphql --fail Book.averageRating
../../../bin/course design outage club.graphqls club-page.graphql --fail User.name,User.avatarUrl
../../../bin/course design outage club.graphqls club-page.graphql --fail Club.nextMeetup
../../../bin/course design outage club.graphqls club-page.graphql --fail Club.viewerIsMember,Club.memberCount,Club.members
```

Each prints the page's data and how many of its 25 values survive. Run all four on the starter
schema and on yours. Then run the **starter** with `--no-propagation` added: graphql-java's
`@experimental_disableErrorPropagation`, the shape of `onError: NULL` from
[RFC #1163](https://rfcs.graphql.org/rfcs/1163/). Record everything in `audit.md`.

## Done when

`bin/course test m07/04` is green and `bin/course grade m07/04` gives 7/10 or more.
