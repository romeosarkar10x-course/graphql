# Fake Open Library

Recorded on 2026-11-22 from openlibrary.org (`_workbench/2026-11-22-m14-probe/openlibrary/`), served offline so
tests never depend on someone else's API, and so they can break it on purpose.

```bash
java -cp "$(source /course/tools/testlib.sh; lib_cp)" FakeOpenLibrary.java <data-dir> [port]   # default 4500
```

Real endpoints it imitates:

- `GET /api/books?bibkeys=ISBN:a,ISBN:b&format=json&jscmd=data`: one object keyed by bibkey; unknown ISBNs are
  left out (an empty `{}` for none), exactly like the real API. Ten recorded books: the seed's ISBNs.
- `GET /search.json?q=...`: recorded for `le guin`, `ishiguro`, `piranesi`; anything else has no results.

Switches for tests (not part of the real API):

- `POST /_fake/mode?set=ok|down|slow|ratelimit|garbage[&ms=3000]`: `down` answers 503, `slow` waits `ms` before
  answering, `ratelimit` answers 429 with `Retry-After: 1`, `garbage` answers 200 with an HTML page.
- `GET /_fake/stats`: `{"requests": n, "bibkeys": [[...] per /api/books call], "searches": [...], "userAgents": [...]}`
- `POST /_fake/reset`: zero the stats and set mode `ok`.
