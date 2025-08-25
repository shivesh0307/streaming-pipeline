from flask import Flask, request, jsonify
import joblib
import numpy as np
import logging
import os

# Set up basic logging
logging.basicConfig(level=logging.INFO)

app = Flask(__name__)

# --- Load the pre-trained model ---
MODEL_PATH = 'model.joblib'
model = None

try:
    model = joblib.load(MODEL_PATH)
    logging.info(f"Model loaded successfully from {os.path.abspath(MODEL_PATH)}")
except FileNotFoundError:
    logging.error(f"Model file not found at {MODEL_PATH}. Make sure to run model_train.py first.")
    # Exit or handle gracefully if the model is essential for the app to run
    exit(1)


@app.route('/score', methods=['POST'])
def score():
    """
    Scores an event for anomalies using a pre-trained IsolationForest model.
    """
    if not request.is_json:
        return jsonify({"error": "Request must be JSON"}), 400

    data = request.get_json()
    value = data.get('value')

    if value is None:
        return jsonify({"error": "Missing 'value' in request body"}), 400

    try:
        # Prepare the data for the model (must be a 2D array)
        input_data = np.array([[float(value)]])

        # Get prediction: -1 for anomalies (outliers), 1 for inliers
        prediction = model.predict(input_data)
        is_anomaly = bool(prediction[0] == -1)

        # Get anomaly score: lower scores are more abnormal
        anomaly_score = model.score_samples(input_data)[0]

        response = {
            'anomaly': is_anomaly,
            'score': anomaly_score
        }
        logging.info(f"Scored event with value {value}: {response}")
        return jsonify(response)

    except (ValueError, TypeError) as e:
        logging.error(f"Error processing value '{value}': {e}")
        return jsonify({"error": "Invalid 'value' format, must be a number"}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)