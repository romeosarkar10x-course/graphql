## What changes in the schema

<!-- Types and fields added, changed, deprecated or removed. Paste the `bin/course design diff` output. -->

## Breaking changes

- [ ] None, or each one is listed here with its usage evidence below
- [ ] Every removed field was deprecated (with a reason that names the replacement) for at least 90 days

## Usage evidence

<!-- For every removed or changed field: the registry query, the window (at least 30 days), and which clients
     and versions used it last. "Nobody uses it" needs numbers. -->

- [ ] Usage checked over at least 30 days, including subscriptions and persisted documents

## Deprecations

- [ ] New `@deprecated` fields have a reason naming the replacement and an earliest removal date

## Style

- [ ] Names follow the style guide (camelCase fields, PascalCase types, no abbreviations)
- [ ] Every new type and field has a description
- [ ] Lists that can grow are paginated or capped (`first`)

## Ownership

- [ ] Each touched type's owner team (CODEOWNERS) has reviewed
- [ ] New types have an owner line in `.github/CODEOWNERS`

## Clients

- [ ] Changelog entry written (`CHANGELOG.md`), with a migration note for anything breaking
