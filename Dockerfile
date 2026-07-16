FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S pitflow && adduser -S pitflow -G pitflow
WORKDIR /app
COPY --from=build /workspace/target/pitflow-payment-*.jar app.jar
USER pitflow
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
