// Apollo Client with props, children wrapped in React.memo: Apollo keeps unchanged objects identical (===)
// between results, so memo can skip the rows whose user did not change. The parent still re-renders.
import { gql } from "@apollo/client";
import { useQuery } from "@apollo/client/react";
import { memo } from "react";
import { render } from "../renders.js";

export const CLUB_CARD = gql`
  query MemoClubCard($id: ID!) {
    club(id: $id) {
      id
      name
      memberCount
      currentBook { id title averageRating }
      members(first: 10) { id name handle }
    }
  }
`;

const BookPanel = memo(function BookPanel({ book }) {
  render("BookPanel");
  return <p className="book">{book.title}: {book.averageRating}</p>;
});

const MemberRow = memo(function MemberRow({ user }) {
  render("MemberRow");
  return <li>{user.name} (@{user.handle})</li>;
});

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
