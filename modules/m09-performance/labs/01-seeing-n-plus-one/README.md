# Lab 9.1 — Seeing N+1: count every statement

**Lecture:** [9.1 Seeing N+1](../../lectures/9.1-seeing-n-plus-one.md) · **Grading:** both · **Needs:** postgres (`bin/course ref up`)

The starter is Shelfie's club dashboard served the obvious way: one repository call per field per
parent, all correct and all fast on their own. Before you fix anything, you make the cost visible: every
response reports how many SQL statements it ran.

The app serves `contract/club.graphqls` on `:4000` from schema `m09_01` (`bin/course clubs m09_01` loads
it; the test reloads it). Run it with
`bin/course shell -c 'mvn -q package -DskipTests && java -jar target/shelfie-perf.jar'` from your copy.

## Do

1. Turn on Hibernate's SQL logging (`spring.jpa.show-sql`) and run the dashboard
   (`contract/dashboard.graphql`) once. Scroll the log.
2. Count statements properly: wrap the `DataSource` in datasource-proxy (`net.ttddyy:datasource-proxy`
   1.11.1) with a `BeanPostProcessor`, and count in a `QueryExecutionListener`. Logging is for reading;
   a counter is for tests.
3. Report the count per request in the response: `extensions.sql.statements`, from a
   `WebGraphQlInterceptor` (read the counter before and after `chain.next`). Every request counts from
   zero, and a request that loads nothing reports 0.

## Experiment

Before running each query, predict its statement count from the schema and the resolvers, and write
the prediction down. Then measure. Three queries are in `notes.md`. For each, write the formula that
explains the number, then the general rule for nested lists.

## Done when

`bin/course test m09/01` is green (it checks five exact counts, including 1,250 for the dashboard) and
`bin/course grade m09/01` passes the notes.
