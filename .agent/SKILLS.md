---
# .agent/SKILLS.md
# ─────────────────────────────────────────────────────────
# SDD — Skill-Driven Development entry point
# This file is read by the AI agent BEFORE every task.
# Commit this folder alongside your source code.
# ─────────────────────────────────────────────────────────
name: tap-guard
stack: android
sdd-version: "1.8.0"
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
- **Versions** (only in `gradle/libs.versions.toml` — never a literal version in a `build.gradle.kts`): Gradle 9.7.0, AGP 9.3.3, Kotlin 2.4.21, ktlint-gradle 14.2.0 with ktlint 1.8.0, detekt 1.23.8, JUnit 5.14.4 (+ `junit-platform-launcher` 1.14.4) in `core-domain`, JUnit 4.13.2 in Android modules. In `app`: Compose BOM 2026.09.00 (Material 3), `activity-compose` 1.13.0, `navigation-compose` 2.10.2 (brings the `kotlinx-serialization-core` used by type-safe routes; do not add `kotlinx-serialization-json` unless code needs JSON), Compose and serialization compiler plugins 2.4.21; tests: Robolectric 4.18, `androidx.test:core` 1.7.0, `ui-test-junit4` (BOM). In `core-detection` (SPEC-011): `kotlinx-coroutines-core` 1.11.0 as `api` (the catalog version `coroutines` is shared with `kotlinx-coroutines-test`); tests: Robolectric, `androidx.test:core`, `kotlinx-coroutines-test`.
- **JDK 21** is required: it is the toolchain of every module (`kotlin { jvmToolchain(...) }`), the CI JDK, and the Gradle daemon JDK. `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) makes the daemon run on an installed JDK 21 regardless of `JAVA_HOME`. It was written by hand because `./gradlew updateDaemonJvm` needs a JDK download repository (foojay plugin), which the project does not configure. Reason: detekt 1.23.8 runs inside the Gradle process and fails on JDK 25.
- **No `build-logic`, no convention plugins, no `subprojects {}` / `allprojects {}` configuration**: each module declares its own plugins, `android {}` block, SDKs and dependencies using catalog aliases.
- **Kover** (0.9.11) is applied **only in `core-domain`**, with a verification rule of at least 80% line coverage; `check` depends on `koverVerify`, so `./gradlew build` and CI enforce it. Interfaces do not count, `data class` does. Since SPEC-010 the module has production code (domain models): line coverage 98.8%. The `testFixtures` source set is excluded (`excludedSourceSets.add("testFixtures")`, SPEC-012). Do not apply Kover to Android modules (no coverage target there; open bug `Kotlin/kotlinx-kover#815` with AGP 9).
- **CI**: new GitHub Actions are pinned by commit SHA with the version in a comment on the same line (e.g. `uses: owner/action@<sha> # vX.Y.Z`). New CI steps go into the existing jobs "Regras do projeto" or "Build e testes", which are the required checks of the `main` ruleset.
- **Composable names**: `@Composable` functions use PascalCase. `.editorconfig` (`ktlint_function_naming_ignore_when_annotated_with = Composable`) and `app/detekt.yml` (`naming > FunctionNaming > ignoreAnnotated: ['Composable']`, applied with `buildUponDefaultConfig = true`) exempt only composables; plain functions are still checked. In detekt, write colors as named `const val` ARGB values and pass `dp` sizes as named arguments (`MagicNumber` ignores both by default) instead of suppressing the rule.
- **Compose test rules**: use the rules from `androidx.compose.ui.test.junit4.v2` (`createEmptyComposeRule`, `createAndroidComposeRule`); the ones outside `v2` are deprecated in Compose 1.12. The rest of the Compose test setup follows `skills/android/testing.md`.
- **Tests skipped on Windows** (as `skills/android/testing.md` › DataStore prescribes): the PR description names the tests that only run in CI.
- **ktlint and detekt** are applied in every module; `./gradlew build` runs both plus Android lint and tests. ktlint also checks `*.gradle.kts` (break chained calls such as `libs.versions.minSdk.get().toInt()` before each `.`).

