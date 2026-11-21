# Rubric, lab 9.6 notes (pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | The table has the measured bytes, hit rates and latencies for all three modes. Invented numbers score 0. |
| 3 | Separates the two effects: the latency comes from GET plus Cache-Control (any cacheable GET is a hit); APQ's own gain is request size and skipping parse and validation. |
| 2 | Why APQ is not security: anyone can register any document, so it doesn't limit what runs; an allow-list of trusted documents does (Module 10). |
| 2 | At least one trap, explained: the cached PersistedQueryNotFound, the known hash with other text, or personal data in a public response. |
