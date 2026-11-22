// Apollo Client 4 with data masking: each component declares a fragment and reads it with useFragment.
// The query sees only its own fields, so a change to the book re-renders only the component that asked for it.
import { gql } from "@apollo/client";
import { useFragment, useQuery } from "@apollo/client/react";
import { render } from "../renders.js";

const BOOK_PANEL = gql`
  fragment BookPanel_book on Book { title averageRating }
`;

const MEMBER_ROW = gql`
  fragment MemberRow_user on User { name handle }
`;

export const CLUB_CARD = gql`
  query MaskedClubCard($id: ID!) {
    club(id: $id) {
      id
      name
      memberCount
      currentBook { id ...BookPanel_book }
      members(first: 10) { id ...MemberRow_user }
    }
  }
  ${BOOK_PANEL}
  ${MEMBER_ROW}
`;

function BookPanel({ book }) {
  render("BookPanel");
  const { data } = useFragment({ fragment: BOOK_PANEL, from: book });
  return <p className="book">{data.title}: {data.averageRating}</p>;
}

function MemberRow({ user }) {
  render("MemberRow");
  const { data } = useFragment({ fragment: MEMBER_ROW, from: user });
  return <li>{data.name} (@{data.handle})</li>;
}

export function ClubCard({ id }) {
  render("ClubCard");
  const { data } = useQuery(CLUB_CARD, { variables: { id } });
  if (!data) return <p>loading</p>;
  const club = data.club;
  return (
    <section>
      <h1>{club.name} ({club.memberCount} members)</h1>
      {club.currentBook && <BookPanel book={club.currentBook} />}
      <ul>{club.members.map((m) => <MemberRow key={m.id} user={m} />)}</ul>
    </section>
  );
}
