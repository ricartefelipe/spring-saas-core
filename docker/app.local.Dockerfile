# Build local: use JAR já buildado no host (mvn package).
# Evita falha de rede no build dentro do container (repo.maven.apache.org).
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl jq
WORKDIR /app
RUN adduser -D -s /bin/sh appuser
USER appuser
COPY target/spring-saas-core-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
