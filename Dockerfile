FROM johnnyjayjay/leiningen:openjdk11 AS build
WORKDIR /usr/src/instant-poll
COPY . .
RUN apt-get update && apt-get -y --no-install-recommends install libsodium-dev && lein uberjar

FROM openjdk:11
ARG version
ARG jar=instant-poll-$version-standalone.jar
WORKDIR /usr/app/instant-poll
COPY --from=build /usr/src/instant-poll/target/$jar .
RUN apt-get update && apt-get -y --no-install-recommends install libsodium-dev
ENV jar=$jar
CMD java -jar $jar
