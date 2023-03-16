FROM gradle:8-jdk17 AS build
WORKDIR /expoll/api_server
COPY --chown=gradle:gradle ./server/ .
RUN gradle buildFatJar --no-daemon
#ENTRYPOINT ["gradle", "run", "--no-deamon"]

FROM openjdk:19
EXPOSE 6060:6060
RUN mkdir -p /expoll/api_server
COPY --from=build /expoll/api_server/build/libs/*.jar /expoll/api_server/server.jar
COPY --from=build /expoll/api_server/config /expoll/api_server/config
WORKDIR /expoll/api_server
ENTRYPOINT ["java","-jar","server.jar", "production"]

HEALTHCHECK --timeout=30s CMD curl --silent --fail http://localhost:6060/serverInfo