
import argparse
import os
import joblib
import numpy as np
import pandas as pd
import yfinance as yf

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, classification_report

MODEL_DIR = "models"
MODEL_PATH = os.path.join(MODEL_DIR, "cortex_model.pkl")

FEATURES = [
    "return_1d",
    "return_5d",
    "return_10d",
    "sma_5",
    "sma_10",
    "sma_20",
    "volatility_5",
    "volatility_10",
    "volatility_20",
    "rsi",
    "volume_change",
    "volume_ratio",
    "macd",
    "atr"
]


# --------------------------------
# 1. DOWNLOAD MARKET DATA
# --------------------------------

def download_data(ticker):

    data = yf.download(
        ticker,
        period="5y",
        interval="1d",
        auto_adjust=True,
        progress=False
    )

    if data.empty:
        raise ValueError("No market data found.")

    if isinstance(data.columns, pd.MultiIndex):
        data.columns = data.columns.get_level_values(0)

    data.columns = [str(c).lower() for c in data.columns]

    return data.dropna()


# --------------------------------
# 2. FEATURE ENGINEERING
# --------------------------------

def create_features(data):

    df = data.copy()

    close = df["close"]
    volume = df["volume"]

    returns = close.pct_change()

    features = pd.DataFrame(index=df.index)

    features["return_1d"] = returns
    features["return_5d"] = close.pct_change(5)
    features["return_10d"] = close.pct_change(10)

    for period in [5, 10, 20]:

        sma = close.rolling(period).mean()

        features[f"sma_{period}"] = close / sma - 1

        features[f"volatility_{period}"] = (
            returns.rolling(period).std() * np.sqrt(252)
        )

    # RSI

    delta = close.diff()

    gain = delta.clip(lower=0).ewm(
        alpha=1/14, adjust=False
    ).mean()

    loss = (-delta.clip(upper=0)).ewm(
        alpha=1/14, adjust=False
    ).mean()

    rs = gain / loss.replace(0, np.nan)

    features["rsi"] = 100 - (100 / (1 + rs))

    # Volume indicators

    features["volume_change"] = volume.pct_change()

    features["volume_ratio"] = (
        volume / volume.rolling(20).mean()
    )

    # MACD

    ema12 = close.ewm(span=12, adjust=False).mean()

    ema26 = close.ewm(span=26, adjust=False).mean()

    features["macd"] = (ema12 - ema26) / close

    # Average True Range

    previous_close = close.shift(1)

    true_range = pd.concat([
        df["high"] - df["low"],
        abs(df["high"] - previous_close),
        abs(df["low"] - previous_close)
    ], axis=1).max(axis=1)

    features["atr"] = (
        true_range.rolling(14).mean() / close
    )

    return features.replace(
        [np.inf, -np.inf], np.nan
    )


# --------------------------------
# 3. TRAIN ML MODELS
# --------------------------------

def train_model(ticker):

    print("Downloading market data...")

    raw = download_data(ticker)

    print("Creating ML features...")

    X = create_features(raw)

    next_return = raw["close"].pct_change().shift(-1)

    dataset = X.copy()

    dataset["next_return"] = next_return

    dataset["risk_volatility"] = X["volatility_20"]

    dataset = dataset.dropna()

    if len(dataset) < 300:
        raise ValueError("Not enough historical data.")

    # Direction labels

    threshold = 0.001

    dataset["direction"] = np.where(
        dataset["next_return"] > threshold,
        "UP",
        np.where(
            dataset["next_return"] < -threshold,
            "DOWN",
            "SIDEWAYS"
        )
    )

    # Chronological split

    split = int(len(dataset) * 0.8)

    X_train = dataset[FEATURES].iloc[:split]

    X_test = dataset[FEATURES].iloc[split:]

    y_train = dataset["direction"].iloc[:split]

    y_test = dataset["direction"].iloc[split:]

    # Direction classifier

    direction_model = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),

        ("classifier", RandomForestClassifier(
            n_estimators=300,
            max_depth=8,
            min_samples_leaf=5,
            class_weight="balanced",
            random_state=42,
            n_jobs=-1
        ))
    ])

    print("Training direction prediction model...")

    direction_model.fit(X_train, y_train)

    predictions = direction_model.predict(X_test)

    print("\nDirection model accuracy:")

    print(accuracy_score(y_test, predictions))

    print("\nClassification report:")

    print(classification_report(
        y_test,
        predictions,
        zero_division=0
    ))

    # Risk classification

    training_volatility = dataset[
        "risk_volatility"
    ].iloc[:split]

    low_threshold = training_volatility.quantile(0.33)

    high_threshold = training_volatility.quantile(0.67)

    risk_labels = np.where(
        dataset["risk_volatility"] <= low_threshold,
        "LOW",
        np.where(
            dataset["risk_volatility"] <= high_threshold,
            "MEDIUM",
            "HIGH"
        )
    )

    risk_model = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),

        ("classifier", RandomForestClassifier(
            n_estimators=300,
            max_depth=8,
            min_samples_leaf=5,
            class_weight="balanced",
            random_state=42,
            n_jobs=-1
        ))
    ])

    print("\nTraining risk classification model...")

    risk_model.fit(
        X_train,
        risk_labels[:split]
    )

    risk_predictions = risk_model.predict(X_test)

    print("\nRisk model accuracy:")

    print(accuracy_score(
        risk_labels[split:],
        risk_predictions
    ))

    # Save trained models

    os.makedirs(MODEL_DIR, exist_ok=True)

    joblib.dump({
        "direction_model": direction_model,
        "risk_model": risk_model,
        "features": FEATURES,
        "ticker": ticker,
        "trained_through": str(dataset.index[split - 1].date()),
        "low_threshold": float(low_threshold),
        "high_threshold": float(high_threshold)
    }, MODEL_PATH)

    print("\nMODEL TRAINING COMPLETED!")

    print("Saved model:", MODEL_PATH)


