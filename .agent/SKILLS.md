---
# .agent/SKILLS.md
# ─────────────────────────────────────────────────────────
# SDD — Skill-Driven Development entry point
# This file is read by the AI agent BEFORE every task.
# Commit this folder alongside your source code.
# ─────────────────────────────────────────────────────────
name: tap-guard
stack: android
sdd-version: "1.4.0"
---

# Project Agent Skills

This is **Tap Guard**, an **Android / Kotlin** app (Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager — planned; the six-module Gradle monorepo exists since SPEC-001, with no features yet) that detects installed apps with the profile of NFC card-relay scam apps. Before writing any code,
read and follow all the skills listed below.

## Active Skills

### Stack Skills

| Skill | File | Scope |
|-------|------|-------|
| Architecture & patterns | [skills/android/SKILL.md](skills/android/SKILL.md) | All code |
| Testing | [skills/android/testing.md](skills/android/testing.md) | Test files |
| Database | [skills/shared/database/SKILL.md](skills/shared/database/SKILL.md) | Room (`scan_run`, `app_snapshot`, `user_decision`, `alert_event`) and DataStore in `core-data` (planned) |
| Security | [skills/shared/security/SKILL.md](skills/shared/security/SKILL.md) | Permissions (never `INTERNET`), card data in memory only, data at rest |
| Observability | [skills/shared/observability/SKILL.md](skills/shared/observability/SKILL.md) | Logging without personal or card data; no telemetry |
| Bootstrap | [skills/shared/bootstrap/SKILL.md](skills/shared/bootstrap/SKILL.md) | Project context setup and maintenance |
| Refactor | [skills/shared/refactor/SKILL.md](skills/shared/refactor/SKILL.md) | Convention-aligned refactoring |

> The shared `api-design` and `error-handling` skills describe HTTP services. They apply only if this
> app also ships or talks to a backend the team owns; otherwise follow the error handling section of
> the Android skill.

### Commands

Workflow commands are handled directly by the agent roles:

| Command | Agent | What it does |
|---------|-------|--------------|
| `@planning` | [agents/planner.md](agents/planner.md) | Plan a feature — produces plan.md + .spec.toon + .acceptance.toon |
| `@implementation` | [agents/builder.md](agents/builder.md) | Execute a plan — implements tasks following conventions |
| `@test-plan` | [agents/tester.md](agents/tester.md) | Design the testing strategy + test plan document |
| `@test` | [agents/tester.md](agents/tester.md) | Implement the tests from the test plan |
| `@review` | [agents/reviewer.md](agents/reviewer.md) | Structured code review with 🔴🟡🔵 severity classification |

### Agent Guardrails

| Skill | File | Scope |
|-------|------|-------|
| Anti-invention | [skills/shared/anti-invention/SKILL.md](skills/shared/anti-invention/SKILL.md) | All tasks — prevents hallucination |
| Pattern analysis | [skills/shared/pattern-analysis/SKILL.md](skills/shared/pattern-analysis/SKILL.md) | All generation — 3-example rule |
| Gap analysis | [skills/shared/gap-analysis/SKILL.md](skills/shared/gap-analysis/SKILL.md) | Planning — detects missing info |
| Spec validation | [skills/shared/spec-validation/SKILL.md](skills/shared/spec-validation/SKILL.md) | Specs — validates structure |
| Definition of Ready | [skills/shared/dor/SKILL.md](skills/shared/dor/SKILL.md) | Pre-implementation gate |
| Definition of Done | [skills/shared/dod/SKILL.md](skills/shared/dod/SKILL.md) | Post-implementation gate |

### Agent Roles

For pipeline-driven development, see [agents/README.md](agents/README.md):

| Role | File | When to use |
|------|------|-------------|
| Planner | [agents/planner.md](agents/planner.md) | Convert requirement → spec |
| Architect | [agents/architect.md](agents/architect.md) | Convert spec → contract |
| Builder | [agents/builder.md](agents/builder.md) | Generate production code |
| Tester | [agents/tester.md](agents/tester.md) | Generate tests |
| Reviewer | [agents/reviewer.md](agents/reviewer.md) | Validate + review |
| Conductor | [agents/conductor.md](agents/conductor.md) | Run full pipeline |

## Project Overrides

