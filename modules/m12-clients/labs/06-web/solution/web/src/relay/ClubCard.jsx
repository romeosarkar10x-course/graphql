// Relay: the query composes the children's fragments; each child reads only its own (data masking is built in).
// react-relay is CommonJS: Node cannot see its named exports from an ES module, so destructure the default.
import relay from "react-relay";

const { graphql, useLazyLoadQuery } = relay;
import { render } from "../renders.js";
import { BookPanel } from "./BookPanel.jsx";
import { MemberRow } from "./MemberRow.jsx";

export function ClubCard({ id }) {
  render("ClubCard");
  const data = useLazyLoadQuery(
    graphql`
      query ClubCardQuery($id: ID!) {
        club(id: $id) {
          id
          name
          memberCount
          currentBook { id ...BookPanel_book }
          members(first: 10) { id ...MemberRow_user }
        }
      }
    `,
    { id },
  );
  const club = data.club;
  return (
    <section>
      <h1>{club.name} ({club.memberCount} members)</h1>
      {club.currentBook && <BookPanel book={club.currentBook} />}
      <ul>{club.members.map((m) => <MemberRow key={m.id} user={m} />)}</ul>
    </section>
  );
}
