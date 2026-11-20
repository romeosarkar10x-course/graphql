# Hints, lab 4.6

1. `HttpRequest.BodyPublishers.concat(ofString(head), ofFile(path), ofString(tail))` sends three parts as one body, reading the file as it goes. The multipart framing is text you can build as two strings around the file.
2. The CSRF header is literally `GraphQL-Require-Preflight: 1`. Without it the server answers 400 before reading the parts. Why a header? An HTML `<form>` on another site can POST multipart with the user's cookies, but cannot add custom headers.
3. The signed URL already carries the credentials in its query string: send it with `Content-Type: image/jpeg` (it was signed for that type; another type gets 403) and `BodyPublishers.ofFile`. Then `confirmCoverUpload(input: {uploadId: ...})`.
