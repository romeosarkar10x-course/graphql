import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lab 8.7 harness: the club page in three reviews-service modes, and the club loaded once. */
public class PartialCheck {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok    " : "FAIL  ") + what); if (!ok) fails++; }

    record Run(JsonNode res, int loads, int reviewCalls) {}

    static Run run(Services.Mode mode, String query) {
        var clubs = new Services.ClubService();
        var reviews = new Services.ReviewsService();
        reviews.mode = mode;
        Map<String, Object> spec = ClubPage.graphQL(clubs, reviews).execute(query).toSpecification();
        return new Run(JSON.valueToTree(spec), clubs.loads.get(), reviews.calls.get());
    }

    public static void main(String[] args) {
        String page = ClubPage.PAGE;

        var up = run(Services.Mode.UP, page);
        check(!up.res().has("errors"), "UP: no errors");
        check(up.res().at("/data/club/recentReviews").size() == 5, "UP: 5 recent reviews");
        check(up.res().at("/data/club/members/0/isOwner").asBoolean(false) && !up.res().at("/data/club/members/1/isOwner").asBoolean(true),
            "UP: isOwner is true for the owner only");
        check(up.res().at("/data/club/recentReviews/4/author/isOwner").asBoolean(false), "UP: isOwner works for review authors too (r5 is by the owner)");
        check(up.loads() == 1, "the club is loaded once per request, not once per member (" + up.loads() + " loads)");

        var down = run(Services.Mode.DOWN, page);
        JsonNode errors = down.res().path("errors");
        check(down.res().at("/data/club/name").asString("").equals("The Midnight Readers"), "DOWN: the club page still renders (club.name)");
        check(down.res().at("/data/club/members").size() == 4, "DOWN: members are all there");
        check(down.res().at("/data/club/recentReviews").isArray() && down.res().at("/data/club/recentReviews").isEmpty(),
            "DOWN: recentReviews is an empty list, not null");
        check(errors.size() == 1 && errors.get(0).path("path").equals(JSON.valueToTree(List.of("club", "recentReviews"))),
            "DOWN: exactly one error, at [club, recentReviews]");
        check(errors.path(0).at("/extensions/code").asString("").equals("REVIEWS_UNAVAILABLE"), "DOWN: extensions.code is REVIEWS_UNAVAILABLE");
        check(!errors.path(0).path("message").asString("").contains("reviews-db"), "DOWN: the message hides the database host");

        var partial = run(Services.Mode.PARTIAL, page);
        JsonNode perrors = partial.res().path("errors");
        var ids = partial.res().at("/data/club/recentReviews");
        check(ids.size() == 3, "PARTIAL: the 3 reviews that loaded are in the list (" + ids.size() + ")");
        check(perrors.size() == 1 && perrors.get(0).path("path").equals(JSON.valueToTree(List.of("club", "recentReviews"))),
            "PARTIAL: one error at [club, recentReviews] (error paths are unique, RFC #1183)");
        check(perrors.path(0).at("/extensions/code").asString("").equals("REVIEWS_INCOMPLETE")
            && perrors.path(0).at("/extensions/missing").equals(JSON.valueToTree(List.of("r2", "r4"))),
            "PARTIAL: extensions.code REVIEWS_INCOMPLETE and extensions.missing [r2, r4]");
        check(partial.loads() == 1 && partial.reviewCalls() == 1, "PARTIAL: one club load, one reviews call");
        System.exit(fails == 0 ? 0 : 1);
    }
}
