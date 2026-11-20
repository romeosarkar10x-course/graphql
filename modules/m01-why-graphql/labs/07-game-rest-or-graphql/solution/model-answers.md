# Model answers (grader reference; other picks can win if the trade-off is real)

1. **REST/CDN.** Identical public data: URL caching at the edge beats any query flexibility.
2. **GraphQL.** Polymorphic feed (unions), client picks fields per item type, one round trip on mobile.
3. **Webhook.** Push to a URL the merchant owns; they are not connected to you.
4. **gRPC.** Internal, high-rate, binary, generated stubs; no browser.
5. **REST** (or whatever exists). Simple CRUD, one team: GraphQL's costs buy nothing.
6. **GraphQL.** Old clients live long; the client chooses fields, so the server can evolve without versions.
7. **OData** (or CSV/REST). Excel speaks OData natively; ad-hoc filtering by URL.
8. **tRPC.** One language, one team: inferred types with no schema step.
9. **GraphQL (federated).** Many owners, one product view; federation (Module 15) lets each team own its part.
10. **None: file/CDN or REST with a signed URL.** Binary, huge, resumable download: not a JSON API's job.
11. **GraphQL subscription or WebSocket** (either is fine); a webhook cannot reach a phone.
12. **GraphQL.** Field-level authorization and "ask for what you need" without new endpoints.
13. **gRPC streaming** (or MQTT). Many small binary messages, client streaming, internal.
14. **GraphQL.** One screen from several sources on a slow network; partial results if one source fails (Module 8.6).
15. **None: static file.** Build-time data; serve it with the page.
16. **GraphQL.** Same reason as 6: additive evolution, field-level usage tells you when old fields can go.
17. **REST + OpenAPI** or **gRPC**. Both have mature multi-language SDK generators; GraphQL codegen exists but SDK-as-product is OpenAPI's home turf.
18. **REST.** `GET /health` → 200. Load balancers cannot speak GraphQL bodies.
19. **GraphQL** (or a REST BFF). A BFF for one client is the textbook GraphQL case (PayPal); a hand-written REST BFF is also reasonable.
20. **GraphQL** (or SQL/OData). Ad-hoc questions over a graph of entities; internal users can be trusted with flexible queries.
