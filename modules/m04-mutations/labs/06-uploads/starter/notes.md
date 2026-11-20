# Lab 4.6 notes

## The starter at 48 MB

Paste the error. Why does a 50 MB file need more than 50 MB of heap in the starter?

## Flight Recorder

| | Peak client heap used, 50 MB file |
|---|---|
| Starter multipart (with -Xmx256m) | |
| Your multipart | |
| Your presigned | |

## Server memory

`serverBufferedBytes` for each mode. What happens to the reference server with 20 users uploading
50 MB covers at once through `uploadCover`?

## CSRF

Explain in your own words why a multipart GraphQL endpoint without the header check is a CSRF hole,
when a JSON one is not.

## Question

Pre-signed uploads take three calls instead of one. Give two things you gain and one new failure mode.
