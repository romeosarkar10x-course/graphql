#!/usr/bin/env bash
# Lab 10.1 probe: viewer with no token, a Keycloak token, minted, expired, forged, alg none, wrong aud/iss.
cd "${APP:-/course/modules/m10-security/labs/01-authentication/solution}"
mvn -q package -DskipTests >/tmp/b.log 2>&1 || { tail -30 /tmp/b.log; exit 1; }
java -jar target/shelfie-secure.jar >/tmp/app.log 2>&1 & P=$!
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
T="java /course/modules/m10-security/contract/Token.java"
q='{"query":"{ viewer { id name roles } }"}'
call() { echo "== $1"; shift; curl -s -D /tmp/h -H 'Content-Type: application/json' "$@" -d "$q" localhost:4000/graphql; echo; grep -iE '^HTTP|www-authenticate' /tmp/h; }
call "no token"
kc=$(curl -s -d grant_type=password -d client_id=shelfie-cli -d username=max -d password=max-pw http://keycloak:8180/realms/shelfie/protocol/openid-connect/token | jq -r .access_token)
call "keycloak token (max)" -H "Authorization: Bearer $kc"
call "minted (ada)" -H "Authorization: Bearer $($T ada)"
call "expired" -H "Authorization: Bearer $($T ada --expired)"
call "forged" -H "Authorization: Bearer $($T ada --forged)"
call "alg none" -H "Authorization: Bearer $($T root --none)"
call "wrong audience" -H "Authorization: Bearer $($T ada --aud other-api)"
call "wrong issuer" -H "Authorization: Bearer $($T ada --iss http://evil:8180/realms/shelfie)"
call "garbage" -H "Authorization: Bearer abc.def.ghi"
call "Basic scheme" -H "Authorization: Basic YWRhOmFkYS1wdw=="
kill $P; wait $P 2>/dev/null
