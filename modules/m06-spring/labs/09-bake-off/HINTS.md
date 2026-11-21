# Hints, lab 6.9

1. Nearest rank: the p-th percentile of n sorted values is the value at position ceil(p/100 x n), counting
   from 1. Convert nanoseconds to milliseconds at the end.
2. `/proc/<pid>/status` has a line like `VmRSS:   598016 kB`. `Process.pid()` gives the pid, as long as
   the command starts `java` directly (not through a shell script).
3. Before comparing RSS, decide whether to pass the same `-Xmx` to every app; `bake.sh` passes none.
