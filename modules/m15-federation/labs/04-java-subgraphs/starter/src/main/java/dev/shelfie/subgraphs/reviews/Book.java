package dev.shelfie.subgraphs.reviews;

/** Reviews' view of a book: only its key. Catalog owns the rest. The class name is the GraphQL type name. */
record Book(String id) {}
