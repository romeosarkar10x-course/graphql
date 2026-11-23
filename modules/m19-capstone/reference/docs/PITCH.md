# Pitch: Toolshed

**One line:** a community tool library's catalogue and lending desk, as one GraphQL API for a web page, a
librarian's command line and, later, a phone app.

## The problem

A neighbourhood tool library lends drills, ladders and hedge trimmers from three branches. Today the
catalogue is a spreadsheet, loans are a paper book at each desk, and members phone to ask whether the mitre
saw is in. Two members have turned up for the same pressure washer on the same Saturday. Overdue tools are
chased from memory.

## Who uses it

- **Members** browse the catalogue by category and branch, see whether a tool is on the shelf right now,
  borrow it for up to two weeks and see their own loans.
- **Librarians** scan a barcode at the desk to see who has a tool, take returns, and run a weekly overdue
  report.
- **Anyone** can browse without signing in.

## What it must get right

- **One tool, one borrower:** two people racing for the last drill cannot both win, and a borrow retried
  after a dropped connection does not lend twice.
- **Privacy:** members see that a tool is out, not who has it, and never another member's email.
- **Honest failures:** "someone else has it until Friday" or "you already have three tools out" are answers the
  page shows, not errors.
- **Live state:** the "on the shelf" badge changes when a tool goes out or comes back, without a refresh.

## Why GraphQL and two subgraphs

The screens mix catalogue data (names, specs, branches) with lending data (availability, loans, members). One
graph serves each screen in one round trip. The catalogue and lending change for different reasons and at
different rates, so they are separate subgraphs behind one router (ADR 0001).

## Out of scope

Payments and deposits, reservations for a future date, repairs, and more than one library.
