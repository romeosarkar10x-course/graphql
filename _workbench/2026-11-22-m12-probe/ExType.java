import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;
public class ExType { public static void main(String[] a) {
  var c = HttpSyncGraphQlClient.create(RestClient.create(a[0]));
  for (int i = 0; i < 3; i++) try { c.document("{ hello }").retrieveSync("hello").toEntity(String.class); System.out.println("ok"); }
  catch (Exception e) { System.out.println(e.getClass().getName() + " <- " + (e.getCause() == null ? "" : e.getCause().getClass().getName()) + ": " + e.getMessage()); }
}}
