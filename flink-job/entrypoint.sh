#!/usr/bin/env bash
set -euo pipefail

wait_for() {
  local host="$1" port="$2" name="${3:-$host:$port}"
  echo "⏳ Waiting for $name..."
  for i in {1..60}; do
    # Bash TCP check; no netcat needed
    if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
      echo "✅ $name is up"
      return 0
    fi
    sleep 2
  done
  echo "❌ Timeout waiting for $name"
  exit 1
}

wait_for jobmanager 8081 "Flink JobManager (REST 8081)"
# (optional) wait_for kafka 9092 "Kafka"
# (optional) wait_for cassandra 9042 "Cassandra"

echo "🚀 Submitting Flink job..."
/opt/flink/bin/flink run -d \
  -c com.example.pipeline.StreamingJob \
  /opt/flink/usrlib/job.jar

# keep container alive so logs remain accessible
tail -f /dev/null
