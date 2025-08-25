import numpy as np
from sklearn.ensemble import IsolationForest
import joblib
import os

print("Training anomaly detection model...")

# --- 1. Generate Sample Data ---
# Create some normal, inlier data
rng = np.random.RandomState(42)
X_train = 0.2 * rng.randn(1000, 1) + 50

# --- 2. Train the Model ---
# IsolationForest is effective for anomaly detection
# contamination='auto' is a good default
model = IsolationForest(n_estimators=100, contamination='auto', random_state=rng)
model.fit(X_train)

print("Model training complete.")

# --- 3. Save the Model ---
# Save the trained model to a file for the Flask app to use
model_path = 'model.joblib'
joblib.dump(model, model_path)

print(f"Model saved to {os.path.abspath(model_path)}")