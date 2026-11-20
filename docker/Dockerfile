# GraphQL in Java course toolchain.
# JDK 25 + Maven 3.9.16 + Hurl 8.1.0 + grpcurl + jq. Everything course-related runs in here.
# noble (24.04): Hurl 8.1.0 links libxml2.so.2, which Ubuntu 26.04 renamed to .so.16
FROM eclipse-temurin:25-jdk-noble

ARG MAVEN_VERSION=3.9.16
ARG HURL_VERSION=8.1.0
ARG GRPCURL_VERSION=1.9.4

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      curl ca-certificates unzip jq git bc python3 libxml2 libcurl4t64 \
 && rm -rf /var/lib/apt/lists/*

# Maven
RUN curl -fsSL "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt \
 && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn

# Hurl: plain-text HTTP conformance tests (language-neutral grading)
RUN arch="$(dpkg --print-architecture)" \
 && case "$arch" in amd64) h=x86_64 ;; arm64) h=aarch64 ;; esac \
 && curl -fsSL "https://github.com/Orange-OpenSource/hurl/releases/download/${HURL_VERSION}/hurl-${HURL_VERSION}-${h}-unknown-linux-gnu.tar.gz" -o /tmp/hurl.tgz \
 && tar -xzf /tmp/hurl.tgz -C /tmp \
 && install /tmp/hurl-${HURL_VERSION}-${h}-unknown-linux-gnu/bin/hurl /tmp/hurl-${HURL_VERSION}-${h}-unknown-linux-gnu/bin/hurlfmt /usr/local/bin/ \
 && rm -rf /tmp/hurl*

# grpcurl: poke the gRPC flavour of Shelfie in Module 1.4
RUN arch="$(dpkg --print-architecture)" \
 && case "$arch" in amd64) g=x86_64 ;; arm64) g=arm64 ;; esac \
 && curl -fsSL "https://github.com/fullstorydev/grpcurl/releases/download/v${GRPCURL_VERSION}/grpcurl_${GRPCURL_VERSION}_linux_${g}.tar.gz" | tar -xz -C /usr/local/bin grpcurl

# Local Maven repo lives in the repo's .cache/ so it survives --rm containers
ENV MAVEN_ARGS="-B -ntp -Dmaven.repo.local=/course/.cache/m2"
# ./mvnw downloads its Maven distribution here instead of the throwaway $HOME
ENV MAVEN_USER_HOME=/course/.cache/mvnw
WORKDIR /course
CMD ["bash"]
