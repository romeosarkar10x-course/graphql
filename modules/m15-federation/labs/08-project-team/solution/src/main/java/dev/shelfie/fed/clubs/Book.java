package dev.shelfie.fed.clubs;

/** Clubs' view of a book: its id, and the page count the router brings along for @requires. */
record Book(String id, Integer pages) {}
