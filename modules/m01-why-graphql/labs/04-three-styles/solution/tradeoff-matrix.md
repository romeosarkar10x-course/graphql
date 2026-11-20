# Lab 1.4 trade-off matrix (reference; other picks can score full marks if argued well)

## 1. Public API for thousands of third-party developers
**Pick:** REST (GraphQL is an equally defensible answer)
**Because:** URL-addressable resources cache in every CDN and browser for free, and every language and
tool speaks it with no client library. Third parties cannot be allowlisted, so GraphQL would need cost
limits and budgets from day one (GitHub and Shopify both built them).
**Runner-up, and what would flip it:** GraphQL, if integrators routinely assemble views from many
resources (GitHub's reason: 2–3 calls per view, REST was over 60% of database requests).

## 2. Internal Java to Java, 20k calls/s, server streaming
**Pick:** gRPC
**Because:** binary Protobuf over HTTP/2 with generated stubs is cheap at high rates, and streaming is
first-class. Both ends are services the same organisation controls; no browser in the path.
**Runner-up, and what would flip it:** REST if the callers multiply into many teams and languages with
no appetite for `.proto` tooling, or if the calls must be inspectable with `curl`.

## 3. Mobile home screen, 6 widgets from 4 services, slow networks
**Pick:** GraphQL
**Because:** one round trip instead of a waterfall (lab 1.3: 834 ms vs 2,182 ms at 700 ms RTT), and the
client asks for exactly the fields each widget needs, so old app versions keep working as the screen
evolves.
**Runner-up, and what would flip it:** a REST backend-for-frontend endpoint per screen, if there is
exactly one client and the screen rarely changes.

## 4. TypeScript monorepo, one team, web only
**Pick:** tRPC
**Because:** end-to-end types inferred straight from server functions, no schema or codegen step, and no
other consumer exists to need a language-neutral contract.
**Runner-up, and what would flip it:** GraphQL or REST with OpenAPI as soon as a non-TypeScript client
(mobile app, partner) appears.

## 5. Reporting backend for Excel and Power BI
**Pick:** OData
**Because:** Excel and Power BI speak OData natively, and its URL conventions (`$filter`, `$orderby`,
`$select`, `$top`) are exactly the ad-hoc table queries analysts make.
**Runner-up, and what would flip it:** REST with a small query language, if the BI tools change or OData's
server libraries are a poor fit for the stack.
