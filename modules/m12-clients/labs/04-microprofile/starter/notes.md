# MicroProfile clients: notes

## The same call, twice

<!-- Paste both versions side by side (the interface and the builder). Which reads better, and for whom: the
     person writing it, the reviewer, the person debugging a 2 a.m. failure? -->

## What each one sent

<!-- Run both through contract/Capture.java and paste what went over the wire. What does the typesafe client
     decide for you (selection set, variable types, operation name), and what went wrong the first time? -->

## Typesafe here, generated in 12.3, document files in 12.2

<!-- Where does each approach keep the "source of truth" for the selection set, and what does a schema rename
     do to each? -->
