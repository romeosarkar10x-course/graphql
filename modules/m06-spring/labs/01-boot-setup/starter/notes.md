# Lab 6.1 notes

## Load test (`java Load.java <n>` against `slowHello`, 100 ms each)

| Threads | n = 200 | n = 1000 | n = 2000 |
|---|---|---|---|
| platform (`spring.threads.virtual.enabled=false`) | | | |
| virtual | | | |

## Questions

1. Explain the platform numbers with one division. Which Tomcat setting is in it?
2. Your Module 5 server and this one serve the same schema. List three things Spring now does that you
   wrote by hand, and one thing your server did that Spring does not (run lab 5.4's audit against this
   app and look).
3. Spring for GraphQL can run on Spring MVC or WebFlux. Which did you get, and when would you pick the
   other?
4. Where did `RuntimeWiring`, the `GraphQL` object and the `ExecutionInput` go?
