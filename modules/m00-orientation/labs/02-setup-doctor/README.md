# Lab 0.2 — Your own setup doctor

**Lecture:** [0.2 Your Java toolbox](../../lectures/0.2-toolbox.md) · **Grading:** auto · **Needs:** `bin/course ref up`

`bin/course doctor` already checks your machine. Here you write a smaller one in Java, as a Maven
project, to get your hands on `pom.xml`, the Maven Wrapper, `ProcessBuilder` and `HttpClient`.

## Spec

`Doctor` prints one line per check, then exits **1 if any check failed, else 0**.

| Check name | OK when | Otherwise |
|---|---|---|
| `java` | `Runtime.version().feature() >= 25` | FAIL |
| `hurl` | `hurl --version` runs and exits 0 | FAIL |
| `jq` | `jq --version` runs and exits 0 | FAIL |
| `shelfie` | `GET $SHELFIE_URL/actuator/health` returns 200 (`SHELFIE_URL` defaults to `http://shelfie-ref:8080`) | FAIL |
| `github-token` | `GITHUB_TOKEN` is set and not blank | **warn** (never fails the run) |

Line formats (the tests match these): `ok   <name> (<detail>)`, `warn <name>: <detail>`,
`FAIL <name>: <how to fix it>`. Checks are independent: one failing must not stop the others.

## Do

```bash
bin/course start m00/02
bin/course shell
cd workspace/m00-orientation/02-setup-doctor
./mvnw -q compile exec:java          # run it
exit
bin/course test m00/02
```

## Experiment

Break one tool on purpose and read your own message. Inside `bin/course shell`, after one compile:

```bash
# hurl lives in /usr/local/bin; this PATH keeps java and jq but hides hurl
env PATH=/opt/java/openjdk/bin:/usr/bin:/bin java -cp target/classes dev.shelfie.doctor.Doctor
SHELFIE_URL=http://localhost:1 java -cp target/classes dev.shelfie.doctor.Doctor
```

Is each FAIL line enough to fix the problem without reading your code? If not, improve the message.
