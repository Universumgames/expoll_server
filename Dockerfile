FROM gradle:8.5-jdk21 AS build
WORKDIR /expoll/api_server
COPY --chown=gradle:gradle ./ .
RUN gradle buildFatJar --no-daemon --warning-mode all
#ENTRYPOINT ["gradle", "run", "--no-deamon"]

FROM openjdk:23
EXPOSE 6060:6060
RUN mkdir -p /expoll/api_server
COPY --from=build /expoll/api_server/build/libs/*.jar /expoll/api_server/server.jar
COPY --from=build /expoll/api_server/config /expoll/api_server/config
WORKDIR /expoll/api_server
ENTRYPOINT ["java","-Xmx2g", "-jar","server.jar", "production"]

HEALTHCHECK --timeout=30s --interval=60s CMD curl --silent --fail http://localhost:6060/ping