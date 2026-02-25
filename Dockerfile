FROM eclipse-temurin:21-jre
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8006
ENTRYPOINT ["java", "-jar", "/app.jar"]