> Rules here take precedence over the base skills above.
> Source: `docs/especificacao.md` (13/11/2026). The Gradle build and the six empty modules exist since SPEC-001;
> everything else below is **(planned)**. Items only the code can settle are marked `TODO: no code yet`.

### Context documents
- [project-reference.md](project-reference.md) — consolidated reference (start here)
- [context.md](context.md)
- [architecture.md](architecture.md)
- [runbook.md](runbook.md)
- [glossary.md](glossary.md)
- [conventions.md](conventions.md), [decisions.md](decisions.md), [backlog_rules.md](backlog_rules.md)
- Full product spec: [../docs/especificacao.md](../docs/especificacao.md)

### Modules (created in SPEC-001, no features yet)
```
core-domain/     ← pure Kotlin: models (app, signal, score, level), score engine, collector interfaces — depends on nothing
core-detection/  ← Android library: signal collectors over Android APIs, ScanDevice use case — depends on core-domain
core-nfc/        ← Android library: EMV contactless reading + TLV parser — depends on core-domain
core-data/       ← Android library: Room, DataStore, repositories, history — depends on core-domain
app/             ← screens, navigation, scheduled monitoring, notifications, Hilt — depends on all core-*
decoy-app/       ← harmless test app with a scam-app profile — depends on nothing
```
- No score logic outside `core-domain`. No Android API inside `core-domain`.
- `core-domain` and `core-detection` must never depend on `app` (future bank SDK).

### SDK levels
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 36 (the level the Play Store currently requires); raise it when the Play Store requirement changes
- **compileSdk**: 37 (`platforms;android-37.0`, stable), separate from targetSdk: Compose 1.12 (BOM 2026.09.00), `navigation-compose` 2.10.2 and `lifecycle-*-compose` 2.11.0 require compileSdk ≥ 37. compileSdk only exposes APIs at build time; runtime behavior follows targetSdk (ADR-013)
- **Application id**: `io.github.valossa515.tapguard` (`app`) and `io.github.valossa515.tapguard.decoy` (`decoy-app`). Namespaces: `io.github.valossa515.tapguard.core.domain`, `.core.detection`, `.core.nfc`, `.core.data`; `app` and `decoy-app` use their application id (ADR-011)

### Build and toolchain
- **Versions** (only in `gradle/libs.versions.toml` — never a literal version in a `build.gradle.kts`): Gradle 9.7.0, AGP 9.3.3, Kotlin 2.4.21, ktlint-gradle 14.2.0 with ktlint 1.8.0, detekt 1.23.8, JUnit 5.14.4 (+ `junit-platform-launcher` 1.14.4) in `core-domain`, JUnit 4.13.2 in Android modules. In `app`: Compose BOM 2026.09.00 (Material 3), `activity-compose` 1.13.0, `navigation-compose` 2.10.2 (brings the `kotlinx-serialization-core` used by type-safe routes; do not add `kotlinx-serialization-json` unless code needs JSON), Compose and serialization compiler plugins 2.4.21; tests: Robolectric 4.18, `androidx.test:core` 1.7.0, `ui-test-junit4` (BOM).
- **JDK 21** is required: it is the toolchain of every module (`kotlin { jvmToolchain(...) }`), the CI JDK, and the Gradle daemon JDK. `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) makes the daemon run on an installed JDK 21 regardless of `JAVA_HOME`. It was written by hand because `./gradlew updateDaemonJvm` needs a JDK download repository (foojay plugin), which the project does not configure. Reason: detekt 1.23.8 runs inside the Gradle process and fails on JDK 25.
- **No `build-logic`, no convention plugins, no `subprojects {}` / `allprojects {}` configuration**: each module declares its own plugins, `android {}` block, SDKs and dependencies using catalog aliases.
- **Kover** (0.9.11) is applied **only in `core-domain`**, with a verification rule of at least 80% line coverage; `check` depends on `koverVerify`, so `./gradlew build` and CI enforce it. With no production code the rule passes (0/0); it starts biting at the first class with executable code (interfaces do not count, `data class` does). Do not apply Kover to Android modules (no coverage target there; open bug `Kotlin/kotlinx-kover#815` with AGP 9).
- **CI**: new GitHub Actions are pinned by commit SHA with the version in a comment on the same line (e.g. `uses: owner/action@<sha> # vX.Y.Z`). New CI steps go into the existing jobs "Regras do projeto" or "Build e testes", which are the required checks of the `main` ruleset.
- **Composable names**: `@Composable` functions use PascalCase. `.editorconfig` (`ktlint_function_naming_ignore_when_annotated_with = Composable`) and `app/detekt.yml` (`naming > FunctionNaming > ignoreAnnotated: ['Composable']`, applied with `buildUponDefaultConfig = true`) exempt only composables; plain functions are still checked. In detekt, write colors as named `const val` ARGB values and pass `dp` sizes as named arguments (`MagicNumber` ignores both by default) instead of suppressing the rule.
- **Isolated composable tests** (overrides `skills/android/testing.md`): use `Robolectric.buildActivity(ComponentActivity::class.java)` + `createEmptyComposeRule()` in `src/test`, **not** `createComposeRule()` with `ui-test-manifest` in `debugImplementation`, which would ship a test Activity in the published debug APK. Full-screen and navigation tests use `createAndroidComposeRule<MainActivity>()`. Use the `androidx.compose.ui.test.junit4.v2` rules (the others are deprecated in Compose 1.12).
- **DataStore tests on Windows**: tests that write to a real DataStore more than once start with `Assume.assumeFalse(<Windows>)`. On the Windows JVM, renaming DataStore's `.tmp` file over the existing file fails from the second write on (`Unable to rename …`). They are skipped on Windows and verified by CI (Linux), the same as on Android. Single-write and read-only tests run everywhere. Mention it in the PR description.
- **ktlint and detekt** are applied in every module; `./gradlew build` runs both plus Android lint and tests. ktlint also checks `*.gradle.kts` (break chained calls such as `libs.versions.minSdk.get().toInt()` before each `.`).

