# graphql-inspector for lab 7.6 and the Module 7 games: bin/course inspector <args>.
# Node.js lives only here; the course image stays Java-only. The overrides stop npm's caret
# ranges from pulling newer inspector commands than the pinned CLI.
FROM node:24.22.1-alpine
WORKDIR /opt/inspector
RUN printf '%s' '{"private":true,"dependencies":{"@graphql-inspector/cli":"7.0.2","graphql":"16.12.0"},"overrides":{"@graphql-inspector/diff-command":"7.0.2","@graphql-inspector/core":"8.0.1","@graphql-inspector/commands":"7.0.1","graphql":"16.12.0"}}' > package.json \
 && npm install --no-fund --no-audit \
 && ln -s /opt/inspector/node_modules/.bin/graphql-inspector /usr/local/bin/graphql-inspector
WORKDIR /course
ENTRYPOINT ["graphql-inspector"]
