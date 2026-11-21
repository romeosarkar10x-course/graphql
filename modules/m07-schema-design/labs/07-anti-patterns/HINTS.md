# Hints, lab 7.7

1. Start from the clients, not from `bad.graphqls`: the seven operations are what the product needs.
   Design the fields they want, then add what the rest of the old schema covers.
2. Everything that says "mine" (`getSettings(user_id:)`, `getNotifications(user_id:)`, a shelf) belongs
   under a `viewer` field: one entry point whose identity comes from the request. Typed settings
   (`NotificationPreferences`) replace the JSON blob.
3. A boolean argument becomes an enum argument (`meetups(when: UPCOMING)`, `notifications(status:
   UNREAD)`) or an input filter field (`books(filter: {availability: IN_PRINT})`). Input fields aren't
   arguments, so the `boolean-args` rule doesn't apply to them, but ask whether an enum reads better.
