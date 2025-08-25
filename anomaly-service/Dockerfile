FROM python:3.11.5-slim

WORKDIR /app

# Upgrade pip
RUN python -m pip install --upgrade pip

# Copy dependency file first (for caching)
COPY requirements.txt .

# Install all dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application
COPY . .

# Run the training script
RUN python model_train.py

# Expose port and run the web server
EXPOSE 8000
CMD ["flask", "run", "--host=0.0.0.0", "--port=8000"]
