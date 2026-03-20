FROM eclipse-temurin:21-jdk-alpine AS build
ARG CACHE_BUST=1
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml pom.xml
RUN mvn dependency:go-offline -q -B || true
COPY src src
RUN mvn -q -DskipTests package -B

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl jq
WORKDIR /app
RUN adduser -D -s /bin/sh appuser
USER appuser
COPY --from=build /build/target/spring-saas-core-*.jar app.jar
EXPOSE 8080
# Garante escuta na porta que o PaaS injeta (Railway: PORT).
ENTRYPOINT ["sh", "-c", "exec java -Xmx512m ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -Dmanagement.tracing.enabled=false -jar app.jar"]
