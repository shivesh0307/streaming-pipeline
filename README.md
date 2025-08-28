# Real-Time Anomaly Detection Pipeline

This project implements a **real-time data streaming pipeline** using Kafka, Flink, Cassandra, Prometheus, Grafana, and Jaeger. It simulates IoT sensor events, scores them with a Python-based anomaly detection service, and persists results in Cassandra with full observability.

<img width="200" height="1000" alt="Untitled diagram _ Mermaid Chart-2025-08-28-044824" src="https://github.com/user-attachments/assets/e4c3205b-09ae-4ec0-b141-9a196d574d00" />


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

<img width="1907" height="1075" alt="docker desktop" src="https://github.com/user-attachments/assets/52f2d9de-764c-4abe-be78-26e46fe71df5" />


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
    
<img width="1567" height="842" alt="producer" src="https://github.com/user-attachments/assets/30f0bcf7-53a4-4b9e-8552-762591aac233" />


### 2. **Kafka**
- Verify topics:

    ```
    docker compose exec kafka kafka-topics --bootstrap-server kafka:9092 --list
    ```
    
<img width="1192" height="136" alt="topic_created_kafka" src="https://github.com/user-attachments/assets/a37843de-1bed-4f34-949e-5f8e4ed7b06d" />

* Consume messages directly:

    ```
    docker compose exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic raw-events --from-beginning
    ```
    
<img width="1193" height="652" alt="kafka_processing_messages" src="https://github.com/user-attachments/assets/18cbe780-66c5-4d6a-8545-8967512959ae" />


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
    
<img width="1252" height="785" alt="flink_job_inserting_data_into_cassandra" src="https://github.com/user-attachments/assets/bd4901ae-2112-4550-be4e-ffa72bbcb898" />

---

### 5. **Flink**
- UI: [http://localhost:8081](http://localhost:8081)

<img width="1918" height="1021" alt="flink_job" src="https://github.com/user-attachments/assets/c0ebeeb2-99df-455b-9610-51c542d4fb6c" />

<img width="1916" height="1023" alt="flink_job_running" src="https://github.com/user-attachments/assets/bc8caf3e-35fc-497d-a614-809738da3d89" />

- Logs:

    ```
    docker compose logs -f flink-job
    ```

---

### 6. **Prometheus**
- UI: [http://localhost:9090](http://localhost:9090)  

<img width="1918" height="1021" alt="Prometheus_for_observability" src="https://github.com/user-attachments/assets/6c546970-7309-44ac-a357-518b6035e554" />

- Example query:

    ```
    flink_taskmanager_job_task_operator_numRecordsIn
    ```

### 7. **Grafana**

* UI: [http://localhost:3000](http://localhost:3000)
* Login: `admin / admin`
* Dashboards auto-provisioned from `grafana/provisioning`.

<img width="1918" height="941" alt="grafana_dashboard" src="https://github.com/user-attachments/assets/add9a14e-24ef-4446-934f-0433a7654b68" />

<img width="1916" height="1023" alt="grafana_cassandra_metrics" src="https://github.com/user-attachments/assets/dd25042a-7b59-42dd-a4e3-b9a99b44bc5c" />

<img width="1911" height="1022" alt="grafana_flink_job_metrics" src="https://github.com/user-attachments/assets/02327a50-1561-4766-9034-e324c5569cf8" />

### 8. **Jaeger**

* UI: [http://localhost:16686](http://localhost:16686)
* Check `event-producer` or `anomaly-service` traces.

---
### 9. **Complete Architecture**

<img width="3459" height="1379" alt="image" src="https://github.com/user-attachments/assets/a52828cf-6a51-4e89-8364-4c8eb87c54ce" />

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
