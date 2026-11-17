# Test Plan: SPEC-004 Base de persistência e injeção

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-004-persistencia.contract.toon` e critérios de `.agent/specs/acceptance/spec-004-persistencia.acceptance.toon` · 2026-11-17
> Sem desvio da estratégia aprovada: 8 alvos, 25 cenários.

## Scope
DAOs das quatro tabelas em memória, chaves e cascata, migração inicial contra o schema commitado, preferências com os padrões, grafo do Hilt no `core-data` e o `MergedManifestTest` estendido. Tudo em `src/test`, sem aparelho, no `./gradlew build`.

## Padrões seguidos (pattern analysis)
- Existentes: `MergedManifestTest`, `TapGuardNavHostTest`, `RiskLevelIndicatorTest`, `CoreDataSmokeTest`. JUnit 4 nos módulos Android, `@RunWith(RobolectricTestRunner::class)` com `@Config(sdk = [36])`, `org.junit.Assert`, nomes em frase com crases, constantes em `private companion object`, sem classes-base nem fixtures compartilhadas.
- Room em memória conforme `skills/android/testing.md` › Room Tests (`Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries()`, `@Before`/`@After`), com `runTest` do `kotlinx-coroutines-test`.
- Cada classe monta o próprio banco e os próprios dados (sem builders compartilhados, regra do tester).

## Unit tests

### `SettingsDataSource` — `core-data/src/test/.../settings/SettingsDataSourceTest.kt` (JUnit 4, sem Robolectric)
DataStore real num arquivo temporário (`TemporaryFolder`), criado com `PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = …)` dentro de `runTest`.

| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | first-read-returns-the-four-defaults | `onboardingDone` false, `monitoringEnabled` true, `scanIntervalHours` 6, `lastDefaultPaymentComponent` null | AC-010 |
| 2 | written-values-are-read-back | depois de gravar `true`, `false`, `12` e um componente, a leitura devolve esses valores | AC-011 |
| 3 | writing-null-removes-last-default-payment-component | gravar um componente e depois `null` → leitura null | AC-011 |

Emenda 1.1 do contrato: os cenários 2 e 3 começam com `Assume.assumeFalse(<Windows>)`. No Windows, a segunda gravação falha ao renomear o `.tmp` do DataStore por cima do arquivo (JVM), e eles aparecem como ignorados. No Android e no Linux do CI eles rodam. O cenário 1 roda em todos os sistemas.

## Integration tests (Robolectric)

### `ScanRunDao` — `database/dao/ScanRunDaoTest.kt` (banco em memória)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | insert-generates-id-and-get-by-id-returns-the-row | `insert` devolve id > 0; `getById` devolve a linha com esse id | AC-001 |
| 2 | observe-all-emits-inserted-rows-newest-first | duas varreduras → `observeAll().first()` em ordem decrescente de `startedAt` | AC-001 |
| 3 | optional-finished-at-round-trips-as-null | `finishedAt = null` volta null | AC-001 |

### `AppSnapshotDao` — `database/dao/AppSnapshotDaoTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | insert-all-and-get-by-scan-id-returns-the-rows | snapshots de uma varredura voltam iguais (incluindo `installer` nulo) | AC-002 |
| 2 | duplicate-scan-id-and-package-name-is-rejected | segundo snapshot com a mesma chave → `SQLiteConstraintException` | AC-005 |
| 3 | orphan-scan-id-is-rejected | snapshot com `scanId` inexistente → `SQLiteConstraintException` | AC-006 |
| 4 | deleting-the-scan-run-cascades-to-its-snapshots | `DELETE FROM scan_run` por SQL direto → `getByScanId` vazio | AC-007 |

### `UserDecisionDao` — `database/dao/UserDecisionDaoTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | insert-and-get-by-package-name-returns-the-decision | decisão `TRUSTED` volta igual | AC-003 |

### `AlertEventDao` — `database/dao/AlertEventDaoTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | insert-with-all-fields-and-get-by-id-returns-the-row | alerta completo volta igual | AC-004 |
| 2 | optional-fields-round-trip-as-null | `packageName`, `versionCode`, `level` e `readAt` nulos voltam nulos | AC-004 |

### Migração — `database/TapGuardDatabaseMigrationTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | version-1-is-created-and-validated-from-the-exported-schema | `MigrationTestHelper(instrumentation, file, AndroidSQLiteDriver(), TapGuardDatabase::class)`: `createDatabase(1)` e `runMigrationsAndValidate(1, emptyList())` sem erro | AC-008, AC-009 |

### Grafo do Hilt — `di/DataModuleTest.kt` (`@HiltAndroidTest`, `HiltTestApplication`)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | injects-database-daos-and-datastore | banco, quatro DAOs e `DataStore<Preferences>` injetados | AC-012 |
| 2 | database-and-datastore-are-singletons | duas chamadas a `Provider.get()` devolvem a mesma instância do banco e do DataStore | AC-012 |

### Manifesto mesclado — `app/src/test/.../MergedManifestTest.kt` (estendido)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | requested-permissions-are-within-the-explicit-allowlist | sem mudança | AC-018 |
| 2 | application-class-is-tap-guard-application | `applicationInfo.className` = `io.github.valossa515.tapguard.TapGuardApplication` | AC-013 |
| 3 | exported-components-are-main-activity-and-profile-install-receiver | exportados entre Activities, serviços, receptores e provedores = `MainActivity` e `ProfileInstallReceiver` (substitui "main activity is the only exported activity") | AC-017 |
| 4 | backup-is-disabled | `FLAG_ALLOW_BACKUP` desligada | AC-015 |
| 5 | data-extraction-rules-exclude-every-domain-in-cloud-backup-and-device-transfer | os nove domínios excluídos em cada seção de `R.xml.data_extraction_rules` | AC-016 |
| 6 | full-backup-content-excludes-every-domain | os nove domínios excluídos em `R.xml.full_backup_content` | AC-016 |

## Edge cases
- Só os da estratégia: opcionais nulos, chave duplicada, chave estrangeira órfã e cascata.

## Test data & fixtures
- Valores locais em cada teste. Sem fixtures compartilhadas.

## Infrastructure
- Robolectric 4.18, `androidx.test:core`, `room-testing`, `kotlinx-coroutines-test` e `hilt-android-testing`, já no `core-data/build.gradle.kts` (tarefas 2 e 4). Nenhuma dependência nova.

## Execução
- [x] `./gradlew build` verde, sem aviso novo, em 2026-11-17 (Windows): `core-data` 19 testes (17 aprovados, 2 ignorados por Assume: as escritas do `SettingsDataSourceTest`); `app` com os 6 cenários do `MergedManifestTest` aprovados. As duas escritas rodam no CI (Linux).
