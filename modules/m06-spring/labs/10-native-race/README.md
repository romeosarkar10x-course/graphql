# Game 6b — Native image race

**Lecture:** [6.9 Framework bake-off](../../lectures/6.9-bake-off.md) · **Grading:** rubric · **Needs:** m06/01 and m06/08 in your workspace; 8 GB of free RAM; about 1 GB download the first time

Build your Spring (lab 6.1) and Quarkus (lab 6.8) apps as GraalVM native binaries. Fastest cold start to
the first correct GraphQL answer wins.

`bin/course native <mvn args>` runs Maven inside GraalVM CE 25 (`ghcr.io/graalvm/native-image-community:25`
plus the course's Maven), in the directory you run it from:

```
cd workspace/m06-spring/01-boot-setup && ../../../bin/course native -Pnative native:compile -DskipTests
cd workspace/m06-spring/08-quarkus   && ../../../bin/course native package -DskipTests -Dquarkus.native.enabled=true
```

Then time them with lab 6.9's `Bench.java`, which accepts any start command:

```
bin/course shell
cd workspace/m06-spring/09-bake-off
java Bench.java "Spring native" ../01-boot-setup/src ../01-boot-setup -- ./target/shelfie-boot
java Bench.java "Quarkus native" ../08-quarkus/src ../08-quarkus -- ./target/shelfie-quarkus-1-runner
```

Fill `race.md`: the table, what broke, three questions.

## Solo variant

Beat the reference: Quarkus native answered in 32 ms and Spring native in 49 ms on the build machine.
Try `-march=native`, a smaller `-Xmx`, or removing an extension, and record what each changed.

## Group variant

Everyone builds on their own machine and posts the four rows. The fastest first answer per stack wins;
then compare build times and explain why the rankings differ between machines.

## Done when

`bin/course grade m06/10` gives 7/10 or more (rubric only: binaries are too big to grade automatically).
