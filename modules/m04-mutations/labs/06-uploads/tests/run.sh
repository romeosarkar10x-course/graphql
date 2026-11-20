#!/usr/bin/env bash
source /course/tools/testlib.sh
cd "$LAB_DIR"
require_file UploadCover.java || finish
head -c 5242880 /dev/urandom >/tmp/cover-5mb.jpg
head -c 52428800 /dev/urandom >/tmp/cover-50mb.jpg
for size in 5mb 50mb; do
  want=$(sha256sum /tmp/cover-$size.jpg | cut -d' ' -f1); bytes=$(stat -c %s /tmp/cover-$size.jpg)
  for mode in multipart presigned; do
    out=$(timeout 120 java -Xmx48m -cp "$(lib_cp)" UploadCover.java $mode b3 /tmp/cover-$size.jpg 2>/tmp/up.err)
    if [[ "$out" == "$mode bytes=$bytes sha256=$want "* ]]; then pass "$mode, $size, client heap 48 MB: every byte arrived (sha256 matches)"
    else fail "$mode, $size, client heap 48 MB" "${out:-$(grep -m1 -E 'Exception|Error' /tmp/up.err)}"; continue; fi
    buffered=${out##*serverBufferedBytes=}
    if [[ $mode == multipart ]]; then [[ "$buffered" == "$bytes" ]] && note "multipart: the GraphQL server held all $bytes bytes in memory"
    else [[ "$buffered" == 8192 ]] && note "presigned: the GraphQL server never held more than $buffered bytes"; fi
  done
done
code=$(curl -s -o /dev/null -w '%{http_code}' "$REF/graphql" -F 'operations={"query":"mutation($f: Upload!) { uploadCover(bookId: \"b3\", file: $f) { bytes } }","variables":{"f":null}}' -F 'map={"0":["variables.f"]}' -F '0=@/tmp/cover-5mb.jpg')
[[ "$code" == 400 ]] && note "the server refuses multipart without GraphQL-Require-Preflight (HTTP $code): that is the CSRF guard"
rm -f /tmp/cover-5mb.jpg /tmp/cover-50mb.jpg
ref_reset_data
notes_check notes.md 60 m04/06
finish
