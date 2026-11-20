# Contract: the club page view model

Labs 1.2 and 1.3 build the same screen two ways. Both programs take a club id as their only argument
and print **exactly this JSON** (key order and whitespace do not matter) to stdout:

```json
{
  "name": "The Midnight Readers",
  "description": "Speculative fiction, one book a month, arguments optional.",
  "owner": "Kemi Adeyemi",
  "memberCount": 8,
  "members": ["Kemi Adeyemi", "Asha Rao", "Ben Okafor", "Chloe Martin", "Dev Patel"],
  "currentBook": { "title": "The Left Hand of Darkness", "authors": ["Ursula K. Le Guin"] },
  "latestReviews": [
    { "reviewer": "Hugo Silva", "rating": 4, "body": "Estraven deserved better." },
    { "reviewer": "Jonas Berg", "rating": 3, "body": "Beautiful but I wanted more physics." },
    { "reviewer": "Chloe Martin", "rating": 5, "body": "The ice crossing chapter. That's it, that's the review." }
  ],
  "nextMeetup": { "title": "Left Hand of Darkness, part 2", "startsAt": "2026-11-27T19:00:00Z" }
}
```

| Field | Rule |
|---|---|
| `members` | the first **5** members' names, in the club's member order |
| `latestReviews` | the **3** newest reviews of the current book, newest first |
| `nextMeetup` | the soonest **upcoming** meetup (past meetups excluded); `null` if none |
| `currentBook` | `null` if the club reads nothing; then `latestReviews` is `[]` |

On **stderr**, as the last line, print what it cost:

```
requests=<HTTP requests made> bytes=<response body bytes received> ms=<wall-clock milliseconds>
```

The base URL comes from the `SHELFIE_URL` environment variable (default `http://shelfie-ref:8080`).
The grader counts requests on the server side and compares with your `requests=`.

Expected outputs for every seeded club: [expected/](expected/).
