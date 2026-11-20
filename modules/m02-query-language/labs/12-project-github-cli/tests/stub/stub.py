"""A stand-in for api.github.com/graphql, for offline tests of lab 2.12.

Usage: python3 stub.py <port> <request-log.jsonl> <responses-dir>
Logs each request (method, path, headers, body) as one JSON line, then answers with
<responses-dir>/<variables.login>.json, or notfound.json for any other login.
The responses are synthetic fixtures shaped like GitHub's real answers; the numbers are made up.
"""
import json, os, sys
from http.server import BaseHTTPRequestHandler, HTTPServer

port, log_path, responses = int(sys.argv[1]), sys.argv[2], sys.argv[3]

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        raw = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        try:
            body = json.loads(raw)
        except ValueError:
            body = None
        with open(log_path, "a") as log:
            log.write(json.dumps({"method": "POST", "path": self.path,
                                  "headers": {k.lower(): v for k, v in self.headers.items()},
                                  "body": body}) + "\n")
        login = ((body or {}).get("variables") or {}).get("login", "")
        name = os.path.join(responses, os.path.basename(login) + ".json")
        if not os.path.isfile(name):
            name = os.path.join(responses, "notfound.json")
        data = open(name, "rb").read()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        self.send_response(405)
        self.end_headers()

    def log_message(self, *args):
        pass

HTTPServer(("127.0.0.1", port), Handler).serve_forever()
