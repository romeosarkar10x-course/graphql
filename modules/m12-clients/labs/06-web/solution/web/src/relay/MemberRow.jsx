// react-relay is CommonJS: Node cannot see its named exports from an ES module, so destructure the default.
import relay from "react-relay";

const { graphql, useFragment } = relay;
import { render } from "../renders.js";

export function MemberRow({ user }) {
  render("MemberRow");
  const data = useFragment(
    graphql`
      fragment MemberRow_user on User { name handle }
    `,
    user,
  );
  return <li>{data.name} (@{data.handle})</li>;
}
