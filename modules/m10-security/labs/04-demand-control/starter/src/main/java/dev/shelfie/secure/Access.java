package dev.shelfie.secure;

import org.springframework.stereotype.Component;

/**
 * Lecture 10.2: every authorization rule in one place, in the business layer. Resolvers, services and
 * the node(id:) entry point all ask here, so a new path to the same data cannot skip a rule.
 * Bean name "access" so @PreAuthorize can call it: @PreAuthorize("@access.ownsClub(#id)").
 */
@Component("access")
public class Access {
  private final Store store;
  private final Viewer viewer;

  public Access(Store store, Viewer viewer) {
    this.store = store;
    this.viewer = viewer;
  }

  static boolean isAdmin(Reader r) { return r != null && r.roles().contains("admin"); }

  static boolean isModerator(Reader r) { return r != null && (r.roles().contains("moderator") || isAdmin(r)); }

  /** Row level: a public club, or a private one the viewer belongs to. */
  public boolean canSee(Club c) {
    if (c == null) return false;
    Reader me = viewer.current();
    return !c.isPrivate() || isAdmin(me) || (me != null && c.memberIds().contains(me.id()));
  }

  /** A review is as visible as its club. */
  public boolean canSee(Review r) { return r != null && canSee(store.clubs.get(r.clubId())); }

  /** Field level. */
  public boolean canSeeEmail(Reader r) {
    Reader me = viewer.current();
    return me != null && (me.id().equals(r.id()) || isAdmin(me));
  }

  public boolean canSeeInviteCode(Club c) {
    Reader me = viewer.current();
    return me != null && (me.id().equals(c.ownerId()) || isAdmin(me));
  }

  /** Used by @PreAuthorize on ClubService. */
  public boolean isMember(String clubId) {
    Club c = store.clubs.get(clubId);
    Reader me = viewer.current();
    return c != null && me != null && c.memberIds().contains(me.id());
  }

  public boolean ownsClub(String clubId) {
    Club c = store.clubs.get(clubId);
    Reader me = viewer.current();
    return c != null && me != null && (me.id().equals(c.ownerId()) || isAdmin(me));
  }

  public boolean canRemoveReview(String reviewId) {
    Review r = store.reviews.get(reviewId);
    Reader me = viewer.current();
    if (r == null || me == null) return false;
    return me.id().equals(r.authorId()) || isModerator(me) || ownsClub(r.clubId());
  }
}
