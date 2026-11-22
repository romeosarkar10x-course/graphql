# Hints: 17.3 parser

1. One method per production, each starting at the current token, with `peek`, `expect(punct)` and
   `expectName()` helpers. A Description is a StringValue in front of a definition: read it, then look at the
   next token to decide what it describes.
2. Optional lists in the grammar (`FieldsDefinition`, `ArgumentsDefinition`...) are optional, but when present
   they need at least one item: `{}` is an error. For the fieldless form, the spec's `[lookahead != {]` means:
   when a `{` follows the type's header, it must be fields.
3. When a fuzz disagreement looks random, shrink it by hand: delete half, run both parsers on it, keep the half
   that still disagrees. Then compare with the grammar production for the smallest thing that still differs.
