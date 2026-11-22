package dev.shelfie.typed;

/**
 * Lab 12.3: print the club page (README for the exact format) with DgsGraphQlClient and the classes
 * that `mvn generate-sources` writes to target/generated-sources/dev/shelfie/typed/generated/:
 * client/*GraphQLQuery and client/*ProjectionRoot for requests, types/* for results. No document text.
 */
public class ClubPage {
  public static void main(String[] args) {
    String url = args.length > 0 ? args[0] : "http://shelfie-ref:8080/graphql";
    String clubId = args.length > 1 ? args[1] : "c1";
    System.out.println("TODO: the club page for " + clubId + " from " + url);
  }
}
