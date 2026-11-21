# Game 10.10: Red team / blue team

Lectures: all of Module 10. Grading: auto. Needs nothing. Play it after the capture the flag (lab 10.9):
`tests/` holds the exploit for every flag.

You were the red team in lab 10.9. Now be the blue team: the starter is the vulnerable fork's source
(`contract/vulnerable-shelfie`). Patch it so that every exploit fails and Shelfie still works.

```bash
bin/course start m10/10
bin/course test m10/10      # 13 exploits (12 flags and the CSRF chain), then 8 functional checks
```

Rules: keep the schema's public surface (sign in, clubs, search, joining with a code, profiles, reviews,
`review(id:)`). Removing a field that only existed to leak (or an input field nobody should set) is
fair; removing `searchBooks` is not. Each fix gets a one-line `// Blue team:` comment saying what it
closes. The server must also survive the suite: it runs with a 512 MB heap.

## Solo

Close all 13. Then re-attack your own server with lab 10.3's attacks and graphql-cop (lab 10.8).

## Group (pairs)

1. Each player patches their own fork for 45 minutes (the suite stays hidden: don't run `bin/course test`).
2. Swap servers (`bin/course shell`, then share the fork's port through the course network, or swap
   workspace folders) and attack each other's patched server for 30 minutes. Each flag still captured
   scores 2 for the attacker.
3. Patch what was found (15 minutes), re-attack (15 minutes), then run `bin/course test m10/10` on both.
Record the results in `games/leaderboards/red-blue.md`.
