vcl 4.1;
# Module 9.6. Varnish starts with no backend; a lab test points it at the app under test with
# `bin/course varnish <port>` (tools/varnish.py), which loads lab.vcl with the caller's address.
backend default none;