### Persistence (infrastructure since SPEC-004; repositories in SPEC-018)
- **Database**: Room 2.8.5 (`androidx.room`, not Room 3) in `core-data` — `TapGuardDatabase` version 1, file `tapguard.db`, tables `scan_run`, `app_snapshot`, `user_decision`, `alert_event` (see project-reference.md §5). DAOs only insert and read until SPEC-018.
- **Settings**: DataStore Preferences 1.3.0 — file `settings`, keys `onboardingDone` (default false), `monitoringEnabled` (true), `scanIntervalHours` (6), `lastDefaultPaymentComponent` (null when never recorded), read through `SettingsDataSource`
- **Annotation processing**: KSP 2.3.14 only (Room and Hilt). **Never kapt**.
- **Divergence from `skills/shared/database/SKILL.md`**: column names are the exact camelCase names of the spec (section 9), not `snake_case`; ids are `Long` autoincrement, not UUID; no generic `created_at`/`updated_at`.
- **Divergence from `skills/android/SKILL.md` › DataStore**: until SPEC-018, preferences are read through a thin `SettingsDataSource` in `core-data`, without a repository interface in `core-domain`.
- **Hilt graph test** lives in `core-data` (`DataModuleTest`, `HiltTestApplication`), the module that owns the Room and DataStore types; in `app`, KSP cannot resolve those `implementation` types.
- **Migration tests**: `MigrationTestHelper(instrumentation, file, AndroidSQLiteDriver(), TapGuardDatabase::class)` under Robolectric; the classic `(instrumentation, Class)` constructor fails there. Schemas reach the local test assets through `android.sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")` (the Room plugin only covers `androidTest`).
- **Schema rule (ADR-014)**: until the first public release, version 1 may change with the regenerated `1.json` committed alongside, without a `Migration`. After it, a published schema is frozen: a change bumps the version and ships a `Migration` and a migration test, and CI must block edits to an existing `N.json` on `main` (step planned for SPEC-073). Today CI step "Schema do Room commitado" fails when `./gradlew build` regenerates `core-data/schemas` with a diff (the build rewrites the schema on every compile, so the migration test alone cannot catch it).
- **Retention**: keep the 10 most recent scans; keep alerts for 90 days
- **Migrations / schema export**: Room schema exported to `core-data/schemas` and versioned. Every version change ships a `Migration` and a migration test. `fallbackToDestructiveMigration()` is forbidden in release.
- No card data ever enters the database or preferences

