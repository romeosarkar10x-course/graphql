# Game — GitHub API scavenger hunt

**Module 0 game** · **Grading:** auto (score out of 8) · **Needs:** a browser

Answer 8 questions about the GitHub GraphQL API using **only the explorer's docs pane**: no web
search, no schema file, no AI. The point is learning to navigate a large schema (GitHub's is
about 76,000 lines of SDL) by clicking through types.

The explorer at https://docs.github.com/en/graphql/overview/explorer needs a GitHub sign-in. No
account? Use GraphiQL against `https://api.github.com/graphql` with a token, or pair with someone who
has one.

```bash
bin/course start m00/06      # then fill in answers.txt
bin/course test m00/06       # score; wrong answers are named, right ones are not revealed
```

**Solo:** under 15 minutes for 8/8.

**Group:** everyone starts together; first to 8/8 on `bin/course test` wins. Log times in
[games/leaderboards/api-scavenger-hunt.md](../../../../games/leaderboards/api-scavenger-hunt.md).
Answers were checked against GitHub's public schema on 2026-11-21; if GitHub changes it, a test may
go stale: tell the course maintainer.
