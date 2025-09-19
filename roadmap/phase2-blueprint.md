# Phase 2 Blueprint — CI, Lint, and Release Hardening (1 week)

Objective
- Establish basic CI/CD hygiene, static analysis, and release readiness.
- Deliver an enhanced MVP with quality gates and minimized APK.

Motivation
- Research indicates top repos enforce CI, code style, and R8/Proguard (see Android_Task_Manager_Architecture_Research_Perplexity_ai.md).

Scope (Do)
1) CI/CD
- Add GitHub Actions workflow: build, unit tests, lint.
- Cache Gradle and wrapper for speed.

2) Static Analysis
- Integrate Detekt and Ktlint.
- Add detekt.yml and ktlint baseline; fix critical issues.

3) Release Hardening
- Enable R8/Proguard for release with minimal keep rules.
- Verify release build; document signing (keystores ignored).

4) Tests Completion
- Fill TODOs in repository/use case/ViewModel tests.
- Add DAO tests for Room.

Acceptance Criteria
- CI green on push/PR for main branches.
- No Detekt/Ktlint errors.
- Release build succeeds; APK size reduced vs debug.

Artifacts/Changes
- .github/workflows/android.yml (CI)
- detekt.yml, ktlint baseline
- proguard-rules.pro updates; build.gradle.kts release config
- Updated README (CI badge, how to run checks)

Risks & Mitigation
- Lint noise → use baseline and gradually improve.
- CI flakiness → pin AGP/Gradle versions and cache directories.
