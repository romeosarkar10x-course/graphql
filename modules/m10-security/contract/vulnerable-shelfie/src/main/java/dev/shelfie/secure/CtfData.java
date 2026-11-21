package dev.shelfie.secure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** What the fork adds to the course dataset: a hidden reader, notes, audit logs, passwords, a SQL table. */
@Component
public class CtfData {
  final Map<String, String> welcomeNotes = new HashMap<>();
  final Map<String, List<String>> clubLogs = new HashMap<>();
  final Map<String, String> passwords = new ConcurrentHashMap<>();

  CtfData(Store store, JdbcTemplate sql) {
    store.readers.put("r41", new Reader("r41", "ghost", "Deactivated reader", Flags.of("enumeration") + "@shelfie.test", List.of("member")));
    store.addReview("c3", "b7", 5, "Ben's members-only note: " + Flags.of("node-leak"), "r2");
    for (Club c : store.clubs.values()) welcomeNotes.put(c.id(), "Welcome to " + c.name() + "!");
    welcomeNotes.put("c1", "Welcome, night owl. Tonight's password is " + Flags.of("brute-force"));
    for (Club c : store.clubs.values()) clubLogs.put(c.id(), List.of("created " + c.id() + " for " + c.ownerId()));
    clubLogs.put("c2", List.of("created c2 for r1", "root: rotated the admin recovery code to " + Flags.of("role-bypass")));
    for (Reader r : store.readers.values()) passwords.put(r.username(), UUID.randomUUID().toString());
    passwords.put("ben", "ben-pw");   // the player account

    sql.execute("create table book (id varchar(10) primary key, title varchar(200))");
    sql.execute("create table vault (name varchar(40), secret varchar(200))");
    for (Book b : store.books.values()) sql.update("insert into book values (?, ?)", b.id(), b.title());
    sql.update("insert into vault values ('backup-key', ?)", Flags.of("sql-injection"));
  }
}
