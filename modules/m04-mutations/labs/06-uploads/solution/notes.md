# Lab 4.6 notes (reference; _workbench/2026-11-21-m04-probe/upload-50mb.txt)

## The starter at 48 MB

`java.lang.OutOfMemoryError: Java heap space` in `readAllBytes` or the ByteArrayOutputStream. The file
(50 MB) plus the stream's growing buffer (copies on each doubling) plus `toByteArray()` (another 50 MB
copy) are alive at once: 100 MB or more.

## Flight Recorder

Measured (`_workbench/2026-11-21-m04-probe/upload-jfr.txt`, -Xmx256m, 50 MB): starter multipart
219.5 MB peak heap used; streaming multipart 48.1 MB; presigned 48.1 MB. The 48 MB is the baseline of
a source-launched program (javac compiles `UploadCover.java` inside the same JVM), not the file: it is
the same for a 5 MB file. The starter's peak is about four copies of the file.

## Server memory

multipart: 52428800 (all of it; the reference server reads the part into a byte array, like many
simple implementations). presigned: 8192 (it streams the object from storage through SHA-256). 20
concurrent 50 MB multipart uploads hold about 1 GB of heap in the GraphQL server; with signed URLs the
bytes never reach it.

## CSRF

Browsers send cross-site "simple requests" (form-encoded, multipart, text/plain POSTs) with the
victim's cookies and no preflight; an attacker's page can auto-submit a hidden form to
`/graphql` and run a mutation as the victim. A JSON POST is not simple: the browser preflights it, and
the server's CORS policy refuses. Requiring a custom header makes the multipart request non-simple too.

## Question

Gains: the GraphQL server neither buffers nor proxies bytes (memory, bandwidth, timeouts); storage
handles resumable/parallel upload and CDN delivery; uploads can bypass the API's request size limits.
New failure modes: an upload that succeeds but is never confirmed (orphaned objects, need a lifecycle
rule), a URL that expires mid-upload, clock skew between signer and storage.
