---
# .agent/SKILLS.md
# ─────────────────────────────────────────────────────────
# SDD — Skill-Driven Development entry point
# This file is read by the AI agent BEFORE every task.
# Commit this folder alongside your source code.
# ─────────────────────────────────────────────────────────
name: tap-guard
stack: android
sdd-version: "1.3.0"
---

# Project Agent Skills

This is **Tap Guard**, an **Android / Kotlin** app (Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager, six-module Gradle monorepo — all planned, no code yet) that detects installed apps with the profile of NFC card-relay scam apps. Before writing any code,
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
> Source: `docs/especificacao.md` (13/11/2026). **No code exists yet** — everything below is **(planned)**;
> items only the code can settle are marked `TODO: no code yet`.

### Context documents
- [project-reference.md](project-reference.md) — consolidated reference (start here)
- [context.md](context.md)
- [architecture.md](architecture.md)
- [runbook.md](runbook.md)
- [glossary.md](glossary.md)
- [conventions.md](conventions.md), [decisions.md](decisions.md), [backlog_rules.md](backlog_rules.md)
- Full product spec: [../docs/especificacao.md](../docs/especificacao.md)

### Modules (planned)
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
- **targetSdk / compileSdk**: the latest level required by the Play Store on the delivery date
- **Application id**: open decision (spec §13) — TODO: no code yet

### Persistence (planned)
- **Database**: Room — tables `scan_run`, `app_snapshot`, `user_decision`, `alert_event` (see project-reference.md §5)
- **Settings**: DataStore — `onboardingDone`, `monitoringEnabled`, `scanIntervalHours`, `lastDefaultPaymentComponent`
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

### How data and platform layers are tested (planned)
- `core-domain`: JUnit 5 on the JVM, MockK; minimum 80% coverage
- Collectors: Robolectric (JUnit 4) with a simulated `PackageManager`
- Room repositories: Robolectric with an **in-memory** database — no real on-device database in integration tests
- Screens: Compose UI tests for each screen state and navigation; Turbine for Flows
- End-to-end: install `decoy-app` via `adb install`, run the scan, check level and notification — emulator in CI and physical device
- Manual: real contactless card reading on a physical NFC device
- Automated privacy checks: fail if the merged manifest declares `INTERNET`; search for the full synthetic card number in DB, preferences and logs after a read

### Spec format (non-HTTP features)
- In feature specs without HTTP (`.spec.toon`), `outputs.success.status` and `outputs.errors[].status` hold the **exit code of the command** that exercises the feature (e.g. `0` for success, `1` for failure), not an HTTP status.
- The HTTP status check in `.agent/scripts/validate-spec.sh` ("invalid HTTP status") does not apply to these specs; every other validator check still applies.

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
