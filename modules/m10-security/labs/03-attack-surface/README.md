# Lab 10.3: The attack surface

Lecture: [10.3 The attack surface](../../lectures/10.3-attack-surface.md). Grading: auto + rubric. Needs nothing.

`contract/vulnerable-shelfie/` is a deliberately vulnerable fork of Module 10's Shelfie, with every
shortcut a real team takes under deadline ("it's an API", "the web app keeps the token in a cookie",
"unsigned tokens are handy for local testing"). Attack it nine ways and record what each attack cost it.
Treat it as a black box: read the schema through introspection, not the source. The same fork hosts
the capture the flag (lab 10.9), and some of these attacks earn flags.

```bash
bin/course start m10/03
bin/course shell
  /course/modules/m10-security/contract/vulnerable.sh start        # :4000, heap capped at 512 MB
  cd /course/workspace/m10-security/03-attack-surface
  /course/modules/m10-security/contract/attack.sh attacks/depth.graphql
  # status 200  12 ms  1151 bytes  server CPU 50 ms  RSS 491 -> 493 MB  errors: none
  /course/modules/m10-security/contract/vulnerable.sh stop
bin/course java /course/modules/m10-security/contract/Shape.java attacks/aliases.graphql   # depth, fields, aliases
```

The player account is `ben` / `ben-pw`: `mutation { login(username: "ben", password: "ben-pw") }` returns
a token. `bin/course token` does not work here: the fork signs with its own key, new at every start.

## The nine attacks

`attacks/README.md` lists each file and when it counts as working. Deep nesting, alias overloading, list
fan-out, a fragment cycle, field suggestions, introspection, SQL injection in a resolver, CSRF with a
simple content type, and a batched brute force of club c1's 4-digit invite code.

Then **crash a node**: push the fan-out deeper until the fork dies, and record what the last request and
the next one got. `vulnerable.sh start` brings it back.

## Done when

- `bin/course test m10/03` passes: every attack does what `attacks/README.md` says, against a fresh fork.
- `notes.md` passes the rubric (`bin/course grade m10/03`), with your own measured numbers.
