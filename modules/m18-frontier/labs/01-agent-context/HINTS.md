# Hints: 18.1 Ten operations an agent can read

1. Read each operation as a stranger would: you know only its name, its variables and your description. What would
   you have to guess? Every guess is a sentence the description is missing.
2. Check the schema's defaults: `books(first: Int = 20)`, `members(first: Int = 10)` and
   `meetups(upcomingOnly: Boolean = true)` all hide data without saying so in the result. A question about the
   past is where an agent trips.
3. For the report, compare the two runs question by question: for each query that changed between runs, find
   the sentence in your descriptions the model must have used, and for each query that still failed, the sentence
   that was missing.
