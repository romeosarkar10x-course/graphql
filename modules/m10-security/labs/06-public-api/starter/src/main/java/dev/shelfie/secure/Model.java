package dev.shelfie.secure;

import java.util.List;

// The four Node types. Record names match the GraphQL type names, so Spring resolves Node by class name.
record Reader(String id, String username, String name, String email, List<String> roles) {}

record Club(String id, String name, boolean isPrivate, String ownerId, List<String> memberIds, String inviteCode) {}

record Book(String id, String title) {}

record Review(String id, int rating, String body, String authorId, String bookId, String clubId) {}
