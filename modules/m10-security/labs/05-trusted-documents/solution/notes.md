# Trusted documents: notes (reference)

## The replay: what the captured query got before and after

The captured Home query, edited to add `email` to members, sent ad hoc (T05). Before (lab 10.4's server):
it ran, at cost 230, and returned every member's name; emails came back null only because of lab 10.2's
field rule. Any field without such a rule would have leaked, and the attacker could shape the query freely
(within the demand limits). After: `not a trusted document`, code `NOT_TRUSTED`, no `data`, no resolver
ran and nothing was parsed beyond the hash check. Sending the exact original text (T04) still works,
because its SHA-256 is in the manifest.

## APQ is not trust: what T07 would have done on lab 9.6's server

Automatic persisted queries register any document whose hash matches its text, so T07's
`{ readers(first: 40) { email } }` with its own correct hash would have been stored and run, and the hash
alone would run it from then on (T08). APQ saves bandwidth; it says nothing about who wrote the document.
Here the manifest is fixed at build time: an id that is not in it is refused even with the right text,
and a known id with other text is refused (T06) instead of running either one. The vulnerable fork's
`grantBadge` (lab 10.9) is the same mistake: "persisted" checked as "came with a hash".

## Who can still run an ad-hoc query, and how

Nobody in production. Developers run with `shelfie.trusted-documents.enforce=false` locally; introspection
stays off even then (`spring.graphql.schema.introspection.enabled: false`), because clients are built against
the schema file, not a live server. Third parties cannot be allowlisted this way: their documents are not
in our build, which is why lecture 10.6 uses keys, scopes and cost budgets for a public API instead.
Trusted documents also do not replace authorization: a trusted `Club($id)` still has to check that the
caller may see that club, because the attacker chooses the variables.

## What changes for the client team

Every new or edited operation needs a client build and a server deploy that knows the new manifest, in
that order: deploy the server with the new ids first, then ship the client. Old ids must stay in the
manifest until no deployed client uses them (mobile apps live for months). The win: the server knows
every operation in advance, so it can cost, test and cache each one, and parsing is skipped for them.
