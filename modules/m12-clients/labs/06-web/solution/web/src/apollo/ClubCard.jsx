// Apollo Client, the usual way: one query at the top, data passed down as props.
import { gql } from "@apollo/client";
import { useQuery } from "@apollo/client/react";
import { render } from "../renders.js";

export const CLUB_CARD = gql`
  query ClubCard($id: ID!) {
    club(id: $id) {
      id
      name
      memberCount
      currentBook { id title averageRating }
      members(first: 10) { id name handle }
    }
  }
`;

function BookPanel({ book }) {
  render("BookPanel");
  return <p className="book">{book.title}: {book.averageRating}</p>;
}

function MemberRow({ user }) {
  render("MemberRow");
  return <li>{user.name} (@{user.handle})</li>;
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
