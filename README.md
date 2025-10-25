# Van Guardian - Server
Van Guardian is a Ktor HTTP service that analyzes images of a camper-van interior and returns a concise safety assessment. It uses LLMs via **Ollama** to produce image descriptions and translations.

## How it works

* Accepts an image as input.
* Produces a single JSON result describing whether a safety alert is present, a short reason, and a severity level (low / medium / high).
* (Optional) If an alert is present, the human-readable reason is translated to Spanish.

## API contract

* `/analyze`: inputs an image and returns a JSON:

    * `alert -> boolean`: If LLM detects a safety issue
    * `reason -> string`: Human-readable explanation of the issue
    * `severity -> enum (nullable)`: `low`, `medium`, `high`

### Example request

```bash
curl -X POST --data-binary @photo.jpg http://localhost:8080/analyze -H "Content-Type: image/jpeg"
```

### Example responses

**No alert detected:**

```json
{
  "alert": false,
  "reason": "No suspicious activity detected"
}
```

**Alert detected:**

```json
{
  "alert": true,
  "reason": "Smoke detected",
  "severity": "medium"
}
```

## Ollama connection

Vanguardian requires access to an **Ollama server** to perform its analysis.

### Setup instructions

1. **Install Ollama**: [https://ollama.com/download](https://ollama.com/download)
2. **Start the Ollama server** via CLI (for GUI instructions, go to 4):

   ```bash
   ollama serve
   ```
3. Load or pull the required models:

   ```bash
   ollama pull llava
   ollama pull gemma3
   ```
   By default, `llava` is used to describe the images and output the json, and `gemma3` is used to translate the `reason` to spanish.
   
4. (Optional) You can also **run it via GUI**, by opening it and running any prompt using the required models, so the Ollama service is started and ready in the background
5. Ensure it is reachable:

   ```bash
   curl http://localhost:11434/api/tags
   ```
6. (Optional) If using a different host or port, set the service’s Ollama URL through configuration (`OllamaUrl` const).

