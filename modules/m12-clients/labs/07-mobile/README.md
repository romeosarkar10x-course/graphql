# Lab 12.7: Mobile clients (optional, overview track)

Lecture: [12.7 Mobile clients](../../lectures/12.7-mobile-clients.md). Grading: rubric. Needs the reference
server, Toxiproxy and internet (Gradle downloads Apollo Kotlin once).

A phone-like client on the JVM with **Apollo Kotlin 5.2**: generated Kotlin models, and a normalised cache in
SQLite that survives app restarts. **Gradle, not Maven, in this lab only:** Apollo Kotlin's code generator is a
Gradle plugin, and the only Maven plugin targets Apollo 3.8 (2023). `bin/course mobile` runs the official
Gradle 9.9 image; you install nothing. No Android SDK or emulator: lecture 12.7 covers what changes on a phone.

```bash
bin/course start m12/07
cd workspace/m12-clients/07-mobile/mobile
bin/course mobile gradle -q installDist     # codegen + compile
bin/course mobile ./airplane.sh             # the experiment
```

`airplane.sh` runs the app as separate processes (each one an app restart) through a Toxiproxy proxy it
switches off and on:

1. `online`: the club card from the network, stored in `shelfie.db`.
2. Airplane mode on. `online` fails; `offline` (`FetchPolicy.CacheOnly`) still shows the card.
3. `review b1 1 ...`: **you write it.** The network is unreachable, so the review goes in an outbox.
4. Airplane mode off. `sync`: **you write it.** Replay the outbox, then show the card *from the cache*. It
   must show the new average Shelfie computed, which takes one more thing: cache ids for Shelfie's types.

In `notes.md`: the run, what made the cache answer, how your outbox behaves, and what changes on Android.

## Done when

`notes.md` passes the rubric (`bin/course grade m12/07`). This lab is optional: it is not in the checkpoint.
