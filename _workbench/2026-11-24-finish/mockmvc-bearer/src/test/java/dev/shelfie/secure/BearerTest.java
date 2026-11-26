package dev.shelfie.secure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Module 13 finding 7: a bearer token through MockMvc. TOKEN is a course token for ada. */
@SpringBootTest
@AutoConfigureHttpGraphQlTester
class BearerTest {
  static final String TOKEN = System.getenv("TOKEN");
  @Autowired HttpGraphQlTester http;

  @Test
  void httpGraphQlTester() {
    var res = http.mutate().header("Authorization", "Bearer " + TOKEN).build()
        .document("{ viewer { name } }").execute();
    System.out.println("TESTER " + res.returnResponse());
  }
}
