# Pocket Sarkar — Ollama Setup

## Laptop / Demo Server (judges use this)

```bash
# 1. Install Ollama  →  https://ollama.com/download
# 2. Pull base model (~3 GB)
ollama pull gemma4:e4b

# 3. Create Pocket Sarkar model with system prompt
ollama create pocket-sarkar -f deployment/ollama/Modelfile

# 4. Test it
ollama run pocket-sarkar "PM Kisan mein kitna paisa milta hai?"

# 5. Start API server (OllamaClient.kt connects to this)
ollama serve          # runs on localhost:11434
```

## Raspberry Pi 5 — Village CSC Deployment

```bash
# Needs: Raspberry Pi 5, 16 GB RAM, 32 GB SD card
# Estimated setup time: ~20 min on good broadband

# 1. Install Ollama on Pi
curl -fsSL https://ollama.com/install.sh | sh

# 2. Pull the larger model (~16 GB — overnight download on village connection)
ollama pull gemma4:27b

# 3. Edit Modelfile: change  FROM gemma4:e4b  →  FROM gemma4:27b
# 4. Create model
ollama create pocket-sarkar-pi -f deployment/ollama/Modelfile

# 5. Start server (accessible on local network)
OLLAMA_HOST=0.0.0.0:11434 ollama serve

# 6. Point the Android app to Pi's IP
#    In OllamaClient.kt, set:  baseUrl = "http://<pi-ip>:11434"
```

## Verify it's working

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "pocket-sarkar",
  "prompt": "Kisan Credit Card ke liye kaun eligible hai?",
  "stream": false
}'
```

Expected: JSON response with `"response"` field containing scheme details.