### Persistence (infrastructure since SPEC-004; repositories in SPEC-018)
- **Database**: Room 2.8.5 (`androidx.room`, not Room 3) in `core-data` — `TapGuardDatabase` version 1, file `tapguard.db`, tables `scan_run`, `app_snapshot`, `user_decision`, `alert_event` (see project-reference.md §5). DAOs only insert and read until SPEC-018.
- **Settings**: DataStore Preferences 1.3.0 — file `settings`, keys `onboardingDone` (default false), `monitoringEnabled` (true), `scanIntervalHours` (6), `lastDefaultPaymentComponent` (null when never recorded), read through `SettingsDataSource`
- **Annotation processing**: KSP 2.3.14 only (Room and Hilt). **Never kapt**.
- **Divergence from `skills/shared/database/SKILL.md`**: column names are the exact camelCase names of the spec (section 9), not `snake_case`; ids are `Long` autoincrement, not UUID; no generic `created_at`/`updated_at`.
- **Divergence from `skills/android/SKILL.md` › DataStore**: until SPEC-018, preferences are read through a thin `SettingsDataSource` in `core-data`, without a repository interface in `core-domain`.
- **Schema rule (ADR-014)**: until the first public release, version 1 may change with the regenerated `1.json` committed alongside, without a `Migration`. After it, a published schema is frozen: a change bumps the version and ships a `Migration` and a migration test, and CI must block edits to an existing `N.json` on `main` (step planned for SPEC-073). Today CI step "Schema do Room commitado" fails when `./gradlew build` regenerates `core-data/schemas` with a diff (the build rewrites the schema on every compile, so the migration test alone cannot catch it).
- **Retention**: keep the 10 most recent scans; keep alerts for 90 days
- **Migrations / schema export**: Room schema exported to `core-data/schemas` and versioned. Every version change ships a `Migration` and a migration test. `fallbackToDestructiveMigration()` is forbidden in release.
- No card data ever enters the database or preferences

