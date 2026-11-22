# Hints: 16.9 governance council

1. For each proposal, answer two questions before you debate: is it breaking (`bin/course design diff` on a
   sketch), and who uses what it touches (the usage lines)? Most decisions follow from those two answers.
2. "Reject" is rarely the whole answer. A breaking change often has an additive twin: a new field next to the
   old one, then a deprecation, then a removal decided by data. An additive change can still be the wrong shape,
   and a public shape is hard to change later.
3. Conditions make a decision enforceable. Name an owner and a date for each, and say what data would reopen
   the decision. The style rules that apply here: money, unbounded lists, and implicit units.
