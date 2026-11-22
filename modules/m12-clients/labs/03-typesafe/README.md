# Lab 12.3: Type-safe Java clients

Lecture: [12.3 Type-safe Java clients](../../lectures/12.3-typesafe-clients.md). Grading: auto + rubric.
Needs the reference server.

`pom.xml` runs DGS codegen (`graphqlcodegen-maven-plugin`, `generateClientApi`) on
`src/main/resources/schema/shelfie.graphqls`, a copy of the reference schema. Write `ClubPage.java`: print a
club page using only the generated requests, projections and types, through `DgsGraphQlClient`. No document
text in Java.

`java -jar target/shelfie-typed.jar <url> <clubId>` prints exactly (club c1):

```
The Midnight Readers (8 members), run by Kemi Adeyemi
Reading: The Left Hand of Darkness by Ursula K. Le Guin (1969)
  4/5 Hugo Silva: Estraven deserved better.
  3/5 Jonas Berg: Beautiful but I wanted more physics.
In the catalogue:
  The Left Hand of Darkness (4.2)
  Piranesi (4.5)
  The Remains of the Day (5.0)
```

The club, its owner, its current book with the book's last 2 reviews, then the first 3 books with their
average rating (left out when null). A club with no current book prints `Reading: nothing right now`; an
unknown id prints `No club <id>`. **One HTTP request** per run: `club` and `books` go in one document.

```bash
bin/course start m12/03
cd workspace/m12-clients/03-typesafe
bin/course shell -c "mvn -q package && java -jar target/shelfie-typed.jar http://shelfie-ref:8080/graphql c1"
```

Experiment: rename `Book.title` to `Book.name` in your schema copy and rebuild. The test does the same to a
copy of your project and expects `mvn compile` to fail in `ClubPage.java`.

In `notes.md`: the document that went over the wire, the compiler error, and what codegen costs.

## Done when

`bin/course test m12/03` passes (both club pages byte for byte, one request each, the rename failing to
compile) and `notes.md` passes the rubric (`bin/course grade m12/03`).
