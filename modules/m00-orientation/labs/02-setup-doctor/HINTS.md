# Hints, lab 0.2

1. Each check is a small method returning a `Result`. Start with `jq`: how do you start an external program from Java and read its exit code? Look at `java.lang.ProcessBuilder`.
2. When a program is not on `PATH`, `ProcessBuilder.start()` does not return a failed process: it throws. Which exception, and what should your check return when it is thrown? For the server, `java.net.http.HttpClient` throws for "connection refused" too.
3. Read the base URL from `System.getenv("SHELFIE_URL")` with a default of `http://shelfie-ref:8080`, GET `/actuator/health`, and treat both an exception and a non-200 status as FAIL. At the end, count FAIL results and pass that to `System.exit`.
