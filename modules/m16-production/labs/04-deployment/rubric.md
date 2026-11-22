# Rubric: 16.4 deployment (DEPLOY.md, the manifest)

Pass mark: 6 of 10.

| Points | Criterion |
| --- | --- |
| 3 | A table of at least two measured runs (dropped, duplicates, failed queries), each run a different version |
| 3 | Explains each change by the mechanism: resume for messages published during a reconnect; readiness and drain for the balancer's delay; something for kept-alive connections or in-flight requests |
| 2 | The grace period in the manifest is justified by the app's own shutdown times (drain, shutdown phase) |
| 2 | Native image and CRaC: a reasoned yes or no for this service, naming at least one cost of each |
