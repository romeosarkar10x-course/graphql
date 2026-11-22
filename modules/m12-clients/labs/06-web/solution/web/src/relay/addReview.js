// react-relay is CommonJS: Node cannot see its named exports from an ES module, so destructure the default.
import relay from "react-relay";

const { commitMutation, graphql } = relay;

// The response selects the book's id and averageRating: Relay merges them into the store record.
export function addReview(environment, input) {
  return new Promise((resolve, reject) =>
    commitMutation(environment, {
      mutation: graphql`
        mutation addReviewMutation($input: CreateReviewInput!) {
          createReview(input: $input) { review { id book { id averageRating } } userErrors { code } }
        }
      `,
      variables: { input },
      onCompleted: resolve,
      onError: reject,
    }),
  );
}
