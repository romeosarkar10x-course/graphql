# GraphQL security scanners for lab 10.8, in their own image (bin/course seckit).
#   graphql-cop 1.15 (2025-11-07): 17 checks for common GraphQL misconfigurations
#   Clairvoyance 2.5.5: recovers a schema from field suggestions when introspection is off
FROM python:3.13-slim
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/* \
 && git clone --depth 1 --branch 1.15 https://github.com/dolevf/graphql-cop /opt/graphql-cop \
 && pip install --no-cache-dir -r /opt/graphql-cop/requirements.txt clairvoyance==2.5.5 \
 && printf '#!/bin/sh\nexec python3 /opt/graphql-cop/graphql-cop.py "$@"\n' > /usr/local/bin/graphql-cop \
 && chmod +x /usr/local/bin/graphql-cop
WORKDIR /work
