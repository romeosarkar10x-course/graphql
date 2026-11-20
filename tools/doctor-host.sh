#!/usr/bin/env bash
# Host half of `bin/course doctor`: what must be true outside the container.
ROOT="$1"; IMAGE="$2"
fail=0
ok()   { printf '  \033[32mok\033[0m    %s\n' "$1"; }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n        fix: %s\n' "$1" "$2"; fail=1; }
warn() { printf '  \033[33mwarn\033[0m  %s\n        %s\n' "$1" "$2"; }

echo "Host"
if docker info >/dev/null 2>&1; then ok "docker daemon reachable ($(docker version --format '{{.Server.Version}}'))"
else bad "docker daemon not reachable" "start Docker, and make sure your user can run 'docker info' without sudo"; fi

if docker compose version >/dev/null 2>&1; then ok "docker compose ($(docker compose version --short))"
else bad "docker compose plugin missing" "install the Docker Compose v2 plugin (SETUP.md)"; fi

if (( BASH_VERSINFO[0] >= 4 )); then ok "bash ${BASH_VERSION%%(*}"
else bad "bash ${BASH_VERSION} is too old" "install bash 4+ (macOS: brew install bash)"; fi

if docker image inspect "$IMAGE" >/dev/null 2>&1; then ok "course image $IMAGE built"
else bad "course image missing" "bin/course image"; fi

free_kb=$(df -Pk "$ROOT" | awk 'NR==2{print $4}')
if (( free_kb > 5*1024*1024 )); then ok "disk: $((free_kb/1024/1024)) GB free"
else warn "disk: only $((free_kb/1024)) MB free" "Maven repo and images need about 5 GB"; fi

for port in 8080 9090 8474 8666; do
  owner=$(docker ps --filter "publish=$port" --format '{{.Names}}' 2>/dev/null | head -1)
  if [[ -n "$owner" ]]; then
    [[ "$owner" == gqlcourse-* ]] && ok "port $port used by $owner (course service)" \
      || bad "port $port taken by container $owner" "stop it, or it will clash with bin/course ref up"
  elif (exec 3<>/dev/tcp/127.0.0.1/$port) 2>/dev/null; then
    bad "port $port taken by a host process" "find it with: ss -ltnp | grep :$port"
  else ok "port $port free"; fi
done

command -v git >/dev/null && ok "git $(git --version | awk '{print $3}')" || warn "git not on host" "only needed for checkpoints and commits"
exit $fail
