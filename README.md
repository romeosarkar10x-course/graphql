# GraphQL in Java: Zero to Production

A hands-on GraphQL course in Java: 19 modules, from your first query to a federated, secured,
observable graph, plus a capstone. You build **Shelfie**, a book-club platform, along the way.
The full plan is in [plan/syllabus.md](plan/syllabus.md).

Stack: Java 25, Maven, Spring Boot 4.2, Spring for GraphQL 2.1, graphql-java 26 ([VERSIONS.md](VERSIONS.md)).

## Setup

You need Docker (with Compose) and bash. Everything else runs in the course image. See [SETUP.md](SETUP.md).

```bash
bin/course doctor        # checks your machine; builds the image on first run
bin/course ref up        # reference Shelfie: http://localhost:8080/graphiql
bin/course list          # modules and labs
```

## Doing a lab

```bash
bin/course start m00/01            # copy the starter to workspace/m00-orientation/01-first-red-test/
# ...read the lab README, edit files under workspace/...
bin/course test m00/01             # auto-graded part
bin/course grade m00/01            # rubric part: prints the prompt for your AI grader
bin/course test m00/01 --solution  # see the reference pass (spoiler-free: it prints checks, not answers)
```

| Command | What it does |
|---|---|
| `bin/course doctor` | Host + image + services check, with a fix for each failure |
| `bin/course ref up\|down\|status\|logs` | Reference server (REST, GraphQL, gRPC) and Toxiproxy |
| `bin/course toxi latency 700` / `toxi reset` | Add or remove latency on `localhost:8666` |
| `bin/course hurl file.hurl` | Hurl inside the course network, `{{ref}}` preset |
| `bin/course java File.java` / `jshell` | Single-file Java or jshell with Jackson 3 on the class path |
| `bin/course shell` | bash in the course image |
| `bin/course checkpoint NN` | Verify a module's checkpoint and tag `checkpoint-NN` in `workspace/` |
| `bin/course check --starters` | Course self-test |

Grading: **auto** labs are tests; **rubric** labs are graded by an AI agent following
[docs/AGENT_GRADING.md](docs/AGENT_GRADING.md); **both** means both. Stuck? Each lab's `HINTS.md` has three
escalating hints, and [docs/TUTOR.md](docs/TUTOR.md) tells an AI tutor how to help without giving answers.
Playing with friends: [docs/GROUP_PLAY.md](docs/GROUP_PLAY.md).

## Modules

| Phase | Module | Status |
|---|---|---|
| Foundations | [M0 Orientation and the Java toolchain](modules/m00-orientation/README.md) | Ready |
| | [M1 Why GraphQL exists](modules/m01-why-graphql/README.md) | Ready |
| | M2 The query language · M3 Type system and SDL · M4 Mutations | Planned |
| Building servers | M5 graphql-java from scratch · M6 Spring for GraphQL, DGS, Quarkus · M7 Schema design · M8 Pagination and errors | Planned |
| Production | M9 Performance · M10 Security · M11 Real-time · M12 Clients · M13 Testing · M14 Advanced patterns | Planned |
| Platform | M15 Federation · M16 Production · M17 Under the hood · M18 The frontier · Capstone | Planned |

## Layout

```
bin/course              runner (docker run / docker compose)
docker/                 course image and compose services
tools/                  runner internals, test helpers, course-lib (jars for single-file labs)
reference/shelfie-ref/  reference server used by Modules 0-1
modules/mNN-*/          lectures/, labs/, checkpoint.md
games/leaderboards/     group scores
plan/                   the course plan
docs/                   grading, tutoring, authoring, group play
workspace/              your work (git-ignored here; your own git repo)
_workbench/             build artifacts kept for reference
```
