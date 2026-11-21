# Setup

## 1. Install Docker and bash

| OS | Install |
|---|---|
| Linux | Docker Engine + the Compose plugin from your distro or docs.docker.com; add yourself to the `docker` group (`sudo usermod -aG docker $USER`, then log out and in) |
| macOS | Docker Desktop (or OrbStack/Colima); bash 4+ via `brew install bash` (macOS ships bash 3.2) |
| Windows | Docker Desktop with WSL 2; run everything from a WSL Ubuntu shell |

Check: `docker info` works without `sudo`, and `docker compose version` prints v2 or later.

## 2. Build and check

```bash
git clone <this repo> && cd graphql
bin/course doctor
```

The first run builds the `graphql-course:1` image (about 1 GB, a few minutes) and downloads jars into
`.cache/`. Fix every `FAIL` line the doctor prints.

## 3. Start the reference server

```bash
bin/course ref up
```

Open http://localhost:8080/graphiql and run `{ hello }`. Ports used: 8080, 9090 (gRPC), 8474 and 8666
(Toxiproxy), 8333 (S3 storage, Module 4) and 5432 (PostgreSQL, Module 8). `bin/course ref down` stops them.

## 4. Optional: a GitHub token

Lab 0.3's GitHub part and some Module 2 exercises call the GitHub GraphQL API, which needs a token.
Create a fine-grained personal access token (GitHub → Settings → Developer settings → Personal access
tokens) with read-only access to public repositories, then:

```bash
export GITHUB_TOKEN=github_pat_...    # in your shell profile; bin/course passes it into the image
```

Never commit it.

## 5. Optional: local JDK and IDE

Grading always runs in the image, but an IDE wants a local JDK: `sdk install java 25-tem` and
`sdk install maven 3.9.16` with [SDKMAN](https://sdkman.io), then open a lab folder's `pom.xml` in IntelliJ.
Install the IntelliJ GraphQL plugin for `.graphql` file support.
