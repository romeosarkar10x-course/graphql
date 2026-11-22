import graphql.schema.idl.MockedWiringFactory;

/**
 * Fake data for any schema, with no resolvers written by hand. This starter only borrows graphql-java's
 * MockedWiringFactory, which makes a schema executable and returns null for every field: try it.
 *
 * TODO implement WiringFactory yourself: a data fetcher for every field (providesDataFetcher/getDataFetcher),
 * a type resolver for every interface and union, a scalar for every custom scalar. Values are a function of
 * (seed, response path); field names steer them; id arguments are echoed; first/last/limit size lists.
 */
public class MockWiring extends MockedWiringFactory {

    private final long seed;

    public MockWiring(long seed) { this.seed = seed; }
}
