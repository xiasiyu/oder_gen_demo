FROM adoptopenjdk/openjdk11:alpine-jre
VOLUME /tmp
ADD build/libs/zt-darkhorse-0.0.1-SNAPSHOT.jar zt-darkhorse.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","zt-darkhorse.jar"]
