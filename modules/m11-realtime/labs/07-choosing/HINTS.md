# Hints: m11/07

1. For each feature ask four questions: how stale may it be, how often does it change, how many clients
   watch it at once, and who is the client (a person's screen or another server)?
2. Polling cost grows with clients × poll rate, even when nothing changes; subscription cost grows with
   sockets held plus subscribers × events. Put your lab 11.5 numbers next to Shelfie's peak (2,000 online).
3. A good pitch names the runner-up's best argument before the sceptic does, and answers it with a number
   or a failure mode, not with "it's more modern".
