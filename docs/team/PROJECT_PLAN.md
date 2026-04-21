# Pocket Sarkar — Project Plan

**Hackathon Deadline: May 18, 2026 · 23:59 UTC**
**Start date: April 21 · Days available: ~27**

---

## Team

| Person | Role | Owns |
|--------|------|------|
| **Tech Lead** | 99% coding | Android, AI pipeline, backend, DB, fine-tuning, Ollama, WhatsApp, all scripts |
| **Co-Lead** | Testing + QA + non-technical delivery | Bug reports, demo video, submission form, screenshots, press kit |

### Co-Lead Task List
- [ ] Write bug reports in `.github/ISSUE_TEMPLATE/bug_report.md` format after each phase checkpoint
- [ ] Collect sample docs for testing → `assets/sample_docs/` (rental agreement, loan T&C, court notice)
- [ ] Record 3-min demo video → `assets/demo_video/` (Phase 10)
- [ ] Upload APK to GitHub Releases (Phase 10)
- [ ] Make GitHub repo public before deadline
- [ ] Fill submission form on Kaggle before May 18 23:59 UTC
- [ ] Screenshot pack for judges → `assets/screenshots/`

---

## 10 Phases

### Phase 1 — Foundation (Days 1–2)
**Goal:** Skeleton that compiles and pushes to GitHub.
- [ ] Android project init (Kotlin, minSdk 26, Compose)
- [ ] FastAPI skeleton with `/health` endpoint
- [ ] MediaPipe LLM Inference dependency added to `build.gradle.kts`
- [ ] GitHub repo + this folder structure pushed
- [ ] `.github/workflows/` CI: Android lint + Python lint on push

**Co-Lead checkpoint:** Clone repo → build → no errors.

---

### Phase 2 — Scheme Database (Days 2–4)
**Goal:** 447 schemes queryable in SQLite on-device.
- [ ] Scrape central schemes → `data/raw/central_schemes/`
- [ ] Write normalization script → `data/processed/`
- [ ] Room DB schema (Scheme, EligibilityRule entities) in `android/.../db/`
- [ ] FTS5 full-text search index
- [ ] Eligibility rule engine (income limits, state, category)
- [ ] `confidence_score` decay logic (0.01/week, stale warning < 0.6)
- [ ] `scripts/build_db/` script

**Co-Lead checkpoint:** Query "PM Kisan" → correct result. Query fake scheme → empty result.

---

### Phase 3 — Gemma 4 On-Device Integration (Days 3–6)
**Goal:** Real Gemma 4 E4B inference running on a physical Android device.
- [ ] `scripts/download_model/download_model.py --model e4b-int4`
- [ ] MediaPipe LlmInference wired in `android/.../ai/mediapipe/`
- [ ] Streaming token output (perceived speed)
- [ ] Model lazy-load (not at app startup)
- [ ] Ollama path working: `deployment/ollama/Modelfile`
- [ ] Basic text query → response on real device

**Co-Lead checkpoint:** App loads. Type a Hindi question. Get a response. Time it.

---

### Phase 4 — Document Decoder (Days 5–9)
**Goal:** Point camera at a document → get plain-language risk analysis.
- [ ] Camera input → Gemma 4 vision encoder (no separate OCR)
- [ ] PDF + screenshot import
- [ ] Production system prompt → `ai/prompts/decoder/system_prompt.txt`
- [ ] Structured output parser (DOCUMENT_TYPE, RED_FLAGS, RISK_SCORE, ACTION, QUESTIONS)
- [ ] Risk badge UI: 🟢 SAFE / 🟡 CAUTION / 🔴 HIGH RISK
- [ ] ML Kit OCR fallback if vision confidence is low
- [ ] Image preprocessing (deskew, denoise, normalize)

**Co-Lead checkpoint:** Scan `assets/sample_docs/rental_agreement.jpg` → verify deposit clause and auto-debit flagged as 🚨. Log any missed flags.

---

### Phase 5 — Scheme Explainer + Function Calling (Days 7–11)
**Goal:** Zero hallucination on scheme details via native function calling.
- [ ] Function schema: `query_scheme_db(query, state, category)`
- [ ] Function call → SQLite → structured fact response
- [ ] Gemma 4 constrained to only explain retrieved facts
- [ ] Fake scheme detector (`ai/prompts/schemes/fake_detection.txt`)
- [ ] Multi-turn conversation state in `android/.../modules/schemes/`

**Co-Lead checkpoint:** "Gorakhpur ke kisan ko kya milega?" → correct schemes + amounts. Made-up scheme → "not found, do not trust."

---

