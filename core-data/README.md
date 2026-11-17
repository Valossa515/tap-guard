# core-data

Biblioteca Android com o banco local (Room), as preferências (DataStore), os repositórios e o histórico de varreduras, decisões e alertas.

> **Estado (SPEC-004):** infraestrutura pronta, sem regra de negócio: banco Room versão 1 com as quatro tabelas da seção 9, DAOs só com inserir e ler, preferências com os padrões e o módulo do Hilt. Repositórios, retenção e consultas com regra entram na SPEC-018.

## Dependências

- **Módulos:** `core-domain`.
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), `com.google.devtools.ksp`, `com.google.dagger.hilt.android`, `androidx.room`, ktlint e detekt.
- **Bibliotecas:** Room 2.8.5 (`room-runtime`, `room-compiler` por KSP), DataStore Preferences 1.3.0, Hilt 2.60.1 (`hilt-android`, `hilt-compiler` por KSP). Sem kapt.
- **Testes:** JUnit 4, Robolectric 4.18, `androidx.test:core`, `room-testing` 2.8.5, `kotlinx-coroutines-test` 1.11.0, `hilt-android-testing` 2.60.1.
- **Namespace:** `io.github.valossa515.tapguard.core.data` · minSdk 26 · compileSdk 37.

## Estrutura

| Pacote | Conteúdo |
|--------|----------|
| `database` | `TapGuardDatabase` (versão 1, arquivo `tapguard.db`) |
| `database.entity` | `ScanRunEntity`, `AppSnapshotEntity`, `UserDecisionEntity`, `AlertEventEntity` |
| `database.dao` | `ScanRunDao`, `AppSnapshotDao`, `UserDecisionDao`, `AlertEventDao` (só inserir e ler) |
| `settings` | `SettingsDataSource` (DataStore `settings`) |
| `di` | `DataModule` (Hilt, `SingletonComponent`): banco e DataStore únicos, e cada DAO |

## Banco (especificação, seção 9)

Colunas com os nomes da especificação (camelCase). Datas em milissegundos; enums como texto.

| Tabela | Chave | Colunas (`?` = opcional) |
|--------|-------|--------------------------|
| `scan_run` | `id` autoincremento | `startedAt`, `finishedAt?`, `trigger` (`MANUAL`, `APP_OPEN`, `PERIODIC`), `totalApps`, `redCount`, `yellowCount` |
| `app_snapshot` | `scanId` + `packageName`; `scanId` → `scan_run.id` com exclusão em cascata | `label`, `versionCode`, `installer?`, `firstInstallTime`, `lastUpdateTime`, `signals` (S1 no bit 0 até S9 no bit 8), `score`, `level` (`GREEN`, `YELLOW`, `RED`) |
| `user_decision` | `packageName` | `decision` (`TRUSTED`), `decidedAt` |
| `alert_event` | `id` autoincremento | `type` (`RISKY_APP`, `DEFAULT_PAYMENT_CHANGED`), `packageName?`, `versionCode?`, `level?`, `createdAt`, `readAt?` |

### Schema

- Exportado pelo build em `schemas/io.github.valossa515.tapguard.core.data.database.TapGuardDatabase/1.json` e **commitado**. O build regera o arquivo a cada compilação.
- Ao mudar uma entidade, rode `./gradlew build` e commite o schema regerado junto. O passo "Schema do Room commitado" do CI falha se o schema estiver diferente do commitado.
- Até a primeira versão pública, a versão 1 pode mudar assim, sem `Migration`. Depois dela, o schema publicado fica congelado: mudança exige subir a versão, uma `Migration` e um teste de migração (ADR-014).
- Os schemas também entram nos assets do teste local (`sourceSets.test.assets.directories`), para o `MigrationTestHelper` sob Robolectric.

## Preferências (DataStore `settings`)

| Chave | Tipo | Padrão |
|-------|------|--------|
| `onboardingDone` | booleano | falso |
| `monitoringEnabled` | booleano | verdadeiro |
| `scanIntervalHours` | inteiro | 6 (a validação de 1, 6, 12 ou 24 fica para a SPEC-018) |
| `lastDefaultPaymentComponent` | texto | nulo quando nunca registrado; gravar nulo apaga |

## Regras

- Nenhum dado de cartão entra no banco nem nas preferências.
- Um único DataStore por arquivo: o `DataModule` fornece um só (`@Singleton`).
- `fallbackToDestructiveMigration()` é proibido em release.

## Testes

- DAOs em banco em memória (Robolectric), com chave duplicada e snapshot órfão recusados e a cascata.
- Migração inicial: `MigrationTestHelper` com arquivo e driver (`AndroidSQLiteDriver`) cria e valida a versão 1.
- `SettingsDataSourceTest` em JUnit 4 puro com DataStore real. **No Windows**, os dois testes que gravam mais de uma vez são ignorados (`Assume`), porque a JVM do Windows não consegue renomear o arquivo temporário do DataStore por cima do existente. Eles rodam no CI (Linux).
- Grafo do Hilt (`DataModuleTest`, `HiltTestApplication`): banco, DAOs e DataStore, com singletons.

## Comandos

```bash
./gradlew :core-data:build
./gradlew :core-data:testDebugUnitTest   # DAOs, migração, preferências e Hilt (Robolectric)
```
