# Native-image builds (game 6b): GraalVM CE 25 plus the course's Maven. bin/course native builds it.
FROM ghcr.io/graalvm/native-image-community:25
COPY --from=graphql-course:1 /opt/apache-maven-3.9.16 /opt/apache-maven-3.9.16
ENV PATH=/opt/apache-maven-3.9.16/bin:$PATH MAVEN_ARGS="-B -ntp -Dmaven.repo.local=/course/.cache/m2"
ENTRYPOINT []
