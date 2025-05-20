FROM openjdk:21-jdk-slim
WORKDIR /app
COPY ProxyAsLocalModel-0.0.9.jar /app

RUN chmod +x /app/ProxyAsLocalModel-0.0.9.jar

EXPOSE 1234
EXPOSE 11434

ENTRYPOINT ["java","-jar","/app/ProxyAsLocalModel-0.0.9.jar"]
