# Hints: 17.9 build from scratch

1. Work in the order the data flows: lexer, parser, schema checks, validation, execution, introspection. Labs 17.2
   to 17.6 test each part on its own, with the rest taken from the reference engine.
2. Lines go fastest in the parser and the executor. Small helpers (`expect`, `many`, `optionalMany`; one
   `complete` with a switch on the type) save more than clever code. Records and switch pattern matching keep the
   AST code short.
3. If you are over 2,000 lines, look for code you wrote twice: literal and variable coercion share most of their
   rules, and printing types for introspection and for SDL is the same function.
