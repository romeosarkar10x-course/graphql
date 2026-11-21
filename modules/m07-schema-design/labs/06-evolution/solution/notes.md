# Lab 7.6 notes

## 1. Where you guessed wrong

(Reference notes: the guess column in this copy is the tool's answer, so this section explains the
traps instead.) The usual misses:
- 08, `Book` to `Book!` on an output field: SAFE. Clients already handle a value; they get one more
  guarantee. The same change on an **input** or an argument is the opposite: BREAKING (05).
- 09, `String!` to `String` on an output field: BREAKING. Generated client types said non-null; a null
  now crashes them.
- 06, a new optional argument: SAFE for SchemaDiff. Old documents don't send it and the default
  behaviour is unchanged.
- 15, a changed default: DANGEROUS. No document becomes invalid, but every client that relied on 10
  now gets 20 results.

## 2. Where the tools disagree

- 06: inspector says dangerous (⚠ "Argument author: String added"), SchemaDiff says nothing. Inspector
  is cautious because an argument can change what the field returns.
- 07: deprecation is DANGEROUS in SchemaDiff (DEPRECATION_ADDED) and safe in inspector. Nothing breaks
  on deprecation; SchemaDiff flags it as the start of a removal.
In CI I'd run both and fail only on breaking from either: they agree on every breaking change here, and
their dangerous lists are both worth a reviewer's look.

## 3. The deprecation lifecycle for `Club.description`

1. Ship `about` next to `description` (additive, SAFE) and mark `description` `@deprecated(reason: "Use
   about: it supports Markdown.")`: the gate shows it as DANGEROUS, which is the point.
2. Log field usage per client (operation name or client name header) for `Club.description`. Module 16
   adds real field-usage metrics; until then, the trusted-documents list (Module 10) tells you which
   operations select it.
3. Tell the client teams; give a date (GitHub's schema writes it into the reason: "Removal on
   2027-01-01 UTC").
4. When usage has been zero for a full client release cycle (mobile apps stay installed for months), remove
   it. That release is BREAKING by definition; the gate fails, and the reviewer approves it on the usage data.
5. RFC #1231's rule keeps you honest along the way: with every deprecated element removed, the schema
   must still be valid, so `about` must exist before `description` is deprecated.

## 4. Breaking, dangerous, safe

Breaking: an operation that was valid stops being valid, or a response that was valid for its declared
type stops being so (a null in a once non-null field). Dangerous: every operation stays valid, but
results change meaning: a new enum value hits a client's exhaustive `switch`, a new union member comes
back as an empty object, a default changes. A "safe" change that still breaks clients: lab 7.5's new
`BookFinished` implementation of the `FeedItem` interface. SchemaDiff calls it SAFE (it only adds a type),
yet a client that maps `__typename` with an exhaustive `switch` and no default throws on the first
`BookFinished` item. Validity is the server's contract; clients can depend on more than validity.
