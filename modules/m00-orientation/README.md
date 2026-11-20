# Module 0 — Orientation and the Java toolchain

About 3 hours, 4 lectures. By the end your toolchain works, you can read a conformance-test failure,
and you have sent GraphQL from Java with nothing but the JDK and Jackson.

## Before you start

```bash
bin/course doctor      # fix every FAIL line
bin/course ref up      # reference Shelfie at http://localhost:8080/graphiql
```

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 0.1 | [Welcome and course map](lectures/0.1-welcome.md) | [m00/01 First red test](labs/01-first-red-test/README.md) | auto + rubric |
| 0.2 | [Your Java toolbox](lectures/0.2-toolbox.md) | [m00/02 Your own setup doctor](labs/02-setup-doctor/README.md) | auto |
| 0.3 | [First contact](lectures/0.3-first-contact.md) | [m00/03 Three public APIs](labs/03-first-contact/README.md) | auto + rubric |
| 0.4 | [One request under the hood](lectures/0.4-under-the-hood.md) | [m00/04 GraphQL from jshell](labs/04-jshell-first-query/README.md) | auto + rubric |

## Games

| Game | Solo | Group |
|---|---|---|
| [m00/05 jshell speedrun](labs/05-jshell-speedrun/README.md) | Beat par (2 lines) | 15-minute round, leaderboard |
| [m00/06 GitHub API scavenger hunt](labs/06-api-scavenger-hunt/README.md) | 8/8 in 15 minutes | Race to 8/8 |

## Spec refs

[GraphQL-over-HTTP: request parameters and media types](https://graphql.github.io/graphql-over-http/draft)

## Checkpoint

[checkpoint.md](checkpoint.md) is the quiz. Then:

```bash
bin/course checkpoint 00    # doctor lab green, 3 saved operations, 1 jshell script; tags workspace/
```
