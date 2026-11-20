# Game 3c reference notes (grader only)

The reference schema at the end of Module 4: `bin/course gql schema-lines http://localhost:8080/graphql/schema`.
Root types: Query, Mutation, Subscription. Object types reachable from Query: Club, User, Book, Review,
Meetup; union SearchResult = Book | Club | User; enum Visibility.

1. Cycles: Club -members-> User -clubs-> Club (shortest: 2 edges); also Book -reviews-> Review -book-> Book,
   Review -author-> User -reviews-> Review, Meetup -club-> Club -meetups-> Meetup. Any cycle lets a client
   nest without bound: that is Module 10's depth limit.
2. Only from Mutation: the payloads (CreateReviewPayload, DeleteReviewPayload, JoinClubPayload,
   LeaveClubPayload, UploadCoverPayload, CreateCoverUploadPayload), UserError, UserErrorCode, the
   CreateReviewResult union and its members (ReviewCreated, ValidationFailed, FieldProblem, NotAllowed),
   all input types, and the Upload scalar.
