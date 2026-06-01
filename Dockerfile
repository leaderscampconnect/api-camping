FROM eclipse-temurin:17
COPY target/api-camping-0.0.1-SNAPSHOT.jar api-camping.jar
EXPOSE 8083
ENTRYPOINT ["java","jar", "api-camping.jar"]