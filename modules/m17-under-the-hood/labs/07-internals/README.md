# Lab 17.7: graphql-java internals

Lecture: [17.7 graphql-java internals](../../lectures/17.7-internals.md). Grading: rubric. Needs internet (the
source and Gradle's dependencies), about 3 GB of disk and a few minutes of every core for the build.

You have built each part yourself. Now read the engine the rest of the course runs on, at the exact version the
course pins:

```bash
git clone --depth 1 --branch v26.1 https://github.com/graphql-java/graphql-java.git
cd graphql-java
```

graphql-java builds with **Gradle**, not Maven: it is the one place in the course where you run someone else's
build as it is. Its wrapper (`./gradlew`) downloads Gradle 9.4.0 and needs a Java 25 toolchain, the course image's.

## 1. Annotate five classes

For each class, write in `INTERNALS.md`: what part of the spec it implements (sections), the method that does the
work, which part of your engine it corresponds to, and one design difference from yours and why graphql-java made
that choice.

| Class | Your engine |
| --- | --- |
| `graphql.parser.Parser` (with `GraphqlAntlrToLanguage`) | Lexer, Parser |
| `graphql.validation.OperationValidator` | Validator |
| `graphql.execution.ExecutionStrategy` (with `AsyncExecutionStrategy`) | Executor |
| `graphql.execution.ValuesResolver` | Executor's coercion |
| `graphql.introspection.Introspection` | Introspection |

## 2. Run its tests

The tests are Spock (Groovy) and JUnit. Run the parser's: `./gradlew test --tests 'graphql.parser.*'`. Note how many
ran and how long it took.

## 3. Make a one-line change

Finding 7 of this module: `StringValueParsing.removeIndentation` splits block strings on `\n` only, so a block
string with CRLF keeps its `\r`s, where the spec's `BlockStringValue()` splits on every line terminator. Make the
one-line fix, add a case to `StringValueParsingTest.groovy`, and run that test class.

## 4. Build it and use it

```bash
RELEASE_VERSION=26.1-local ./gradlew publishToMavenLocal -PpublishToMavenLocal
```

`RELEASE_VERSION` sets the version (otherwise it is a date-stamped development version), and the
`publishToMavenLocal` property turns off signing (`build.gradle`, the `signing` block). Then point a pom at
`com.graphql-java:graphql-java:26.1-local`: any earlier lab's server will do. Show that the CRLF block string now
comes out the spec's way.

The course has not run step 4 for you: its build was not run while this module was written, so treat these two
flags as read from `build.gradle` and check them yourself. Note in `INTERNALS.md` what you had to change.

## Done when

`INTERNALS.md` passes the rubric (`bin/course grade m17/07`).
