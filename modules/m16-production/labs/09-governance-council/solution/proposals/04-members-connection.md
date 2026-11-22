# Proposal 4: paginate Club.members (community team)

**Change.** Add `Club.membersConnection(first: Int = 20, after: String): MemberConnection!` (Relay-style
connection with `totalCount`), and deprecate `Club.members: [User!]!` with reason "Use membersConnection".

**Why.** Clubs today have 4 to 8 members; the community team is launching open clubs, expected to reach thousands
of members. `Club.members` returns everyone, every time.

**Usage today (7 days).** `Club.members` is selected by web 3.2.0 (`ClubPage`) and iOS 2.9.1 (`ClubScreen`).
`Club.memberCount` (deprecated) is still used by iOS 2.8.0.

**Cost.** `Club.members` counts as 6 items in the cost rule (lab 16.6); with open clubs it would be unbounded.
