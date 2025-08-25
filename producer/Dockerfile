FROM python:3.11.5-slim

WORKDIR /app

# Upgrade pip first
RUN python -m pip install --upgrade pip

# Copy dependency file first (to leverage Docker cache)
COPY requirements.txt .

# Install all dependencies from requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application
COPY . .

# Run your training script
CMD ["python", "producer.py"]
