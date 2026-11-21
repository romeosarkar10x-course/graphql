#!/usr/bin/env python3
"""Point the course Varnish (service `varnish`, admin port 6082, started with -S none) at an app.

    python3 tools/varnish.py <port> [host]     host defaults to this container's own address

Speaks the varnishd CLI protocol: each reply is "<status> <length>\\n<body>\\n". Loads lab.vcl as a new
VCL with the backend filled in and switches to it, so every test starts with an empty cache too
(`ban req.url ~ .`)."""
import socket, sys, time

def reply(f):
    head = f.readline().split()
    body = f.read(int(head[1]) + 1)
    return int(head[0]), body.decode().strip()

def main():
    port = sys.argv[1]
    host = sys.argv[2] if len(sys.argv) > 2 else socket.gethostbyname(socket.gethostname())
    vcl = open("/course/docker/varnish/lab.vcl").read().replace("BACKEND_HOST", host).replace("BACKEND_PORT", port)
    s = socket.create_connection(("varnish", 6082), timeout=10)
    f = s.makefile("rb")
    status, banner = reply(f)
    if status != 200:
        sys.exit(f"varnish: unexpected banner {status}: {banner}")
    name = f"lab{int(time.time() * 1000)}"
    for cmd in [f"vcl.inline {name} << EOF_VCL\n{vcl}\nEOF_VCL", f"vcl.use {name}", "ban req.url ~ ."]:
        s.sendall(cmd.encode() + b"\n")
        status, body = reply(f)
        if status != 200:
            sys.exit(f"varnish: '{cmd.splitlines()[0]}' failed ({status}): {body}")
    print(f"varnish -> {host}:{port} ({name})")

if __name__ == "__main__":
    main()
