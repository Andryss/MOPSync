FROM openjdk:17.0.1-jdk-slim

ARG JAR_FILE=target/data-simulator*.jar

WORKDIR /opt/app

COPY ${JAR_FILE} data-simulator.jar

ENTRYPOINT ["java","-jar","data-simulator.jar"]