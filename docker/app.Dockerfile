FROM eclipse-temurin:21-jdk-alpine@sha256:b78a1d385da875588e21207f07dddf9a1418849971ae857fb24c8f3a7d441a72 AS build
ARG CACHE_BUST=1
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml pom.xml
RUN mvn dependency:go-offline -q -B || true
COPY src src
RUN mvn -q -DskipTests package -B

FROM eclipse-temurin:21-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372 AS runtime
# Correções de segurança do Alpine publicadas depois da imagem base entram em cada build.
RUN apk upgrade --no-cache && apk add --no-cache curl jq
WORKDIR /app
RUN adduser -D -s /bin/sh appuser
USER appuser
COPY --from=build /build/target/spring-saas-core-*.jar app.jar
EXPOSE 8080
# Sem variável definida, application.yml cairia no profile "local".
# Staging herda este default; em produção defina explicitamente SPRING_PROFILES_ACTIVE=prod.
ENV SPRING_PROFILES_ACTIVE=staging
# Garante escuta na porta que a plataforma injeta (PORT).
ENTRYPOINT ["sh", "-c", "exec java -Xmx512m ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -Dmanagement.tracing.enabled=false -jar app.jar"]
