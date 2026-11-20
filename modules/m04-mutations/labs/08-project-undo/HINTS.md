# Hints, mini-project 4

1. A command must remember enough to reverse itself *without looking at the current state*: a rename stores both the old and the new name; a delete stores the whole review. `inverse()` of a rename is a rename the other way; of a join, a leave; of a create, a delete.
2. `conflict(state)` asks "is the data still what I left?": for a rename, is the name still my `to`? For a join, is the user still a member? For a create, is my review still there, unchanged? Undo applies the inverse only when that holds.
3. One lock around "look up token, check conflict, apply inverse, mark used, store the redo command" keeps two undos of the same token from both succeeding. The redo token is just `run(command.inverse())`.
