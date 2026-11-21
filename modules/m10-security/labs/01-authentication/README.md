# Lab 10.1: Authentication

Lecture: [10.1 Authentication](../../lectures/10.1-authentication.md). Grading: auto + rubric. Needs: `bin/course ref up` (Keycloak).

The starter is Module 10's Shelfie: the contract in `contract/secure.graphqls`, served from memory
(`Store.java`: 40 readers, 12 clubs, 2 of them private). Nobody is ever signed in: `viewer` is always
null and every mutation says "sign in first". Make the app an OAuth 2 resource server for the course
Keycloak, so that a valid token identifies the reader and a bad one never reaches GraphQL.

## Keycloak and the five users

Realm `shelfie` at `http://keycloak:8180` (from the host: `localhost:8180`, admin console `admin`/`admin`).
Client `shelfie-cli` (public, password grant on for tests). Every password is `<user>-pw`.

| User | Reader | Roles |
|---|---|---|
| ada | r1 | member |
| ben | r2 | member |
| max | r3 | member, moderator |
| olu | r4 | member (owns club c1) |
| root | r5 | member, admin |

```bash
bin/course shell -c 'curl -s -d grant_type=password -d client_id=shelfie-cli -d username=ada -d password=ada-pw \
  http://keycloak:8180/realms/shelfie/protocol/openid-connect/token | jq -r .access_token'
bin/course token ada --expired        # tokens Keycloak would never give you, signed with the course key
```

The password grant is here because tests have no browser. A real client uses the authorization code flow
with PKCE (lecture 10.1).

## What to build

1. Add `spring-boot-starter-oauth2-resource-server` and a `SecurityFilterChain` that permits every path
   but validates any bearer token it sees. Configure issuer, audience (`shelfie-api`), and the claims:
   username in `preferred_username`, roles in `roles`.
2. `viewer` returns the signed-in reader, using `@AuthenticationPrincipal`. `Viewer.current()` does the
   same for mutations.
3. Run the experiment: call `{ viewer { id name roles } }` with no token, a Keycloak token, an expired
   one, a forged one, `--none`, `--aud other-api`, and `--iss` set to another issuer. Record what came
   back in `notes.md` and answer its questions.

```bash
bin/course start m10/01
cd workspace/m10-security/01-authentication && bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-secure.jar'
bin/course test m10/01
```

## Done when

- `bin/course test m10/01` passes: 11 Hurl checks (anonymous works; Keycloak tokens identify ada and max;
  expired, forged, unsigned, wrong-audience, wrong-issuer and malformed tokens get 401 with a Bearer
  challenge; a mutation without a token is `UNAUTHORIZED`).
- `notes.md` passes the rubric (`bin/course grade m10/01`).

## Changes from the plan

Keycloak runs as a course service, not in Testcontainers, because the course container has no Docker
socket (the same reason PostgreSQL is a service since Module 8). The realm signs with a course key that
is in the repository on purpose (`contract/keys/README.md`), so tests can mint expired and wrong-audience
tokens.
