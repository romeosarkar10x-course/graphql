package dev.shelfie.dgs;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** DGS's own in-process executor: run a document, read the result with a JsonPath. */
@SpringBootTest
class ClubFetcherTest {

    @Autowired DgsQueryExecutor dgs;

    @Test
    void clubWithOwnerAndCurrentBook() {
        Map<String, Object> club = dgs.executeAndExtractJsonPath(
            "query($id: ID!) { club(id: $id) { name memberCount owner { handle } currentBook { title averageRating } } }",
            "data.club", Map.of("id", "c2"));
        assertThat(club).containsEntry("name", "Slow Burn Mysteries").containsEntry("memberCount", 4);
        assertThat(club.get("owner")).isEqualTo(Map.of("handle", "gracek"));
        assertThat(club.get("currentBook")).isEqualTo(Map.of("title", "The Name of the Rose", "averageRating", 4.5));
    }

    @Test
    void everyClubInOrder() {
        List<String> ids = dgs.executeAndExtractJsonPath("{ clubs { id } }", "data.clubs[*].id");
        assertThat(ids).containsExactly("c1", "c2", "c3");
    }

    @Test
    void averageIsNullWithoutReviews() {
        var result = dgs.execute("{ book(id: \"b5\") { title averageRating } }");
        assertThat(result.getErrors()).isEmpty();
        Map<String, Map<String, Object>> data = result.getData();
        assertThat(data.get("book")).containsEntry("title", "Good Omens").containsEntry("averageRating", null);
    }
}
