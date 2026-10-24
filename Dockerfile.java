FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/sigelab-1.0-SNAPSHOT.jar /app/sigelab.jar
COPY target/docker-libs/ /app/lib/

CMD ["java", "-cp", "/app/sigelab.jar:/app/lib/*", "umg.sigelab.Main"]