### Permissions
<!-- Anything not listed here must not be added. -->
- `android.permission.QUERY_ALL_PACKAGES` — list installed apps (restricted on Play Store: security-app declaration)
- `android.permission.NFC` — card test and default-payment lookup
- `android.permission.POST_NOTIFICATIONS` — monitoring alerts (Android 13+)
- `uses-feature android.hardware.nfc` with `required=false`
- **Never** `android.permission.INTERNET`

Brought in by AndroidX dependencies (accepted in SPEC-003; not declared by the project):
- `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (declared and used) — from `androidx.core`. Signature-level, held only by this app; `ContextCompat.registerReceiver` with `RECEIVER_NOT_EXPORTED` needs it below Android 13, so removing it would break that call.
- `androidx.profileinstaller.ProfileInstallReceiver` (exported, protected by `android.permission.DUMP`) — from `androidx.profileinstaller`. Only the shell and the system hold `DUMP`; it installs baseline profiles for faster cold start (RNF-03).
- `androidx.startup.InitializationProvider` (not exported) — from `androidx.startup`, used by AndroidX libraries to initialize at startup.
- `androidx.room.MultiInstanceInvalidationService` (not exported) — from Room (SPEC-004), used for multi-process invalidation; unused by the app but declared by the library.

Backup (SPEC-004): `android:allowBackup="false"`, `android:dataExtractionRules` (all nine domains excluded in `<cloud-backup>` and `<device-transfer>`, because on some devices `allowBackup` alone does not stop device-to-device transfer with targetSdk 31+) and `android:fullBackupContent` (all nine domains excluded, Android 11 and lower).

Enforced by `app/src/test/.../MergedManifestTest.kt`: it reads the merged manifest and fails on any permission outside `QUERY_ALL_PACKAGES`, `NFC`, `POST_NOTIFICATIONS` and `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; on an `Application` other than `TapGuardApplication`; on any exported component (activity, service, receiver or provider) other than `MainActivity` and `androidx.profileinstaller.ProfileInstallReceiver`; on backup being enabled; or on either backup rules file leaving a domain out. The wiring of the two rules files in the manifest (not public API) is checked by CI step "Regras de backup no manifesto". A new permission or exported component needs a recorded decision here and an update to that list in the same spec.

### How data and platform layers are tested (planned)
- `core-domain`: JUnit 5 on the JVM, MockK; minimum 80% line coverage, enforced by Kover (`:core-domain:koverVerify`, part of `./gradlew build`)
- Collectors: Robolectric (JUnit 4) with a simulated `PackageManager`
- Room repositories: Robolectric with an **in-memory** database — no real on-device database in integration tests
- Screens: Compose UI tests for each screen state and navigation; Turbine for Flows
- End-to-end: install `decoy-app` via `adb install`, run the scan, check level and notification — emulator in CI and physical device
- Manual: real contactless card reading on a physical NFC device
- Automated privacy checks: fail if the merged manifest declares `INTERNET`; search for the full synthetic card number in DB, preferences and logs after a read

### Custom rules
- No backend, login, telemetry or ads. All processing on-device.
- Card data: masked inside `core-nfc` (UI receives only the last 4 digits), memory only, discarded when leaving the screen, never logged or sent to crash reporting; card-test screen marked secure.
- Reader mode enabled only while the card-test screen is visible.
- Never uninstall or block apps automatically — only open the system screen.
- Background monitoring via WorkManager only; no permanent foreground service.
- Every screen with data has loading, empty, content and error states.
- Risk level is never conveyed by color alone: color + icon + text.
- Every user-visible string in Brazilian Portuguese in resource files; no technical terms (HCE, APDU) in the UI; use the exact texts fixed in the spec (see backlog_rules.md).
- Score weights live in a single configuration file.
- Tests never use real malware or real card data (synthetic TLV vectors only).
- One branch and one PR per spec, with the spec ID in the title; ktlint, detekt and the test suite must pass.
- The shared `api-design` and `error-handling` skills (HTTP / RFC 7807) do not apply: there is no backend.

## How to use

Point your agent at this file before each task:
> "Read `.agent/SKILLS.md` and follow all linked skills before writing any code."

Or configure your IDE plugin to auto-load context from `.agent/`.
