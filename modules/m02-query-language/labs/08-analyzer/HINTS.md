# Hints, lab 2.8

1. Count first, expand later: write `measure` with `new NodeTraverser().preOrder(visitor, op)` and a `NodeVisitorStub` that overrides `visitField`. `depthFirst` calls your visitor twice per node (on the way in and on the way out); `preOrder` calls it once.
2. Depth: `context.getParentNodes()` lists the nodes above the current one. Count the `Field`s among them and add one. For `expand`, `new AstTransformer().transform(op, visitor)` with `return TreeTransformerUtil.changeNode(context, newNode)` in `visitFragmentSpread`.
3. Two traps. The transformer walks into the nodes you insert, so a cycle never ends: check "fragment A spreads B spreads A" with a depth-first search over the definitions first. And the traverser visits each node *instance* once: if two spreads share the definition's `SelectionSet` object, the second copy is not counted. Use `getSelectionSet().deepCopy()`.
