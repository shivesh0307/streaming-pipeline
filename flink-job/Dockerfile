# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-11 AS builder
WORKDIR /app

# Copy pom + src
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: Flink runtime
FROM flink:1.17.1-scala_2.12

# Copy built job JAR into Flink’s userlib dir
COPY --from=builder /app/target/flink-job-1.0-SNAPSHOT.jar /opt/flink/usrlib/job.jar

# ✅ Explicitly call standalone-job.sh
CMD ["/opt/flink/bin/standalone-job.sh", \
     "start-foreground", \
     "--job-classname", "com.example.pipeline.StreamingJob"]
