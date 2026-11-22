# Game 17.11: Mock working group

Lecture: [17.1 Reading the spec](../../lectures/17.1-spec.md). Grading: rubric.

The GraphQL spec changes through RFCs that a champion brings to the Working Group, stage by stage (strawman,
proposal, draft, accepted; graphql-spec's `CONTRIBUTING.md`). You have just built an engine, so you know what a
change costs. Propose one, small, and take it through a mock meeting.

Pick one, or bring your own of the same size:

1. **`Float` literals without a leading digit** (`.5`): today a syntax error.
2. **Allow `@skip` and `@include` on a subscription's root field** when the condition is a literal.
3. **An `Int` range directive**, `@range(min:, max:)`, applied to arguments and input fields.
4. **Report every NoFragmentCycles error the same way**: one per cycle, located at the first spread.

Write `RFC.md` (the starter has the headings):

- the problem, with a document that shows it, and why it cannot be solved without a spec change;
- the change as **spec edits**: the productions, the algorithm steps or the rule text, written the spec's way;
- the change in **your engine**: a diff to `contract/engine`'s code, or yours, and a test that shows it;
- compatibility: what existing documents, servers and clients it breaks or changes;
- alternatives you considered;
- the meeting: the questions it got, the objections, and the outcome (the stage it reaches, or rejected, and why).

## Solo

Play every seat: write the champion's case, then three objections as a skeptical implementer, a client author and
a spec editor would raise them, then answer each. Decide the outcome honestly: rejection is an outcome.

## Group (3 to 7 people, about 45 minutes)

Seats: a **champion** per RFC, a **facilitator** (keeps 12 minutes per RFC), **implementers** (one has the
engine open and tries the diff live), a **client author** and a **spec editor** who rules on wording. Each RFC gets
5 minutes of presentation, 5 of questions and 2 for the decision. The champion writes the outcome into their
`RFC.md` afterwards. Look at real agendas first: https://github.com/graphql/graphql-wg (agendas and notes).

```bash
bin/course start m17/11
bin/course grade m17/11
```