### Domain models (SPEC-010)
- **Immutable models:** `data class` with `val` only, enforced by detekt `DataClassShouldBeImmutable` in `core-domain/detekt.yml` (applied with `buildUponDefaultConfig = true`, same shape as `app/detekt.yml`). No arrays in models.
- **Collections in models:** private constructor, private `copy()` (`@ConsistentCopyVisibility`) and a `companion operator fun invoke` with the same name and parameters that copies each collection with `unmodifiableCopy()` (`Collections.unmodifiableSet/List` over a copy). The exposed collection rejects mutation even through a cast to `MutableSet`/`MutableList`, and the tests check it. Never `Set.copyOf`/`List.copyOf` (API 30 on Android) nor `toSet()`/`toList()` as a defensive copy.
- **`@Suppress` — two closed exceptions:**
  - `@Suppress("LongParameterList")` **only** on a factory that mirrors exactly the constructor of a `data class` (detekt already exempts the constructor through `ignoreDataClasses`, but not the factory). Today: `InstalledApp` (ADR-015).
    - **Extended by ADR-019 (SPEC-014) to test fixtures that pass through to such a data class factory, with default values**, in `src/testFixtures`. Today: `anInstalledApp` (8 parameters; detekt's `LongParameterList` already fires at 6), with a KDoc saying why. It does **not** cover any other test function: a test helper with too many parameters is split or simplified.
  - `@Suppress("DEPRECATION")` as in `skills/android/SKILL.md` › Reading installed packages, and only in `core-detection`'s `PackageManagerVersionCompat.kt` and in the test fixture that sets `versionCode` for API 26 (ADR-016).
  - Anywhere else, no `@Suppress`: fix the code, and test fixtures expose only the parameters the tests override.
- **Rule 1 (system apps ignored)** is enforced by `AppInventory`, which never returns system apps (SPEC-011; definition in `skills/android/SKILL.md` › Reading installed packages). `InstalledApp` has no system flag. The "100% of the adjustment rules" criterion of SPEC-017 covers rules 2 to 4.
- **Signal bitmask** conversion lives in `core-domain` (`toBitMask()`, `signalsFromBitMask()`); the bit is `Signal.bit`, never the `ordinal`. An invalid mask (negative or with a bit above 8) throws `IllegalArgumentException`. `core-data` only calls these functions.
- **`NfcTagHandle`** is an opaque marker with no members: `core-domain` never sees the Android `Tag` nor card commands; `core-nfc` wraps and unwraps the `Tag` (SPEC-041).
- **Full card number barred by type:** `CardReadResult.Success` holds `LastFourDigits` (exactly four ASCII digits), `applicationLabel` and `expiry` only. `toString()` of `Success` and `LastFourDigits` shows no value, and the validation message never repeats the input. `CardReadResult` is `Success`, `UnsupportedCard` or `ReadInterrupted`; missing or disabled NFC is not a result of `read`.
- **`ScoringContext`** holds only `trustedPackages`; weights are configuration of the `RiskScorer` implementation (SPEC-017). This replaces the `rules` parameter shown in the skill example, and `RiskScorer` is an interface (spec §7), not the class of the skill example.

### Detection (SPEC-011, SPEC-013, SPEC-014)
> Reading, dispatcher and version-branch rules: `skills/android/SKILL.md` › Reading installed packages and Dispatchers in adapters. Only what is specific to this project is listed here.
- **`PackageManagerAppInventory(context, ioDispatcher)`** in `core.detection.inventory`. No Hilt annotations: binding in SPEC-018.
- **Three steps (SPEC-013):** the skill's two-step read plus one `queryIntentServices` per scan, between the light list and the details: action `HostApduService.SERVICE_INTERFACE`, flag `MATCH_DISABLED_COMPONENTS`, kept when `ServiceInfo.permission == BIND_NFC_SERVICE`, grouped by package into `InstalledApp.cardEmulationServices`. Off-host and NFC-F are left out; exported, meta-data and the package's `NFC` permission are not required (ADR-018).
- **Fourth step (SPEC-014):** one read of `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` per scan, after the HCE query: split on `:`, skip empty entries, `ComponentName.unflattenFromString` (full and short `pkg/.Cls` forms), skip nulls, group class names by package into `InstalledApp.enabledAccessibilityServices`. Packages outside the light list are ignored; not cross-checked with `services`. Null or empty key = no service enabled (also what the framework returns when the settings provider fails). No permission (API 26 to 36), no package-visibility filter, no `try` (ADR-019).
- **Public API of `core-detection`:** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores` (SPEC-012), `NfcPermissionCollector`, `CardEmulationServiceCollector` (SPEC-013), `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector` and `EnabledAccessibilityServiceCollector` (SPEC-014), `KnownWallets`, `KnownBankApp` and `KnownBanks` (SPEC-015 BATCH-01), `BankLookalikeCollector` (SPEC-015 BATCH-03): thirteen classes. Everything else is `internal` or `private`.
- **Version branches:** `PackageManagerVersionCompat.kt`, tested with `@Config(sdk = [26, 28, 30, 33, 36])` (`skills/android/testing.md` › Adapter Tests — Robolectric).
- **Lint:** besides `tools:ignore="QueryAllPackagesPermission"` (skill › Permissions and Manifest), `@SuppressLint("QueryPermissionsNeeded")` on `installedPackagesCompat` and `queryIntentServicesCompat`, with a comment (ADR-016, ADR-018).
- **`api`:** `core-detection` exposes `core-domain` and `kotlinx-coroutines-core` as `api` (skill › `api` or `implementation` in library modules).
- **Robolectric hooks:** cancellation and "uninstalled during the read" are tested with a label `CharSequence` that acts on its second read (Robolectric parcels the `PackageInfo` it returns). If Robolectric stops parceling, those tests fail instead of passing.
- **Services in `queryIntentServices` (Robolectric 4.18, SPEC-013):** a service put in the `PackageInfo` given to `installPackage` shows in `getPackageInfo` but not in intent resolution (`addIntentFilterForService` throws "doesn't exist"). Install the package without it, then `addOrUpdateService(serviceInfo)` and `addIntentFilterForService(component, IntentFilter(action))` (helper `installService` in `PackageManagerAppInventoryTest`). Every HCE test expects at least one service found by the query, so a change in Robolectric fails them instead of passing.
- **`Settings.Secure` in Robolectric 4.18 (SPEC-014):** `Settings.Secure.putString(contentResolver, key, value)` is enough on the five API levels, with no reflection, and the key is back to `null` at the start of each test (helper `enableAccessibilityServices`). Robolectric does not enforce `@Readable`: readability comes from the AOSP sources (ADR-019). Tests write the key as a literal (`enabled_accessibility_services`), not the production constant.
- Open (ADR-016): `explicitApi()` in `core-detection` when the SDK is published.

### Signal collectors (SPEC-012, SPEC-013, SPEC-014, SPEC-015)
- **Collectors live in `core-detection`**, package `core.detection.signal`, even when they use no Android API (ADR-017). They only return signals; score logic stays in `core-domain`.
- **S1:** `InstallSourceCollector(trustedInstallers)` returns `UNTRUSTED_INSTALL_SOURCE` when `installerPackageName` is null or not in the set (exact, case-sensitive match). The parameter is required, with no default value; the set is copied. It never calls the `PackageManager`.
- **Trusted stores:** `TrustedStores.packageNames`, 11 packages, one comment per entry with the store name and the source. System package installers, `com.android.shell`, F-Droid, Aurora Store and phone-clone apps are not stores (ADR-017). An SDK user builds its own set from it (`+` or `-`).
- **Support lists (SPEC-015, ADR-020):** same shape as `TrustedStores` (one public `object` per type in `core.detection.signal`, unmodifiable, one comment per entry with the name and the source (in `KnownBanks`, one per institution, with each app's title), extended or reduced by the SDK user with `+` or `-`).
  - `KnownWallets.packageNames`: 6 packages that can be the default payment app without S7 (Google Wallet, Samsung Wallet and the bank apps with in-app NFC Pix: Nubank, Itaú, BB, Inter). Consumer: the S7 collector (SPEC-016).
  - `KnownBanks.apps`: 29 `KnownBankApp(displayName, packageName)` entries (26 packages) from the 14 institutions with the most clients in the Banco Central ranking (Dec 2025). One entry per name and package: a package may have aliases ("BB" and "Banco do Brasil"), and an institution may have several apps.
  - `KnownBankApp` is a public `data class` with no collection; a blank name or package throws `IllegalArgumentException` with a fixed message. `core-domain` does not know the type.
  - Tests compare each list with a hand-written one (`ExpectedKnownWallets`, `ExpectedKnownBankApps`).
- **S9 name matching (SPEC-015 BATCH-02, ADR-020):** `BankNameMatcher.kt`, all `internal`, no `android.*` (`java.text.Normalizer` is from the JDK): `BankNameMatcher(displayNames).matches(label)`, `bankNameSkeleton` (NFKC first, so full-width `：` and `｜` also cut; then cut at the first separator, `lowercase()`, NFD without `CharCategory.NON_SPACING_MARK`, look-alike letters, only `a-z0-9`; no regex). The look-alike map is a partial, hand-written list: a look-alike outside it is deleted, so a short name (threshold 0) escapes; generating it from the UTS #39 `confusables.txt` is issue #69. A mapped letter must use the skeleton convention (`ʟ` maps to `i`, as `l` does) and `editDistance` (OSA). Prefixes and suffixes go through `bankNameSkeleton` when the file loads (`BANK_NAME_PREFIXES`, `BANK_NAME_SUFFIXES`, `internal` for the test that checks each one is removed); they are declared after the separators and the look-alike map, because file properties initialize in text order. Thresholds 5 and 9 are `const val` (detekt `MagicNumber`). Tests write invisible or look-alike characters as `\uXXXX` escapes; the parameterized cases read hand-written names, and only the cross-institution test reads `KnownBanks.apps`.
- **Unmodifiable public collections outside `core-domain`:** `Collections.unmodifiableSet(LinkedHashSet(...))` (or `linkedSetOf`), the same idiom as `unmodifiableCopy()`, which stays `internal`.
- **Tests of a collector with no Android API:** JUnit 4 on the JVM, no Robolectric, no `@Config(sdk)`. A parameterized test (`Parameterized`, one class per parameter group) reads a list written by hand in the test, never the production list, so removing an entry fails its case.
- Known limitations of S1 (ADR-017): installer spoofed with shell or root; borrowed package name of a store missing from the device (issue #57).
- **S2:** `NfcPermissionCollector()` returns `NFC_PERMISSION` only for `android.permission.NFC` in `requestedPermissions` (exact match). `NFC_TRANSACTION_EVENT`, `NFC_PREFERRED_PAYMENT_INFO` and `BIND_NFC_SERVICE` do not count (ADR-018).
- **S3:** `CardEmulationServiceCollector()` returns `CARD_EMULATION_SERVICE` when `InstalledApp.cardEmulationServices` is not empty; it does not depend on S2 (ADR-018).
- **S4:** `EnabledAccessibilityServiceCollector()` returns `ACCESSIBILITY_SERVICE_ENABLED` when `InstalledApp.enabledAccessibilityServices` is not empty; a service only declared in `services` does not count (ADR-019).
- **S5:** `OverlayPermissionCollector()` returns `OVERLAY_PERMISSION` for `android.permission.SYSTEM_ALERT_WINDOW` in `requestedPermissions` (exact match). The declared permission counts, not the grant; `AppOpsManager` is not read (ADR-019).
- **S6:** `RecentInstallOrUpdateCollector(clock: Clock)` returns `RECENTLY_INSTALLED_OR_UPDATED` when `maxOf(firstInstallTime, lastUpdateTime)` is not before `clock.instant()` minus 7 days (`Duration.ofDays(7)`, no time zone). Inclusive bound; a time in the future counts (ADR-019).
- **S8:** `SmsPermissionCollector()` returns `SMS_PERMISSION` for `android.permission.READ_SMS` or `android.permission.RECEIVE_SMS` (exact match). `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`, `READ_CELL_BROADCASTS` and `BROADCAST_SMS` do not count (ADR-019).
- **S9:** `BankLookalikeCollector(knownBankApps)` returns `BANK_LOOKALIKE` when `BankNameMatcher` matches the label against a `displayName` and the package is not the package of any entry (exact, case-sensitive; any entry, not only the matching one, so an official app whose name resembles another app of the same bank gets no S9). The parameter is required, with no default value; the set is copied; an empty set never yields S9. Default list: `KnownBanks.apps`. Borrowed package of a bank missing from the device: no S9 (ADR-017, ADR-020, issue #65). Tests use a small hand-written list and a fictitious bank.
- **Clock in collectors (SPEC-014):** `java.time.Clock` as a required constructor parameter, no default value; never `Instant.now()` or `System.currentTimeMillis()` in the collector. Tests use `Clock.fixed(now, ZoneOffset.UTC)`. The `Clock` binding (`Clock.systemUTC()`) comes with the Hilt wiring in SPEC-018.
- **One collector per signal**, each returning its own signal or nothing. A collector with no parameters is still a `class`, the same shape as `InstallSourceCollector`. Collectors import nothing from `android.*`: Android constants they compare against are written as literals.

### Permissions
<!-- Anything not listed here must not be added. -->
- `android.permission.QUERY_ALL_PACKAGES` — list installed apps (restricted on Play Store: security-app declaration). Declared **only in the `app` manifest** since SPEC-011, never in `core-detection`, which will become an SDK (ADR-016; rule in `skills/android/SKILL.md` › Permissions and Manifest)
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

Enforced by `app/src/test/.../MergedManifestTest.kt`: it reads the merged manifest and fails on any permission outside `QUERY_ALL_PACKAGES`, `NFC`, `POST_NOTIFICATIONS` and `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; on an `Application` other than `TapGuardApplication`; on any exported component (activity, service, receiver or provider) other than `MainActivity` and `androidx.profileinstaller.ProfileInstallReceiver`; on backup being enabled; or on either backup rules file leaving a domain out. The wiring of the two rules files in the manifest (not public API) is checked by CI step "Regras de backup no manifesto". Since SPEC-011 it also fails when `QUERY_ALL_PACKAGES` is not requested. `core-detection/src/test/.../LibraryManifestTest.kt` reads the library's merged manifest (`isIncludeAndroidResources`) and fails on any permission other than `REORDER_TASKS` (brought by `androidx.test:core`, test-only). A new permission or exported component needs a recorded decision here and an update to that list in the same spec.

### How data and platform layers are tested (planned)
- `core-domain`: JUnit 5 on the JVM, MockK; minimum 80% line coverage, enforced by Kover (`:core-domain:koverVerify`, part of `./gradlew build`)
- Collectors: Robolectric (JUnit 4) with a simulated `PackageManager` when they call it; plain JUnit 4 when they only read `InstalledApp` (SPEC-012)
- **Domain fixtures (SPEC-012):** `anInstalledApp` and `aRiskResult` live in `core-domain/src/testFixtures` (`java-test-fixtures` plugin); other modules use `testImplementation(testFixtures(project(":core-domain")))`. The `detekt` task does not scan `src/testFixtures` by default, so `core-domain/build.gradle.kts` adds it with `source.from(files("src/testFixtures/kotlin"))`; `MagicNumber` applies there
- **Injected dispatcher (SPEC-011):** as in `skills/android/testing.md` › Code that must run on an injected dispatcher; the project's example is `CountingDispatcher` in `PackageManagerAppInventoryTest`
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
- One branch and one PR per spec, with the spec ID in the title. When the spec has batches (above 500 lines of code and tests), one PR per batch, with the spec ID and the `BATCH-NN` in the title; only the last batch's PR carries `Closes #N`. ktlint, detekt and the test suite must pass.
- The shared `api-design` and `error-handling` skills (HTTP / RFC 7807) do not apply: there is no backend.

## How to use

Point your agent at this file before each task:
> "Read `.agent/SKILLS.md` and follow all linked skills before writing any code."

Or configure your IDE plugin to auto-load context from `.agent/`.
