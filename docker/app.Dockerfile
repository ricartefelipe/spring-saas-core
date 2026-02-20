FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl
WORKDIR /app
RUN adduser -D -s /bin/sh appuser
USER appuser
COPY --from=build /build/target/spring-saas-core-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
