# Lab 1.1 (reference)

## 1. Card F — draft spec + graphql-js (2015)
GraphQL stopped being "Facebook's server" and became a *specification* anyone could implement, with a
reference implementation to check behaviour against. That is why graphql-java exists at all, and why
Module 17 can ask "spec or library quirk?" and settle it by reading the spec.

## 2. Card C — relicensing (2017)
The OWFa spec licence and MIT graphql-js removed the patent-clause worry that kept some companies away.
Without it, the Java ecosystem this course stands on (graphql-java, Spring for GraphQL, DGS) would have
had far less corporate backing.

## 3. Card B — GraphQL Foundation (2018)
Neutral governance: spec changes now go through RFCs and working groups, which is why the course has an
RFC tracker and labels features like `@defer` and `onError` by RFC stage (Modules 8, 11, 18).

## 4. Card L — Apollo Federation (2019)
It gave many teams a way to share one graph without one team owning everything. Module 15 builds
Shelfie as four subgraphs on the same model, and the vendor-neutral Composite Schemas spec grew from it.

## One sentence
The 2015 spec release: without a public spec there would be no independent implementations, and GraphQL
would have stayed one company's internal tool like many others.
