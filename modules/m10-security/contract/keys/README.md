# Course signing key: public on purpose

`course-private.pem` signs every token in Module 10: Keycloak's `shelfie` realm imports it
(`../keycloak/shelfie-realm.json`), and `bin/course token` uses it to mint tokens offline, including
expired and wrong-audience ones that Keycloak would never issue.

It is in the repository so that tests are reproducible. **Never do this with a real key.** Anyone
with this file can sign in as any course user, including `root`. The key only works against apps
that trust it: Module 10's labs and the course Keycloak.

- `course-public.pem`: what an API needs to verify tokens offline (`public-key-location`).
- `course-cert.pem`: the self-signed certificate Keycloak's key provider wants next to the key.
- Key id (`kid`): `fPw-NL_CmaX4CjJ28Iw3BB7yUsNVcKeQ7CUvnDFpk-A`, which is how Keycloak names it:
  base64url(SHA-256(the DER public key)).
