FROM openjdk:17.0.1-jdk-slim

ARG JAR_FILE=target/rule-engine*.jar

WORKDIR /opt/app

COPY ${JAR_FILE} rule-engine.jar

ENTRYPOINT ["java","-jar","rule-engine.jar"]