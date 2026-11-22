package dev.shelfie.digest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Shelfie is a WireMock stub on a random port: stubs match the GraphQL request by operation and variables. */
@WireMockTest
class DigestClientTest {

    DigestClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) { client = new DigestClient(URI.create(wm.getHttpBaseUrl() + "/graphql")); }

    static void shelfieAnswers(String userId, String response) {
        stubFor(post(urlEqualTo("/graphql"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.operationName", equalTo("UserReviews")))
            .withRequestBody(matchingJsonPath("$.variables", equalToJson("{\"id\": \"" + userId + "\"}")))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @Test
    void bestFirstThenByTitle() throws Exception {
        shelfieAnswers("u5", """
            {"data": {"user": {"name": "Elena Petrova", "reviews": [
              {"rating": 4, "book": {"title": "Piranesi"}},
              {"rating": 5, "book": {"title": "The Seven Moons of Maali Almeida"}},
              {"rating": 4, "book": {"title": "Klara and the Sun"}}]}}}""");
        assertThat(client.digest("u5")).containsExactly(
            "Elena Petrova's week", "5/5 The Seven Moons of Maali Almeida", "4/5 Klara and the Sun", "4/5 Piranesi");
    }

    @Test
    void sendsOneRequestWithTheUserAsAVariable() throws Exception {
        shelfieAnswers("u9", "{\"data\": {\"user\": {\"name\": \"Ines Duarte\", \"reviews\": []}}}");
        assertThat(client.digest("u9")).containsExactly("Ines Duarte's week");
        verify(1, postRequestedFor(urlEqualTo("/graphql"))
            .withRequestBody(matchingJsonPath("$.query", com.github.tomakehurst.wiremock.client.WireMock.containing("$id: ID!"))));
    }

    @Test
    void anUnknownUserIsAnEmptyDigest() throws Exception {
        shelfieAnswers("zz", "{\"data\": {\"user\": null}}");
        assertThat(client.digest("zz")).isEmpty();
    }

    @Test
    void errorsWithoutDataAreAShelfieError() {
        shelfieAnswers("u1", "{\"errors\": [{\"message\": \"Shelfie is down for maintenance\"}], \"data\": null}");
        assertThatThrownBy(() -> client.digest("u1")).isInstanceOf(DigestClient.ShelfieError.class)
            .hasMessage("Shelfie is down for maintenance");
    }
}
