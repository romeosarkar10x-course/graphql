# Rubric, lab 12.6 (optional; pass: 7 of 10)

| Points | Criterion |
|---|---|
| 3 | A working Relay version of the same screen (fragments per component, a mutation through `commitMutation`) and the real output of `bin/course web run measure`. Invented counts score 0. |
| 4 | Each count explained: why the props version re-renders everything, what `memo` relies on (structural sharing, `===`), and why masked fragments re-render only the component whose fields changed. |
| 3 | The cost of Relay (compiler, naming, global ids, tooling) against Apollo with masking, and a reasoned pick for Shelfie. |
