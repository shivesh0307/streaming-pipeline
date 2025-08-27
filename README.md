# Real-Time Anomaly Detection Pipeline

This project implements a **real-time data streaming pipeline** using Kafka, Flink, Cassandra, Prometheus, Grafana, and Jaeger. It simulates IoT sensor events, scores them with a Python-based anomaly detection service, and persists results in Cassandra with full observability.

---

## 🚀 Architecture

1. **Producer (Python)**

   * Generates random sensor data at a configurable rate.
   * Publishes messages to a Kafka topic (`raw-events`).
   * Instrumented with OpenTelemetry for distributed tracing.

2. **Kafka + Zookeeper**

   * Kafka ingests and buffers streaming events.
   * Zookeeper manages Kafka metadata.

3. **Flink (Java)**

   * Reads events from Kafka (`raw-events`).
   * Calls the **Anomaly Detection Service** asynchronously.
   * Applies fault-tolerant, non-blocking retries and fallbacks.
   * Writes enriched results into Cassandra.

4. **Anomaly Detection Service (Python)**

   * Exposes a REST API (`/score`) that accepts an event and returns anomaly + score.
   * Optional: `/healthz` endpoint for Docker health checks.

5. **Cassandra**

   * Stores results in keyspace `flink_keyspace`, table `events`.
   * Schema auto-created by the Flink sink if missing.

6. **Observability**

   * **Prometheus** scrapes Kafka, Cassandra, and Flink metrics.
   * **Grafana** visualizes dashboards.
   * **Jaeger** provides distributed tracing via OpenTelemetry.

---

## 🛠️ Setup & Run

### Prerequisites

* Docker & Docker Compose
* Maven (if building Flink job locally)

### Build & Start

```bash
docker compose build
docker compose up -d
```

### Stop

```bash
docker compose down -v
```

---

## 🔎 Testing Each Component

### 1. **Producer**

* Logs should show:

    ```
    ✅ Connected to Kafka!
    Produced: {"sensorId": "sensor-1", "timestamp": 1690000000000, "value": 42.1}
    ```

* To test manually:

    ```
    docker compose logs -f producer
    ```


### 2. **Kafka**
- Verify topics:

    ```
    docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --list
    ```

* Consume messages directly:

    ```
    docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic raw-events --from-beginning
    ```

---

### 3. **Anomaly Detection Service**
- Health check:

    ```
    curl http://localhost:8000/healthz
    ```

* Score API test:

    ```
    curl -X POST http://localhost:8000/score -H "Content-Type: application/json" -d '{"sensorId": "sensor-1", "timestamp": 1690000000000, "value": 50}'
    ```

---

### 4. **Cassandra**
- Connect:

    ```
    docker compose exec cassandra cqlsh
    ```

* Query data:

    ```sql
    USE flink_keyspace;
    SELECT * FROM events LIMIT 10;
    ```

---

### 5. **Flink**
- UI: [http://localhost:8081](http://localhost:8081)
- Logs:

    ```
    docker compose logs -f flink-job
    ```

---

### 6. **Prometheus**
- UI: [http://localhost:9090](http://localhost:9090)  
- Example query:

    ```
    flink_taskmanager_job_task_operator_numRecordsIn
    ```

### 7. **Grafana**

* UI: [http://localhost:3000](http://localhost:3000)
* Login: `admin / admin`
* Dashboards auto-provisioned from `grafana/provisioning`.

### 8. **Jaeger**

* UI: [http://localhost:16686](http://localhost:16686)
* Check `event-producer` or `anomaly-service` traces.

---

## ⚡ Reliability Features

* Flink async client with retry + fallback.
* Cassandra sink auto-creates schema.
* Restart strategy: `fixedDelayRestart(10, 10s)`.
* Docker health checks for dependencies.

---

## 📂 Repo Structure

```
producer/           # Python Kafka producer
anomaly-service/    # Python anomaly detection API
flink-job/          # Java Flink job
    ├── Dockerfile
    ├── src/main/java/com/example/pipeline
otel/               # OpenTelemetry collector config
prometheus/         # Prometheus config
grafana/            # Grafana provisioning (dashboards, datasources)
jmx-exporter/       # JMX exporter configs for Kafka, Cassandra
```

---

## ✅ Next Steps

* Add richer Grafana dashboards (latency, throughput, error rates).
* Use Jaeger traces to correlate producer → Flink → anomaly-service → Cassandra.
* Deploy in Kubernetes for scaling.
