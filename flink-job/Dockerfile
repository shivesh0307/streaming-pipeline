# Stage 1: build shaded JAR
FROM maven:3.9.6-eclipse-temurin-11 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: lightweight runner that submits the job
FROM flink:1.17.1-scala_2.12

# copy job jar
COPY --from=builder /app/target/flink-job-1.0-SNAPSHOT-shaded.jar /opt/flink/usrlib/job.jar

# copy entrypoint
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
