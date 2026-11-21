// TODO lab 10.6: a well-behaved API client.
//   bin/course java client/Spender.java <url> <api key> <requests>
// Send the catalogue query below <requests> times, all of them, as fast as the budget allows:
// read extensions.cost from each response and wait before a request the bucket cannot pay for.
// Exit 0 when every request got data. Print one line per request: its cost and what was left.
public class Spender {
  static final String QUERY = "{ books(first: 20) { id title reviews(first: 2) { rating } } }";

  public static void main(String[] args) throws Exception {
    throw new UnsupportedOperationException("TODO lab 10.6");
  }
}
