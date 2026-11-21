# Hints: m11/09

1. Check a move in a fixed order, cheapest and most informative first: is this a player, is the game
   over, is there an opponent, is it their turn, is the cell on the board, is it free. After placing the
   mark, look for three in a row of that mark only.
2. `Flux.startWith(snapshot)` puts the current board in front of the updates; `Sinks.Many.emitComplete`
   ends every subscriber's stream when the match ends; for a match that is already over, return
   `Flux.just(final board)`.
3. Count a spectator in `doOnSubscribe` and release in `doFinally` (which runs when a client just goes
   away), and only when `as` is not one of the two players. Send snapshots (an immutable copy of the board),
   never the live array, or an event can change after it was sent.
