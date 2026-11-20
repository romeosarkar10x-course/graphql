# Lab 3.4 — Where does null land?

**Lecture:** [3.4 Nullability and lists](../../lectures/3.4-nullability.md) · **Grading:** auto + rubric · **Needs:** nothing

When a field errors (or returns null where it promised not to), the null goes to the nearest
**nullable** ancestor. Every `!` you write decides how much of a screen one failure can erase.

## Part 1: predict, then run (8 scenarios)

Each `scenarios/NN.json` has a schema, a query and which resolvers fail (`throw`) or return null
(`returnNull`). `User.name@u2` means "only for the user with id u2". All scenarios use the same data:
club c1 (owner Kemi, members Kemi, Asha, Ben; book *The Left Hand of Darkness*) and club c2.

1. For each scenario, **before running it**, write the `data` you expect into `predictions/NN.json`
   (`null`, or the JSON object). Commit or copy your first guesses into `notes.md`: the rubric asks
   for them.
2. Then run `../../../bin/course java Nullability.java scenarios/NN.json` and correct the ones you missed.

## Part 2: design for a bad day

`scenarios/bad-day.json`: the ratings service is down (`Book.averageRating` throws) and one profile is
corrupt (`User.name@u2` throws). With the scenario's schema the club page is blank. Edit `fix.graphqls`
so the same failures leave a usable page: club name, owner, the healthy members and the book title.
Keep `Club.id: ID!`, `Club.name: String!`, `User.id: ID!`, `Book.title: String!` and
`club(id: ID!): Club`. Make as few fields nullable as you can, and say why each one.

Check it with `../../../bin/course java Nullability.java scenarios/bad-day.json fix.graphqls`.

## Done when

`bin/course test m03/04` is green and `bin/course grade m03/04` gives 7/10 or more.
