import java.util.Map;
public class ValShapes {
  public static void main(String[] a) throws Exception {
    var g = Validation.graphQL();
    String m = "mutation($input: CreateReviewInput!) { createReview(input: $input) { review { id } userErrors { field message code } } }";
    for (var in : java.util.List.of(Map.of("bookId","b6","rating",0,"body","Rocky, enough said, mostly."), Map.of("bookId","b6","rating",3,"body","            ")))
      System.out.println(tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(g.execute(graphql.ExecutionInput.newExecutionInput(m).variables(Map.of("input", in)).build()).toSpecification()));
  }
}
