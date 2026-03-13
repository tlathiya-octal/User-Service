FROM eclipse-temurin:21-jdk
COPY build/libs/user-service.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
