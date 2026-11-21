# Lab 7.7 notes

## 1. The worst five

1. **Identity as arguments** (`getUser(user_id:)` for "me", `insertReview(user_id:)`,
   `getNotifications(user_id:)`, `saveSettings(user_id:)`). Any client can act as anyone: an authorisation
   hole in the schema's shape. Replaced by `Query.viewer` and mutations that take the caller from the request.
2. **Untyped escape hatches** (`scalar JSON` on `getSettings`, `Response.data`, `findBooks(filter:)`,
   `patchMeetup(fields:)`, `rawQuery(sql:)`). Nothing validates, nothing documents, clients guess. Replaced
   by `NotificationPreferences`, `BookFilter`, per-mutation payloads, and `rawQuery` deleted: a SQL string
   in the API is an injection endpoint, not a feature.
3. **One generic `Response` for every mutation** (`success`, `message`, `error_code`, `data: JSON`):
   no way to return the changed object, so every mutation costs a refetch. Replaced by a `XPayload`
   with the changed object and `userErrors` (Module 4's convention).
4. **The kitchen-sink `updateUser`** (15 fields from four concerns, including `is_admin` and
   `password`). Replaced by `updateProfile` and `setNotificationPreferences`; admin and password changes
   don't belong in a member-facing mutation at all.
5. **Keys and table names on the surface** (`owner_user_id`, `member_ids`, `tbl_users`,
   `ReviewEntity`, `deleteRow(table:)`). Clients learn the database and break when it changes.
   Replaced by references (`owner: User!`, `members`), domain names, and intent mutations.

## 2. The kitchen sink

Grouped by "who changes it, and on which screen": name, bio and avatar on the profile screen
(`updateProfile`); the four notification switches on the settings screen (`setNotificationPreferences`);
handle, email and password are account-security changes that need re-authentication (left for Module
10, not in this schema); `is_admin` and `is_deleted` are staff operations, not member mutations. A field
moves together with others only if one screen changes them in one save.

## 3. Bounded contexts

| Context | Types | Owns the data for |
|---|---|---|
| Profiles | `User`, `Viewer`, `NotificationPreferences` | who a member is and their settings |
| Clubs | `Club`, `Meetup`, `MeetupLocation`, `ClubVisibility`, `MeetupTime`, `MeetupStatus` | clubs, membership, meetups |
| Catalogue | `Book`, `Author`, `Genre`, `BookAvailability`, `BookFilter` | books and their metadata |
| Reviews | `Review`, `ReviewOrder` | ratings and review text, and the averages |
| Shelves | `ShelfEntry`, `ShelfStatus` | each member's reading status per book |
| Notifications | `Notification`, `MeetupNotification`, `ReviewNotification`, `NotificationStatus` | what to tell whom |

Border types: `Book.averageRating` and `ratingCount` are computed by Reviews but sit on a Catalogue
type: Reviews should own those fields (in federation: Reviews extends `Book`). `Club.currentBook` is a
Clubs fact pointing into Catalogue: Clubs stores the book id, Catalogue resolves the `Book`. `User.clubs`
is a Clubs fact on a Profiles type, the same pattern. `Viewer` is the composition point for everything
"mine" and belongs to Profiles, with each context contributing its fields.

## 4. What you kept on purpose

`MeetupLocation.online: Boolean!` and the booleans in `NotificationPreferences` would make a naive
"no booleans" rule fire; the lint only flags boolean **arguments** (flags that change what a field
returns). A boolean that is a fact (`online`) or a switch (`push`) is fine. On the other side, the lint
would accept `isbn13: String`: in a real API I'd make it an `ISBN` scalar (Module 3.3) so a bad value
fails validation, not a lookup.
