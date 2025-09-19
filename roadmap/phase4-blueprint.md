# Phase 4 Blueprint — Semantic Search Upgrade (1–2 weeks)

Objective
- Replace toy char-frequency embeddings with real semantic embeddings and faster search.

Option A: TensorFlow Lite TextSearcher (On-Device)
- Use TFLite text embedding model
- Generate vectors on-device; store vectors in DB
- Use ScaNN search or approximate NN

Option B: sqlite-vec extension
- Integrate sqlite-vec for vector columns and ANN indexes inside SQLite
- Store float vectors efficiently; use cosine/inner-product distance

Option C: ObjectBox Vector DB
- Evaluate ObjectBox for fully embedded vector DB + relations

Steps (choose A or B first)
1) Introduce vector table: task_id -> float[] (BLOB) or vec column
2) Write migration from CSV embeddings
3) Implement embedding generator (TFLite) and adapter interface
4) Implement ANN-backed search query path
5) Add precision/latency tests and compare vs baseline

Acceptance Criteria
- Search quality subjectively better on varied queries
- Latency < 25ms for 500 tasks on Pixel-class device (target)
- Migration works for existing data (dev only)

Risks & Mitigation
- NDK/ABI woes (sqlite-vec) → provide pure Kotlin fallback path
- Model size/perf (TFLite) → select small embedding model and quantization
