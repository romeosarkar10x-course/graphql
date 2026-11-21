#!/usr/bin/env bash
# Labs 10.2+: verify with the course public key (no Keycloak) and still check iss.
cd /course/modules/m10-security/labs/01-authentication/solution
java -jar target/shelfie-secure.jar --spring.security.oauth2.resourceserver.jwt.public-key-location=file:/course/modules/m10-security/contract/keys/course-public.pem --spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8180/realms/shelfie --logging.level.org.springframework.web.client=DEBUG >/tmp/app.log 2>&1 & P=$!
for _ in $(seq 60); do (exec 3<>/dev/tcp/localhost/4000) 2>/dev/null && break; sleep 0.5; done
T="java /course/modules/m10-security/contract/Token.java"
q='{"query":"{ viewer { id } }"}'
for t in "ada" "ada --iss http://evil/realms/x" "ada --expired" "ada --aud x"; do
  printf '%-36s ' "$t"; curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' -H "Authorization: Bearer $($T $t)" -d "$q" localhost:4000/graphql
done
grep -ciE 'keycloak|well-known' /tmp/app.log
kill $P; wait $P 2>/dev/null
