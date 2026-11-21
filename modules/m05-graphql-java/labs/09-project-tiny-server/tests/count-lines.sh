#!/usr/bin/env bash
# Lines that count: not blank, not a comment, not package or import. Data.java is provided and not counted.
find "${1:-src/main/java}" -name '*.java' ! -name Data.java -print0 | xargs -0 cat \
  | grep -vE '^\s*$|^\s*(//|/\*|\*)|^\s*(package|import)\s' | wc -l
