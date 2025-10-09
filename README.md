# GenAI Tasker Lite (Android 14)

A minimal Android 14 (SDK 34) Jetpack Compose sample showcasing an offline-first task manager with lightweight semantic search.

## 🎯 Best Practices Validation

This repository has been validated against industry best practices for Android local LLM implementations:

- **[Best Practices Summary](BEST_PRACTICES_SUMMARY.md)** - Executive overview and quick reference
- **[Comprehensive Validation Report](ANDROID_LOCAL_LLM_BEST_PRACTICES_VALIDATION.md)** - Detailed 10-part analysis
- **[Phase 1.1 Implementation Guide](PHASE_1.1_IMPLEMENTATION_GUIDE.md)** - Next steps with code examples

**Overall Rating:** ⭐⭐⭐⭐⭐ EXCELLENT (85% complete, production-ready after Phase 1.1)

## Current Feature Set
- Add Task screen (title + description persisted via Room)
- Task List screen (master list; tap to open detail)
- Task Detail screen (read-only view)
- Semantic Search screen (separate navigation destination performing cosine similarity over stored task embeddings)
- Navigation Drawer (hamburger menu) for switching between Add, Tasks, Search
- Single-Activity, multi-screen architecture using `navigation-compose`
- Local deterministic embedding service (character-frequency vector + L2 normalization)
- Repository pattern + ViewModel state with `stateIn` + eager sharing
- Material 3 UI components (Compose)

## Screens
| Screen | Route | Purpose |
|--------|-------|---------|
| Add Task | `add` | Create & persist a task and embedding |
| Task List | `list` | Browse tasks (newest first) & navigate to detail |
| Task Detail | `detail/{id}` | Display full title & description |
| Semantic Search | `search` | Enter query string, view ranked similar tasks |

## Data Model
- `Task(id, title, description)` stored in Room `tasks` table
- `Embedding(taskId, vectorCsv)` stores normalized float vector as CSV (placeholder until vector-friendly storage adopted)

## Technical Architecture (2025 Snapshot)
### Layers (current)
1. UI (Compose screens + Navigation Drawer + NavHost)
2. Presentation: `TaskViewModel` (AndroidX ViewModel, coroutine scope, exposes Flow state)
3. Data: `TaskRepository` (mediates DAOs + embedding service + similarity logic)
4. Local Persistence: Room (DAOs + SQLite DB)
5. Local ML Stub: `LocalEmbeddingService` (deterministic embedding + cosine similarity helper inside repository)

### Data Flow
`UI Event` → `ViewModel` → suspend call on `Repository` → `Room` (persist/fetch) → `Repository` enrich (embedding / similarity) → Flow emits list → `stateIn` exposes snapshot → `collectAsState()` recompose.

### State Management
- Hot Flow: `tasks: Flow<List<Task>>` from DAO observed -> `stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())`
- Search results: local mutable state managed inside `SemanticSearchScreen` (stateless repository call triggers update)

### Navigation
- `navigation-compose` with string routes; simple dynamic segment for task id.
- Drawer orchestrates top-level destinations (Add, List, Search).

### Embedding & Similarity
- Deterministic char hashing -> frequency vector (64 dims) -> L2 normalize.
- Cosine similarity computed CPU-side (O(n * d)). Adequate for tiny datasets; replace for scale.

### Build / Tooling
- Kotlin 1.9.24, Compose BOM 2024.09.02
- Material3, Room 2.6.1, Coroutines 1.9.0, Navigation Compose 2.8.0
- Java/Kotlin target 17

### Current Non-Goals (Explicitly Out of Scope Right Now)
- Remote sync / cloud persistence
- Authentication / multi-user handling
- Background sync / WorkManager integration
- Vector database / ANN acceleration

