FROM python:3.11.5-slim

WORKDIR /app

# Upgrade pip first
RUN python -m pip install --upgrade pip

# Copy dependency file first (to leverage Docker cache)
COPY requirements.txt .

# Install all dependencies from requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

RUN pip install --no-cache-dir \
    opentelemetry-sdk \
    opentelemetry-exporter-otlp \
    opentelemetry-instrumentation \
    opentelemetry-instrumentation-logging \
    opentelemetry-instrumentation-requests \
    opentelemetry-instrumentation-kafka-python

# Copy the rest of the application
COPY . .

# Run your training script
CMD ["opentelemetry-instrument","--traces_exporter", "otlp","--metrics_exporter", "none","--service_name", "event-producer","python", "producer.py"]
