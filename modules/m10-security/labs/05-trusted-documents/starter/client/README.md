# Shelfie's web client, as far as the server is concerned

`operations/` holds every operation the client sends: one file per operation, named after it. In a real
client these are extracted from the source at build time (Apollo's `generate-persisted-query-manifest`,
Relay's `--persist-output`, graphql-codegen's client preset). Here they are the source.

`BuildManifest.java` is the client build's last step: it turns `operations/` into the manifest the server
trusts. Run it after changing any operation:

    bin/course java client/BuildManifest.java client/operations src/main/resources/trusted-documents.json
