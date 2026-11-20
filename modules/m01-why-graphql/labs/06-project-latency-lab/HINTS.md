# Hints, mini-project 1.6

1. `waves` is a depth-first search with memoisation: the wave of a call with no dependencies is 1. Track the calls on the current path in a `Set` to detect a cycle.
2. `roundTrips`: count how many calls share each wave number, then add `ceil(count / maxParallel)` per wave. Integer ceiling division: `(n + p - 1) / p`.
3. For the page, an `<input type="range">` fires `input` events while it moves; recompute both times in one `render()` and set each bar's `style.width` as a percentage of the larger time. A logarithmic bandwidth slider makes the slow end usable.
