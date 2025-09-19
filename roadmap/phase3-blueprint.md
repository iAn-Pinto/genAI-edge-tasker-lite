# Phase 3 Blueprint — Modularization & Architecture Evolution (1–2 weeks)

Objective
- Split single app module into foundational modules to speed builds and clarify boundaries.

Target Module Map
- core-model: pure Kotlin domain models
- core-database: Room entities/DAOs/DB + migration tests
- core-embedding: LocalEmbeddingService and similarity utils
- data-repository: TaskRepositoryImpl and mappers
- feature-task: task creation/list/detail UI + ViewModel
- feature-search: search UI + ViewModel
- app: shell, DI wiring, navigation graph

Steps
1) Introduce core-model and move `domain/model/Task.kt`
2) Introduce core-database and move entities/dao/db; export Room schema (schemas/)
3) Introduce core-embedding module and move LocalEmbeddingService
4) Create data-repository module with TaskRepositoryImpl and mappers
5) Create feature-task and feature-search; wire navigation via app module
6) Wire Hilt across modules; use entry points where needed

Acceptance Criteria
- App builds and runs; features parity maintained
- Unit tests pass; DAO tests moved to core-database
- Build time improves locally (qualitative)

Artifacts
- settings.gradle.kts includes new modules
- Each module has its own build.gradle.kts
- Updated package imports and DI modules

Risks & Mitigation
- Cyclic deps → enforce clear inbound/outbound module dependencies
- DI complexity → centralize bindings in app and data-repository
