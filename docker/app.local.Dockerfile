# Build local: use JAR já buildado no host (mvn package).
# Evita falha de rede no build dentro do container (repo.maven.apache.org).
# eclipse-temurin:21-jre-alpine
FROM eclipse-temurin@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372
# Correções de segurança do Alpine publicadas depois da imagem base entram em cada build.
RUN apk upgrade --no-cache && apk add --no-cache curl jq
WORKDIR /app
RUN adduser -D -s /bin/sh appuser
USER appuser
COPY target/spring-saas-core-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
