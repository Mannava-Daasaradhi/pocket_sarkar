# Pocket Sarkar — Folder Structure

```
pocket-sarkar/
│
├── android/                          ← Android app (Kotlin) — main deliverable
│   └── app/src/main/
│       ├── kotlin/com/pocketsarkar/
│       │   ├── ui/
│       │   │   ├── screens/          ← One file per screen (Home, Decoder, Schemes…)
│       │   │   └── components/       ← Reusable Compose components
│       │   ├── ai/
│       │   │   ├── mediapipe/        ← Gemma 4 E4B LlmInference wrapper
│       │   │   └── ollama/           ← Ollama HTTP client (server bridge path)
│       │   ├── db/
│       │   │   ├── dao/              ← Room DAOs (SchemeDao, RightsDao…)
│       │   │   └── entities/         ← Room entities (Scheme, EligibilityRule…)
│       │   ├── modules/
│       │   │   ├── decoder/          ← Document Decoder logic
│       │   │   ├── schemes/          ← Scheme Explainer + function calling
│       │   │   ├── radar/            ← Opportunity Radar
│       │   │   ├── formpilot/        ← Form Co-Pilot
│       │   │   └── rights/           ← Rights Companion
│       │   ├── voice/
│       │   │   ├── stt/              ← Speech-to-Text pipeline
│       │   │   └── tts/              ← Text-to-Speech pipeline
│       │   └── utils/
│       ├── res/                      ← layouts, drawables, strings, raw audio
│       └── assets/models/            ← Gemma 4 weights (gitignored, ~1.6GB)
│
├── backend/                          ← FastAPI + Celery (Ollama path + delta sync)
│   ├── api/routes/                   ← /schemes  /decode  /rights  /sync
│   ├── core/
│   │   ├── eligibility/              ← Python eligibility rule engine
│   │   └── sync/                     ← Weekly delta sync (<200KB)
│   └── tests/
│
├── ai/                               ← All ML work
│   ├── fine_tuning/
│   │   ├── dataset/raw/              ← Counsellor transcripts, scheme Q&A pairs
│   │   ├── dataset/cleaned/          ← After normalization
│   │   ├── dataset/augmented/        ← After dialect augmentation
│   │   ├── scripts/train.py          ← Unsloth LoRA training (Kaggle T4)
│   │   └── checkpoints/             ← Training checkpoints (gitignored)
│   ├── prompts/
│   │   ├── decoder/                  ← Document Decoder system prompt
│   │   ├── schemes/                  ← Scheme Explainer + fake detection
│   │   ├── rights/                   ← Rights Companion
│   │   └── radar/                    ← Opportunity Radar
│   └── eval/
│       ├── benchmarks/               ← Test query sets per module
│       └── results/                  ← Eval output logs
│
├── data/
│   ├── raw/central_schemes/          ← From myscheme.gov.in + gazette (gitignored)
│   ├── raw/state_schemes/            ← Per-state (gitignored)
│   ├── raw/scholarships/             ← From scholarships.gov.in (gitignored)
│   ├── processed/                    ← Cleaned JSON (gitignored)
│   └── schemes/sqlite/               ← pocket_sarkar.db + FTS5 (gitignored)
│
├── whatsapp/                         ← Meta Cloud API bridge
│   ├── webhook/                      ← Incoming message handler
│   ├── handlers/                     ← Intent routing
│   └── formatters/                   ← Format AI output for WhatsApp
│
├── deployment/
│   ├── ollama/                       ← Modelfile + setup script
│   ├── raspberry_pi/SETUP.md         ← Village CSC guide (~20 min)
│   ├── docker/                       ← Docker Compose for backend
│   └── kaggle_notebook/              ← Supporting files for Kaggle path
│
├── notebooks/
│   └── pocket_sarkar_demo.ipynb      ← Runnable submission notebook (Kaggle T4)
│
├── scripts/
│   ├── download_model/               ← download_model.py --model e4b-int4
│   ├── build_db/                     ← Build SQLite from processed JSON
│   ├── sync_schemes/                 ← Delta sync utility
│   └── export_apk/                   ← Signed release APK helper
│
├── tests/                            ← Co-Lead writes bug reports here
│   ├── android/                      ← Instrumentation tests
│   ├── backend/                      ← pytest suite
│   ├── ai/                           ← Module eval tests
│   └── integration/                  ← End-to-end flows
│
├── assets/
│   ├── demo_video/                   ← 3-min submission video (Co-Lead records)
│   ├── screenshots/                  ← Judge screenshots (Co-Lead captures)
│   ├── sample_docs/                  ← Test docs for QA (rental, loan T&C, notice)
│   └── press/                        ← Logos, banners
│
├── docs/
│   ├── phases/                       ← Per-phase notes and decisions
│   ├── api/                          ← API reference
│   ├── architecture/                 ← System diagrams
│   └── team/
│       ├── PROJECT_PLAN.md           ← Phases + team roles (this doc)
│       └── FOLDER_STRUCTURE.md       ← ← YOU ARE HERE
│
├── .github/
│   ├── workflows/                    ← CI: Android lint + Python lint
│   └── ISSUE_TEMPLATE/bug_report.md  ← Co-Lead uses this for every bug
│
├── demo_server.py                    ← python demo_server.py → localhost:8080
├── requirements.txt                  ← Python deps
├── .gitignore
├── LICENSE                           ← Apache 2.0
└── README.md                         ← Hackathon submission README
```

## Rules

1. **Model weights never go to git** — `android/app/src/main/assets/models/` is gitignored. Fetch via `scripts/download_model/`.
2. **Secrets never go to git** — API keys, Meta webhook tokens → `.env` (gitignored).
3. **Prompts are versioned files** in `ai/prompts/` — never hardcode a prompt in Kotlin or Python.
4. **Bug reports** from Co-Lead go in `tests/` as `.md` files using the issue template.
5. **One module per folder** — each of the 5 modules in `android/.../modules/` is self-contained.
6. **Raw data is gitignored** — regenerate with `scripts/build_db/`. Only the script is versioned.
