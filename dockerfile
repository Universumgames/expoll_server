FROM gradle:7-jdk11 AS build
WORKDIR /expoll/api_server
COPY --chown=gradle:gradle ./server/ .
RUN gradle buildFatJar --no-daemon

FROM openjdk:11
EXPOSE 8080:8080
RUN mkdir -p /expoll/api_server
COPY --from=build /expoll/api_server/src/build/libs/*.jar /expoll/api_server/server.jar
COPY --from=build /expoll/api_server/config /expoll/api_server/config
ENTRYPOINT ["java","-jar","/expoll/api_server/server.jar"]