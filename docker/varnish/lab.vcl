vcl 4.1;
# Loaded by tools/varnish.py with BACKEND_HOST and BACKEND_PORT filled in. Everything else is
# Varnish's built-in behaviour: GET and HEAD are cached for Cache-Control max-age (or 120 s when the
# response says nothing), private, no-cache and no-store responses are not.
backend default { .host = "BACKEND_HOST"; .port = "BACKEND_PORT"; }

sub vcl_deliver {
  if (obj.hits > 0) {
    set resp.http.X-Cache = "HIT";
  } else {
    set resp.http.X-Cache = "MISS";
  }
}
