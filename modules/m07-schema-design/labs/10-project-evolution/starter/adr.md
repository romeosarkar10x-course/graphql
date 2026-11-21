# ADR-007: How Shelfie's schema evolves

Status: accepted · Date: · Deciders:

## Context

What the six change requests asked for, and the constraint: deployed apps never update.

## Decision

The rules you followed in every step (additive first, how you name replacements, what a deprecation
reason must say, when something may be removed). One paragraph per rule.

## The six steps

| Step | Request | What you added | What you deprecated | Riskiest choice |
|---|---|---|---|---|

## Consequences

What this policy costs (schema size, duplicate fields, slower clean-up) and what it buys. Which
deprecated field is still there after step 6, and what has to happen before it can go.

## Alternatives considered

At least two (for example: `/v2` endpoint, changing `rating` to `Float` in place, a `Rating` object), and why
each lost.
