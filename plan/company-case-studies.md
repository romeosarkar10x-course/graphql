# How companies implement GraphQL

Nine companies show the four main ways GraphQL is run in production: client-driven monolith (Meta), public platform API (GitHub, Shopify), federated graph of many teams (Netflix, Hemnet), and central graph hosting business logic (Airbnb). Three of them are JVM-heavy, which makes them the closest models for this course.

| Company | Why GraphQL | How they run it | Stack | Takeaway | Taught in |
| --- | --- | --- | --- | --- | --- |
| [Meta (Facebook)](https://engineering.fb.com/2015/09/14/web/graphql-a-data-query-language/) | 2012 native News Feed rewrite; the feed API only returned HTML | One graph for product apps; Relay on clients; by 2015 it served almost all mobile data at millions of requests per second across about 1,000 shipped app versions | In-house server, Relay | Design the API from the client's view; old app versions live for years | M1, M7, M12 |
| [GitHub](https://github.blog/2016-09-14-the-github-graphql-api/) | Integrators needed 2–3 REST calls per view; REST drove over 60% of database-tier requests | First large public GraphQL API (2016), one API for internal and public use, Relay on the front end | Ruby (graphql-ruby) | Public APIs need cost-based limits and generated docs | M1, M10, M16 |
| [Shopify](https://www.shopify.com/partners/blog/all-in-on-graphql) | Large Admin API surface, efficient data access, typed tooling | REST Admin API marked legacy in October 2024; new public apps must use GraphQL only from 1 April 2025; doubled rate limits and cut connection costs 75% | Ruby, cost-based throttling, dated API versions | Treat the API as a product: versions, budgets, migration guides | M10, M16 |
| [Netflix](https://www.infoq.com/articles/federated-GraphQL-platform-Netflix) | One Studio API owned by many teams | Federated graph with 200+ services; several graphs (Studio, Enterprise, consumer); authorization owned by each subgraph team | Java, Spring Boot, graphql-java, DGS (open-sourced 2021, now on Spring for GraphQL) | Invest in a paved path (framework, codegen, tracing) before mass migration | M6, M15, M16 |
| [Airbnb (Viaduct)](https://airbnb.tech/data/viaduct-five-years-on-modernizing-the-data-oriented-service-mesh/) | Fragmented microservices, duplicated logic | One central schema; teams host business logic inside it and compose via GraphQL re-entrancy; 130+ teams, 1.5M+ lines, traffic up 8× since 2020; open-sourced 2025 | JVM (Kotlin), Gradle-based, open source | A central graph can be a platform, not just a thin gateway | M14, M18 |
| [Expedia Group](https://github.com/ExpediaGroup/graphql-kotlin) | Two sources of truth (schema and code) | Code-first schemas generated from Kotlin, federation support, typed clients | Kotlin on graphql-java (graphql-kotlin, 2018) | Code-first works well when the language's types map cleanly to GraphQL | M3, M6, M15 |
| [PayPal](https://developer.paypal.com/community/blog/graphql-a-success-story-for-paypal-checkout/) | Each round trip cost at least 700 ms at p99 in Checkout (verified on PayPal's blog, 2026-11-24) | REST → batch REST → GraphQL; product teams own BFF GraphQL APIs; grew from 3 to 52 products in 2 years ("Scaling GraphQL at PayPal" on Medium; not verified: Medium refuses automated fetches) | Node.js, Apollo Server | GraphQL shines as a product-team BFF over core services | M1, M15 |
| [Atlassian](https://github.com/atlassian-labs/nadel) | One API across many products' services | Nadel gateway: explicit exposed schema, hydration across services, batching, field and type renames | Kotlin/JVM | Gateway-side hydration is an alternative to federation | M15 |
| [Hemnet](https://the-guild.dev/graphql/hive/case-studies/hemnet) | Split a single-entity GraphQL backend safely | Moved to federation on Hive from November 2025; canary routing by operation name at the edge; gateway ran at under 30% of the previous router's resources | Ruby legacy API, Hive Gateway | Migrate infrastructure and domain boundaries in separate steps | M15, M16 |

## Patterns across the stories

- **Start where the pain is:** mobile latency (Meta, PayPal) or API sprawl (GitHub, Netflix), not "GraphQL everywhere"
- **Tooling decides adoption:** DGS at Netflix, Viaduct's tenant API at Airbnb, generated docs at GitHub
- **Ownership follows the graph:** federated teams own their types and their authorization
- **Public APIs need economics:** cost budgets and versioned release cycles (GitHub, Shopify)
- **Migrate in slices:** canary by operation (Hemnet), REST and GraphQL side by side for years (Shopify)

## Classroom activity: case-study teardown

Teams draw one company's architecture from its write-up, then present: what problem drove it, what they would copy for Shelfie, and what they would not. The class votes on the most transferable lesson.

As of November 2026; figures are as reported in each company's own write-up or interview.
