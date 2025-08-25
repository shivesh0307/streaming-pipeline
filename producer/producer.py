import os
import json
import time
import random
from kafka import KafkaProducer, errors

KAFKA_BOOTSTRAP = os.environ.get("KAFKA_BOOTSTRAP", "kafka:9092")
TOPIC = os.environ.get("TOPIC", "raw-events")
RATE_PER_SEC = float(os.environ.get("RATE_PER_SEC", "1"))

for attempt in range(10):
    try:
        producer = KafkaProducer(
            bootstrap_servers=KAFKA_BOOTSTRAP,
            value_serializer=lambda v: json.dumps(v).encode("utf-8")
        )
        print("✅ Connected to Kafka!")
        break
    except errors.NoBrokersAvailable:
        print(f"⚠️ Kafka not ready yet... retrying {attempt+1}/10")
        time.sleep(5)
else:
    raise Exception("❌ Could not connect to Kafka after 10 retries")


sensor_ids = ["sensor-1", "sensor-2", "sensor-3"]

print(f"Producing to {TOPIC} at {KAFKA_BOOTSTRAP} ({RATE_PER_SEC} msg/sec)")

while True:
    event = {
        "sensorId": random.choice(sensor_ids),
        "timestamp": int(time.time() * 1000),
        "value": round(random.uniform(0, 120), 2)
    }
    producer.send(TOPIC, event)
    print("Produced:", event)
    time.sleep(1.0 / RATE_PER_SEC)