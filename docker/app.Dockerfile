FROM eclipse-temurin:21-jdk-alpine AS build
ARG CACHE_BUST=1
WORKDIR /build
RUN apk add --no-cache maven
COPY pom.xml pom.xml
RUN mvn dependency:go-offline -q -B || true
COPY src src
RUN mvn -q -DskipTests package -B

FROM eclipse-temurin:21-jre-alpine AS runtime
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
