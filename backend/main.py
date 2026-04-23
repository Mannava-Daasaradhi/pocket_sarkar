"""
Pocket Sarkar — FastAPI backend skeleton.
Phase 1: /health endpoint only.
"""
from fastapi import FastAPI

app = FastAPI(
    title="Pocket Sarkar API",
    description="Backend for the Pocket Sarkar offline-first Android app.",
    version="0.1.0-phase1",
)


@app.get("/health")
def health() -> dict:
    """Liveness check — CI and deployment monitors hit this."""
    return {"status": "ok"}
