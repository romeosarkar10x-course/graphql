# Hints, lab 3.3

1. Write `normalise` once and call it from all three methods; the bugs in the starter come from three methods each doing their own thing. Check the type first (`instanceof String`, `instanceof StringValue`) and reject the rest.
2. ISBN-13 check digit: weights 1,3,1,3... over the first 12 digits; check = (10 - sum % 10) % 10. ISBN-10: weights 10 down to 2 over the first 9; check = (11 - sum % 11) % 11, where 10 is written `X`. Converting 10 to 13: prefix `978` to the first 9 digits and compute a new ISBN-13 check digit.
3. `Character.isDigit('٣')` is true, and so is `\d` with some regex flags. "ASCII digits" means `c >= '0' && c <= '9'`. Note `String.valueOf(map)` turns an object into a string your stripping code may happily accept: row 14 of the starter's table.
