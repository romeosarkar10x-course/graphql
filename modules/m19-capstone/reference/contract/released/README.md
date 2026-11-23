# Released schemas

The schemas as last released (1.0.0, 2026-11-23). `SchemaCompatibilityTest` (common) fails the build when
`contract/<subgraph>.graphqls` breaks a client of these: a removed field, a type change, a new required argument.
Copy the contract here when you release; a breaking change needs a deprecation first (Module 7.5).
