# Hints, mini-project 8

1. Keep the cursor from the last page you loaded (`page.endCursor()`), not a count. With new books
   arriving on top, "the next 20 after this book" is the only question that has a stable answer.
2. Put all movement in one method: `move(by)` computes the target index, loads pages while
   `more && target + 5 >= books.size()`, then clamps the selection to the loaded books. `pageDown()` is
   `move(rows)`, `home()` is `move(-selected)`.
3. Scrolling is two comparisons after every move: if `selected < top`, set `top = selected`; if
   `selected >= top + rows`, set `top = selected - rows + 1`. Draw rows `top` to `top + rows - 1`.
