FROM openjdk:17.0.1-jdk-slim

ARG JAR_FILE=target/iot-controller*.jar

WORKDIR /opt/app

COPY ${JAR_FILE} iot-controller.jar

ENTRYPOINT ["java","-jar","iot-controller.jar"]