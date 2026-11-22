package dev.shelfie.fed.clubs;

import dev.shelfie.fed.Data.User;

record BookOfTheMonth(String month, Book book, User pickedBy) {}