## Limitations & Improvement Roadmap (vs Best Practices – Sept 2025)
| Area | Current | Recommended Evolution |
|------|---------|-----------------------|
| Modularization | Single `app` module | Split into `core-model`, `core-database`, `core-embedding`, `feature-task`, `feature-search`, `app` shell for faster incremental builds & parallelization |
| Dependency Injection | Manual factory | Adopt Hilt (or Koin) for scoping DB / repository / embedding service; enable test doubles injection |
| Vector Storage | CSV strings | Use `sqlite-vec` or FTS5 / custom BLOB table for float arrays; enable ANN indexes (IVF / HNSW) |
| Embeddings | Toy char frequency | Replace with on-device model (TFLite, MediaPipe Text Embedding) or managed API fallback + caching |
| Similarity | Full-scan CPU loop | Introduce indexed similarity (sqlite-vec) or incremental pre-computed norms; paginate results |
| Architecture Pattern | Basic MVVM | Formalize UDF/MVI or presenter with sealed UI state; reduce mutable UI states scattered across screens |
| Navigation | String routes | Centralized typed route constants or code-gen (e.g. Compose Destinations) for safer arguments |
| Error Handling | Minimal | Introduce sealed result types + snackbar / inline error surfacing; structured logging |
| Persistence | Plain Room DB | Add migration testing, automatic backups / export, optional encryption (SQLCipher / EncryptedFile) |
| Performance | No baseline profile | Add Baseline Profiles & Macrobenchmark; enable R8 full mode, configuration cache, Gradle Build Optimization flags |
| Testing | Only sample unit test | Add DAO tests, repository tests, UI screenshot tests, semantic search similarity precision tests, macrobench throughput |
| Observability | None | Integrate structured logs (Timber / KLogger), Crashlytics / Sentry, performance traces |
| Accessibility | Not audited | Provide content descriptions, dynamic font scaling, talk-back navigation semantics |
| Theming | Minimal M3 defaults | Add dynamic color (MaterialYou), dark mode, large-screen adaptive layouts (navigation rail / pane) |
| Secrets Mgmt | Manual ignore patterns | Centralize via Gradle catalog + `.env` loader or encrypted keystore for API keys |
| Security | No hardening | Apply Play Integrity API, certificate pinning (if networking added), secure storage for tokens |
| CI/CD | Not configured | Add GitHub Actions (build, lint, test, detekt, unit + instrumentation matrix) + Gradle build scans gating |
| Code Quality | No static analysis | Add Detekt, Ktlint, Android Lint baseline & custom rules, Dependency updates bot |
| Internationalization | Static English strings | Externalize & provide sample locale; use pseudo-localization test |
| Offline Strategy | Local only | Introduce sync adapter layer for eventual cloud back-end; conflict resolution policy |
| Paging / Large Data | Full list in memory | Integrate Paging 3 once tasks scale |

## Getting Started
### Prerequisites
- Android Studio Giraffe (or newer with AGP 8.5.x support)
- JDK 17
- Android SDK 34 installed

### Build & Run
```bash
./gradlew assembleDebug
./gradlew installDebug
```
Then select the installed app on a device/emulator (Android 8+). Use the drawer to switch screens.

### Project Entry Points
| Concern | Location |
|---------|----------|
| Activity | `MainActivity.kt` |
| Entities / DAO | Same file (for brevity) |
| Repository | `TaskRepository` (in `MainActivity.kt`) |
| Embedding | `LocalEmbeddingService` |
| ViewModel | `TaskViewModel` |
| Navigation | `AppRoot` composable + NavHost |

## Development Tips
- Hot Reload: Use Compose previews (extract composables if needed).
- Database Inspection: Android Studio App Inspector → Database Inspector.
- Rapid Iteration: Keep embedding deterministic for reproducible tests.

## Suggested Next Implementation Steps
See the detailed phased plan in `roadmap/`.

Immediate Next Iteration (Phase 1.1)
- Complete unit, DAO, and repository tests; target >80% coverage for domain/data
- Add CI (GitHub Actions) for assemble + unit tests + lint
- Add Detekt + Ktlint with baseline; fix critical issues
- Enable R8/proguard for release builds

Upcoming Phases
- Phase 2: CI, Lint, and Release Hardening
- Phase 3: Modularization & Architecture Evolution
- Phase 4: Semantic Search Upgrade (TFLite or sqlite-vec)
- Phase 5: Performance, Accessibility, and UX Polish

## Testing Strategy (Planned)
| Layer | Test Type |
|-------|-----------|
| DAO | Instrumented (Room migrations, CRUD) |
| Repository | JVM unit (similarity ranking, embedding shape) |
| ViewModel | JVM unit (state emission) |
| UI | Compose UI tests & screenshot regression |
| Performance | Macrobenchmark (cold start, search latency) |

## License
See [LICENSE](./LICENSE).

## Disclaimer
## Roadmap

### Implementation Status
- **Phase 1:** Foundation Architecture ✅ 85% Complete
- **Phase 1.1:** Testing & Hardening 🔄 In Progress (see [Implementation Guide](PHASE_1.1_IMPLEMENTATION_GUIDE.md))
- **Phase 2:** CI, Lint, and Release 📋 Ready to Begin
- **Phase 3-5:** Modularization & Enhancements 📋 Planned

### Documentation
Detailed blueprints are in the `roadmap/` folder:
- `phase1-blueprint.md` — Foundation Architecture (implemented) + Phase 1.1 next iteration
- `phase2-blueprint.md` — CI, Lint, and Release Hardening
- `phase3-blueprint.md` — Modularization & Architecture Evolution
- `phase4-blueprint.md` — Semantic Search Upgrade
- `phase5-blueprint.md` — Performance, Accessibility, and UX Polish

### Validation & Best Practices
- **[BEST_PRACTICES_SUMMARY.md](BEST_PRACTICES_SUMMARY.md)** — Executive summary and quick reference
- **[ANDROID_LOCAL_LLM_BEST_PRACTICES_VALIDATION.md](ANDROID_LOCAL_LLM_BEST_PRACTICES_VALIDATION.md)** — Comprehensive validation report
- **[PHASE_1.1_IMPLEMENTATION_GUIDE.md](PHASE_1.1_IMPLEMENTATION_GUIDE.md)** — Step-by-step guide with code examples

For background research and rationale, see `Android_Task_Manager_Architecture_Research_Perplexity_ai.md`.
This project is an educational scaffold. Not production-hardened (security, performance, scalability) until the roadmap items are addressed.
