# Lab 4.6 — A 50 MB cover, two ways

**Lecture:** [4.6 File uploads](../../lectures/4.6-file-uploads.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up --rebuild` (reference server plus the `storage` service)

GraphQL has no file type. Two designs fill the gap:

1. **The multipart request convention** (community spec, not part of GraphQL): one
   `multipart/form-data` POST with `operations` (the JSON request, `null` where the file goes), `map`
   (`{"0": ["variables.file"]}`) and the file part. The reference server's `uploadCover` implements it,
   and refuses requests without a `GraphQL-Require-Preflight` header (CSRF, lecture 4.6).
2. **Pre-signed URLs:** `createCoverUpload` returns a short-lived signed URL to object storage
   (SeaweedFS in the course; S3, GCS or MinIO in production); the client PUTs the bytes there;
   `confirmCoverUpload` tells Shelfie, which checks the object and links it.

Both payloads report `serverBufferedBytes`: how much of the file the GraphQL server held in memory.

## Do

1. **TODO 1:** fix `multipart`: stream the file (no `readAllBytes`, no `ByteArrayOutputStream`) and send
   the CSRF header. The test runs your client with `-Xmx48m` and a 50 MB file.
2. **TODO 2:** `presigned`: the three steps, streaming the PUT.
3. Experiments for `notes.md`:
   - Run the starter's multipart with a 50 MB file and `-Xmx48m`; read the error.
   - Record both versions with JDK Flight Recorder and compare client heap:
     `java -Xmx256m -XX:StartFlightRecording:filename=up.jfr -cp ... UploadCover.java multipart b3 cover.jpg`, then
     `jfr print --events jdk.GCHeapSummary up.jfr | grep heapUsed | sort -t= -k2 -n | tail -1`.
   - Compare `serverBufferedBytes` for the two modes.

Make a 50 MB test file with `head -c 52428800 /dev/urandom > cover.jpg`.

## Done when

`bin/course test m04/06` is green and `bin/course grade m04/06` gives 7/10 or more.
