FROM eclipse-temurin:21-jdk-alpine AS build
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
ENTRYPOINT ["java", "-jar", "app.jar"]
