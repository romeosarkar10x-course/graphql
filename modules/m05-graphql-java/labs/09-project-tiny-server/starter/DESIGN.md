# DESIGN.md — the tiny server

## Decisions

Answer each in a few sentences. Point at the lines in your code.

1. **Structure.** How did you fit in 150 lines? What did you leave out that a production server needs?
   List at least four things, each with the module that adds it.
2. **Status codes.** Show the expression or method that picks the status code. Which spec rule was the
   hardest to get right, and why?
3. **Errors.** What does a client see when a fetcher throws? What does the log see?
4. **Threads.** Which executor serves requests, and what happens under 1,000 concurrent slow requests?
5. **Compared with lab 5.4.** What did you copy, what did you rewrite, and would you keep the
   HTTP handling separate from the wiring in a real project?
