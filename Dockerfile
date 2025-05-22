# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY license-header .
COPY pom.xml .
COPY src ./src
RUN mvn clean install -DskipTests

# Package and metadata stage
FROM eclipse-temurin:21-jre-alpine AS metadata-gen
ARG MCPTOOLBOX_URL_ARG
ENV MCPTOOLBOX_URL=${MCPTOOLBOX_URL_ARG}
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=metadata-gen /app/app.jar app.jar
COPY --from=metadata-gen /app/application.jsa application.jsa
EXPOSE 8080
ENTRYPOINT ["java", "-Xshare:auto", "-XX:SharedArchiveFile=/app/application.jsa", "-jar", "app.jar"]
