# Hints: 17.2 lexer

1. Keep the source as code points (`source.codePoints().toArray()`), not chars: columns count code points, and
   a character outside the BMP is one code point but two chars. One position index, one line, one line start.
2. Every lexical rule decides on the next one or two code points: `.` must be `...`, `"` may start `"""`, a `-`
   or digit starts a number. After a number, look at the next code point for the "may not be followed by" rule.
   A CR followed by LF is one line terminator.
3. For block strings, collect the raw text between the quotes with only `\"""` resolved, then apply
   `BlockStringValue()` step by step: split on all three line terminators, find the common indent from lines 2
   on that are not all white space, remove it from every line but the first, drop leading and trailing blank lines,
   join with LF.
