# Proposal 1: add Book.price (catalog team)

**Change.** `Book.price: Float` in euros, so the web shop can show prices. Additive.

**Why.** A partner bookshop integration (client `partner-bulk`) will sell through Shelfie from Q1.

**Usage today.** `partner-bulk` 1.0.0 sends `Catalog { books(first: 10) { id title isbn13 year pages } }`,
65 operations in the last 7 days (lab 16.2's replay). Nobody selects prices yet; the field does not exist.

**Open questions from the review thread.** Currency is fixed to euros. Floats round. Prices differ per shop.