### Phase 6 — Voice I/O (Days 9–13)
**Goal:** Full voice conversation, noisy environment tolerant.
- [ ] MediaPipe STT → `android/.../voice/stt/`
- [ ] Vakyansh (AI4Bharat) as fallback STT
- [ ] Noise suppression preprocessing (spectral subtraction)
- [ ] MediaPipe TTS + IndicTTS → `android/.../voice/tts/`
- [ ] IndicLangDetect auto language detection
- [ ] Transcript confirm UI when STT confidence < threshold

**Co-Lead checkpoint:** Record voice queries with background noise (play traffic audio). Target: <20% transcription failure. Log failures with audio clip.

---

### Phase 7 — Opportunity Radar + Rights Companion (Days 11–15)
**Goal:** Proactive scheme discovery + situation-specific rights guidance.
- [ ] On-device user profile builder (nothing uploaded to server)
- [ ] Profile → eligibility match → proactive scheme list
- [ ] Rights Companion: situation → rights → action steps
- [ ] Sample complaint language generator
- [ ] Offline helpline numbers DB
- [ ] Emergency Mode: panic button → critical rights in 2 taps

**Co-Lead checkpoint:** Farmer profile (UP, ₹80k income, 1.5 hectare) → Opportunity Radar shows PM Kisan + 2 others. Confirm via network monitor that nothing left the device.

---

### Phase 8 — Unsloth Fine-Tuning (Days 13–18)
**Goal:** 47MB LoRA adapter that makes responses feel native, not translated.
- [ ] 12,400-pair dataset prepared → `ai/fine_tuning/dataset/`
- [ ] Kaggle T4 training notebook → `deployment/kaggle_notebook/`
- [ ] `ai/fine_tuning/scripts/train.py` (Unsloth LoRA config)
- [ ] Export adapter → `ai/fine_tuning/` (gitignored, shared via link)
- [ ] Adapter integrated into MediaPipe inference path
- [ ] Eval on 20 Bhojpuri-inflected test queries

**Co-Lead checkpoint:** Compare base vs. fine-tuned on the 20 test queries. Mark which responses feel "apna sa lagta hai" vs. "padha-likha bolne wala."

---

### Phase 9 — Demo Server + Kaggle Notebook (Days 16–20)
**Goal:** Judges can run everything without an Android device.
- [ ] `demo_server.py` → `localhost:8080`, all 5 modules, Hindi + English
- [ ] `notebooks/pocket_sarkar_demo.ipynb` runs on free Kaggle T4
- [ ] `deployment/raspberry_pi/SETUP.md` (≤20 min village CSC guide)
- [ ] `scripts/download_model/` one-command setup
- [ ] All 4 submission links ready to fill in README

**Co-Lead checkpoint:** Fresh machine → `pip install -r requirements.txt` → `python demo_server.py` → test all 5 modules. Run Kaggle notebook end-to-end. Log any setup friction.

---

### Phase 10 — Polish + Submit (Days 18–27)
**Goal:** Win-ready submission.

**Tech Lead:**
- [ ] Performance pass (startup <3s, inference >10 tok/sec on SD 680)
- [ ] All edge cases handled (bad image quality, no internet, empty DB result)
- [ ] `./gradlew assembleRelease` → signed APK
- [ ] WhatsApp Bridge: at least one working flow (scheme query)
- [ ] README submission links filled
- [ ] Repo set to public (Apache 2.0)

**Co-Lead:**
- [ ] 3-min demo video shot on real device (all 5 modules, Hindi voice)
- [ ] Upload APK to GitHub Releases
- [ ] Kaggle notebook shared publicly
- [ ] Screenshot pack (10 key screens) → `assets/screenshots/`
- [ ] Submission form submitted on Kaggle before May 18 23:59 UTC

---

## Phase Dependency Order

```
P1 Foundation
└── P2 Database ──────────────────────────────┐
        └── P3 Gemma 4 Integration             │
                ├── P4 Document Decoder        │
                ├── P5 Scheme Explainer ←──────┘
                │       └── P7 Radar + Rights
                └── P6 Voice I/O
P8 Fine-tuning (parallel, start Day 13)
P9 Demo Server (needs P3–P7)
P10 Polish + Submit (needs everything)
```

---

## Final Submission Checklist

- [ ] Public GitHub repo (Apache 2.0 license)
- [ ] README has all 4 links: demo video, repo, Kaggle notebook, APK
- [ ] Kaggle notebook runs end-to-end on T4
- [ ] Android APK downloadable and installable on Android 8+
- [ ] Demo video: ≤3 min, real device, all 5 modules shown
- [ ] Submitted on Kaggle before **May 18, 2026 · 23:59 UTC**
