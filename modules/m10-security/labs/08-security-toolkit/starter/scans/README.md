# Scans

`before.json` and `after.json`: graphql-cop's JSON output against your server before and after your
fixes, from the host (the scanner has its own image):

    bin/course seckit graphql-cop -t http://<host>:4000/graphql -o json > scans/before.json

`<host>` is the name of the container your server runs in; see the lab README.