# --------------------------------
# 4. LOAD TRAINED MODEL
# --------------------------------

app = FastAPI(
    title="CORTEX ML Prediction System",
    description="Machine Learning Market Prediction API",
    version="1.0"
)

model_data = None


@app.on_event("startup")
def load_model():

    global model_data

    if os.path.exists(MODEL_PATH):

        model_data = joblib.load(MODEL_PATH)

        print("CORTEX ML model loaded successfully!")

    else:

        print("Model not found. Train the model first.")


# --------------------------------
# 5. HEALTH CHECK
# --------------------------------

@app.get("/health")
def health():

    return {
        "status": "running",
        "model_loaded": model_data is not None
    }


# --------------------------------
# 6. MARKET PREDICTION API
# --------------------------------

class PredictionRequest(BaseModel):

    ticker: str = "SPY"


@app.post("/predict")
def predict(request: PredictionRequest):

    if model_data is None:

        raise HTTPException(
            status_code=503,
            detail="Train the model first."
        )

    ticker = request.ticker.strip().upper()

    try:

        print("Predicting market:", ticker)

        raw = yf.download(
            ticker,
            period="6mo",
            interval="1d",
            auto_adjust=True,
            progress=False
        )

        if raw.empty:
            raise ValueError("No market data available.")

        if isinstance(raw.columns, pd.MultiIndex):
            raw.columns = raw.columns.get_level_values(0)

        raw.columns = [
            str(c).lower() for c in raw.columns
        ]

        X = create_features(raw).dropna()

        if X.empty:
            raise ValueError("Insufficient data for prediction.")

        latest = X[FEATURES].tail(1)

        direction_model = model_data["direction_model"]

        risk_model = model_data["risk_model"]

        direction = direction_model.predict(latest)[0]

        probabilities = direction_model.predict_proba(latest)[0]

        classes = direction_model.classes_

        confidence = float(max(probabilities))

        risk = risk_model.predict(latest)[0]

        return {
            "project": "CORTEX",

            "ticker": ticker,

            "predicted_direction": str(direction),

            "confidence": round(confidence, 4),

            "probabilities": {
                str(classes[i]): round(
                    float(probabilities[i]), 4
                )
                for i in range(len(classes))
            },

            "risk_level": str(risk),

            "last_close": round(
                float(raw["close"].dropna().iloc[-1]), 2
            ),

            "model_trained_through": model_data[
                "trained_through"
            ],

            "status": "success",

            "disclaimer": "Experimental prediction, not financial advice."
        }

    except Exception as error:

        raise HTTPException(
            status_code=400,
            detail=str(error)
        )


# --------------------------------
# 7. RUN PROGRAM
# --------------------------------

if __name__ == "__main__":

    parser = argparse.ArgumentParser()

    parser.add_argument(
        "mode",
        choices=["train", "serve"]
    )

    parser.add_argument(
        "--ticker",
        default="SPY"
    )

    args = parser.parse_args()

    if args.mode == "train":

        train_model(args.ticker.upper())

    elif args.mode == "serve":

        import uvicorn

        uvicorn.run(
            app,
            host="127.0.0.1",
            port=8001
        )
