package dev.shelfie.secure;

import graphql.schema.DataFetchingEnvironment;
import java.util.*;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
public class CtfController {
  private final Store store;
  private final Viewer viewer;
  private final CtfData data;
  private final JdbcTemplate sql;
  private final CtfSecurity.Keys keys;

  CtfController(Store store, Viewer viewer, CtfData data, JdbcTemplate sql, CtfSecurity.Keys keys) {
    this.store = store;
    this.viewer = viewer;
    this.data = data;
    this.sql = sql;
    this.keys = keys;
  }

  @QueryMapping
  public List<Book> searchBooks(@Argument String titleContains) {
    // Blue team: a bound parameter. The text is data, never SQL.
    return sql.query("select id, title from book where title like ?",
        (rs, i) -> new Book(rs.getString(1), rs.getString(2)), "%" + titleContains + "%");
  }

  @QueryMapping
  public Review review(@Argument String id) {
    Review r = store.reviews.get(id);
    return r != null && visible(r) ? r : null;
  }

  @QueryMapping
  public String adminSecret() { return Flags.of("alg-none"); }

  @QueryMapping
  public String staffNotes() {
    Reader me = viewer.require();
    if (!me.roles().contains("admin")) throw new Denied(ErrorType.FORBIDDEN, "staff only");
    return "Staff notes: " + Flags.of("mass-assignment");
  }

  @QueryMapping
  public List<String> auditLog() { return List.of("2026-11-01 root signed in", "2026-11-02 root signed in"); }

  @SchemaMapping
  public String welcomeNote(Club club) {
    Reader me = viewer.current();
    return me != null && store.clubs.get(club.id()).memberIds().contains(me.id()) ? data.welcomeNotes.get(club.id()) : null;
  }

  @SchemaMapping
  public List<String> auditLog(Club club) {
    Reader me = viewer.current();
    boolean allowed = me != null && (me.id().equals(club.ownerId()) || me.roles().contains("admin"));
    return allowed ? data.clubLogs.get(club.id()) : null;
  }

  private boolean visible(Review r) {
    Club c = store.clubs.get(r.clubId());
    Reader me = viewer.current();
    return !c.isPrivate() || (me != null && c.memberIds().contains(me.id()));
  }

  @MutationMapping
  public String login(@Argument String username, @Argument String password) {
    Reader r = store.byUsername(username);
    if (r == null || !password.equals(data.passwords.get(username))) return null;
    return keys.sign(r);
  }

  @MutationMapping
  public Reader updateProfile(@Argument Map<String, Object> input) {
    Reader me = viewer.require();
    // Blue team: only the fields a reader may change are in the input type, and only they are copied.
    String name = (String) input.getOrDefault("name", me.name());
    Reader updated = new Reader(me.id(), me.username(), name, me.email(), me.roles());
    store.readers.put(me.id(), updated);
    return updated;
  }

  @MutationMapping
  public String grantBadge(@Argument String readerId, DataFetchingEnvironment env) {
    // Blue team: "came with a hash" is not a permission. Check who is asking.
    Reader me = viewer.require();
    if (!me.roles().contains("admin")) throw new Denied(ErrorType.FORBIDDEN, "admins only");
    return "badge granted to " + readerId + ": " + Flags.of("apq-trust");
  }

  /** A bot posts to the private club c3 every second (and tidies up after itself). */
  @Scheduled(fixedRate = 1000)
  void bot() {
    Review r = store.addReview("c3", "b9", 4, "Bot note for c3 members: " + Flags.of("websocket"), "r2");
    store.reviews.remove(r.id());
  }
}
