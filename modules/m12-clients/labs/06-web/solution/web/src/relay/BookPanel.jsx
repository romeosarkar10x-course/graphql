// react-relay is CommonJS: Node cannot see its named exports from an ES module, so destructure the default.
import relay from "react-relay";

const { graphql, useFragment } = relay;
import { render } from "../renders.js";

export function BookPanel({ book }) {
  render("BookPanel");
  const data = useFragment(
    graphql`
      fragment BookPanel_book on Book { title averageRating }
    `,
    book,
  );
  return <p className="book">{data.title}: {data.averageRating}</p>;
}
