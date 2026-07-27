FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S pitflow && adduser -S pitflow -G pitflow
WORKDIR /app
COPY target/pitflow-payment-*.jar app.jar
USER pitflow
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
