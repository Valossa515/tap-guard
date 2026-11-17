# Plan: SPEC-004 Base de persistência e injeção

> Issue #5 · Etapa 0 (Fundação) · Depende de SPEC-001 (#2, fechada) · Especificação seção 9 · RNF-01 (nada sai do aparelho), RNF-02
> Spec: `.agent/specs/features/spec-004-persistencia.spec.toon` (FT-004, `interface: library`) · Aceite: `.agent/specs/acceptance/spec-004-persistencia.acceptance.toon`
> Branch: `spec-004-persistencia` · Planejado em 2026-11-17

## Summary
Montar a infraestrutura de dados e de injeção, sem regra de negócio. Três partes:
- **Hilt** configurado no `app` (`TapGuardApplication`) e no `core-data`;
- **banco Room** com as quatro tabelas da seção 9 (`scan_run`, `app_snapshot`, `user_decision`, `alert_event`), versão 1, schema exportado em `core-data/schemas` e commitado;
- **DataStore** com as quatro preferências e seus padrões.

O app também passa a **não sair do aparelho por backup**: backup desligado e regras de extração que excluem tudo, inclusive na transferência direta entre aparelhos. Repositórios, retenção e consultas com regra ficam para a SPEC-018.

## Scope
- **Módulos afetados:** `core-data` (entidades, DAOs, banco, DataStore, módulo do Hilt, testes); `app` (`TapGuardApplication`, manifesto, regras de backup, `MergedManifestTest`); catálogo e build raiz.
- **Novos arquivos (esperados):** em `core-data`, as quatro entidades, os quatro DAOs, o banco, a fonte das preferências, o módulo do Hilt, `schemas/<banco>/1.json` (gerado pelo build e commitado) e os testes. Em `app`, `TapGuardApplication.kt`, `res/xml/data_extraction_rules.xml` e `res/xml/full_backup_content.xml`.
- **Arquivos modificados:**
  - CI: `.github/workflows/ci.yml` (job "Regras do projeto");
  - build: `gradle/libs.versions.toml`, `build.gradle.kts` (raiz), `core-data/build.gradle.kts`, `app/build.gradle.kts`;
  - app: `app/src/main/AndroidManifest.xml`, `app/src/test/.../MergedManifestTest.kt`;
  - documentos: `core-data/README.md`, `app/README.md`, `CONTRIBUTING.md`, `.agent/runbook.md`, `.agent/project-reference.md` (§1, §3, §5, §8), `.agent/conventions.md`, `.agent/context.md`, `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-014).
- **Fora do escopo:**
  - repositórios e interfaces de repositório no `core-domain`, mapeamento entidade ↔ modelo, retenção, controle de duplicidade de alertas e validação de `scanIntervalHours` (SPEC-018);
  - modelos do domínio, como o `RiskLevel` (SPEC-010);
  - WorkManager (vigia), `@AndroidEntryPoint` em telas e ViewModels.

## Decisões já tomadas (usuário, 2026-11-17)

| Item | Decisão |
|------|---------|
| Versões | KSP 2.3.14, Hilt 2.60.1, Room 2.8.5 (Room 2, `androidx.room`), DataStore Preferences 1.3.0; testes: `hilt-android-testing` 2.60.1, `room-testing` 2.8.5, `kotlinx-coroutines-test` 1.11.0. Conferidas em 2026-11-17 no Maven Central, no Google Maven e nas releases do GitHub; acima dos mínimos da skill para AGP 9 (KSP 2.3.6, Hilt 2.59.2). **Room 3 (`androidx.room3`) fica fora** |
| Processamento de anotações | **só KSP**, sem kapt (incompatível com o Kotlin embutido do AGP 9) |
| Plugins | `com.google.devtools.ksp` e `com.google.dagger.hilt.android` no `app` e no `core-data`; `androidx.room` (para o `schemaDirectory`) no `core-data`. Todos com `apply false` na raiz e versão no catálogo |
| Schema | `room { schemaDirectory("$projectDir/schemas") }` no `core-data`; `core-data/schemas/<classe do banco>/1.json` commitado |
| Migração inicial | versão 1 verificada pelo `MigrationTestHelper` no construtor com **arquivo e driver** (`AndroidSQLiteDriver`), sob Robolectric. Schemas nos assets do teste local pela forma não depreciada do AGP 9: `sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")` |
| Colunas | nomes exatos da seção 9, em camelCase, como em `conventions.md` (Audit fields). Diverge da skill de banco (`snake_case`, `id` UUID, `created_at`/`updated_at`), e a divergência fica registrada no `SKILLS.md` |
| Tipos | `id` como `Long` autoincremento; datas em milissegundos (`Long`); enums como texto (`trigger`: `MANUAL`, `APP_OPEN`, `PERIODIC`; `decision`: `TRUSTED`; `type`: `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED`); `level` como texto `GREEN`, `YELLOW` ou `RED`; `signals` e `score` como `Int`; `versionCode` como `Long`. Opcionais: `scan_run.finishedAt`, `app_snapshot.installer`, `alert_event.packageName`, `alert_event.versionCode`, `alert_event.level`, `alert_event.readAt` |
| Chaves e relações | `scan_run.id` e `alert_event.id` autoincremento; `app_snapshot` com chave (`scanId`, `packageName`) e chave estrangeira `scanId` → `scan_run.id` **com exclusão em cascata**; `user_decision` com chave `packageName` |
| DAOs | **só inserir e ler**. Consultas de retenção, de duplicidade e de comparação entre varreduras ficam para a SPEC-018 |
| Preferências | `DataStore<Preferences>` singleton pelo Hilt e uma fonte de dados fina com as quatro chaves, leitura por `Flow` com padrão e escrita. Padrões: `onboardingDone` **falso**, `monitoringEnabled` verdadeiro, `scanIntervalHours` 6, `lastDefaultPaymentComponent` **nulo** quando nunca registrado. Sem validação de 1/6/12/24 (SPEC-018) |
| Backup | `android:allowBackup="false"`. A documentação de Auto Backup confirma que, com targetSdk 31+, em aparelhos de alguns fabricantes isso **não desliga a transferência direta entre aparelhos**. Por isso entram `android:dataExtractionRules` (exclui os nove domínios em `<cloud-backup>` e em `<device-transfer>`) e `android:fullBackupContent` (exclui os nove domínios no Android 11 ou anterior, exigido pelo lint com minSdk 26) |
| Manifesto | `android:name=".TapGuardApplication"` aprovado. `androidx.room.MultiInstanceInvalidationService` (não exportado, do Room) aceito e registrado no `SKILLS.md` como item do AndroidX |
| Nomes de arquivo (2026-11-17) | banco **`tapguard.db`**; DataStore **`settings`** (arquivo `settings.preferences_pb`) |
| Ligação das regras de backup (2026-11-17) | novo passo no job "Regras do projeto" do `ci.yml`: falha se o `app/src/main/AndroidManifest.xml` não tiver `android:allowBackup="false"`, `android:dataExtractionRules` e `android:fullBackupContent`. Cobre o que o `MergedManifestTest` não consegue ler. Commit próprio na tarefa 5 |
| Divergências com as skills (2026-11-17) | aprovadas para o `SKILLS.md`: colunas em camelCase (skill de banco); preferências sem interface no `core-domain` até a SPEC-018 (skill Android › DataStore); teste do grafo do Hilt no `core-data` |
| Testes de escrita do DataStore no Windows (2026-11-17) | os dois cenários do `SettingsDataSourceTest` que gravam mais de uma vez usam `Assume.assumeFalse(<Windows>)`: no Windows, renomear o `.tmp` do DataStore por cima do arquivo existente falha na JVM (`Unable to rename …`). No Android e no Linux do CI eles rodam. O teste de padrões roda em todos os sistemas. Regra no `SKILLS.md`. A descrição do PR precisa dizer que esses dois testes só rodam no CI |
| `MergedManifestTest` | passa a conferir **todos os componentes exportados** (Activities, serviços, receptores e provedores) contra uma lista explícita, que hoje é `MainActivity` e `ProfileInstallReceiver`. Também confere que o backup está desligado e que as duas regras de backup excluem todos os domínios. A lista de permissões continua igual |

### Experimento (projeto descartável `%TEMP%\tap-guard-spec004-probe-20261117`)
Cópia do repositório com as versões acima, uma entidade, um DAO, o banco, o módulo do Hilt e o DataStore.
1. **Compilação e grafo:** compila com KSP no `app` e no `core-data`. O schema foi exportado em `core-data/schemas/.../1.json`. O teste de DAO em memória (Robolectric) passa, e o teste que injeta banco, DAO e `DataStore<Preferences>` pelo grafo do Hilt (`hilt-android-testing`, `HiltTestApplication`) passa.
2. **Construtor clássico do `MigrationTestHelper`:** falha no Robolectric (`This driver is configured to open a database named 'x.db' but '<caminho>/x.db' was requested`). **O construtor com arquivo e driver passa**: cria a versão 1 a partir do schema e valida.
3. **Schemas nos testes locais:** o plugin do Room só os põe nos assets do `androidTest`. Para os testes locais, `assets.directories.add(...)` funciona; o `assets.srcDir(...)` está depreciado no AGP 9.
4. **Teste do grafo do Hilt:** fica no `core-data`. No `app`, o KSP não resolve `DataStore` nem o supertipo `RoomDatabase`, que são `implementation` do `core-data`.
5. **Manifesto mesclado:**
   - nenhuma permissão nova;
   - novo componente `androidx.room.MultiInstanceInvalidationService`, não exportado;
   - o `PackageManager` do Robolectric enxerga os quatro componentes, e os exportados são exatamente `MainActivity` e `ProfileInstallReceiver`;
   - com o backup desligado e as duas regras, o lint do `app` e do `core-data` não aponta problemas.
6. **ktlint:** não verificou os arquivos gerados pelo KSP. `./gradlew build` completo passa, com 33 testes no `app` e no `core-data`.

## Approach
- Seguir `skills/android/SKILL.md` (Persistence — Room, DataStore, Dependency Injection — Hilt, Built-in Kotlin) e `skills/android/testing.md` (Room Tests). Nomes: `{Thing}Entity`, `{Thing}Dao`, `{Scope}Module`.
- Tudo de dados no `core-data`. O módulo do Hilt instalado em `SingletonComponent` fornece o banco (singleton), cada DAO e o `DataStore<Preferences>` (singleton por arquivo, como o DataStore exige).
- O `app` só ganha a `TapGuardApplication` com `@HiltAndroidApp`. Nenhuma tela é injetada ainda.
- **Divergência com a skill (DataStore):** a skill põe as preferências atrás de uma interface de repositório no `core-domain`. Nesta spec há só a fonte fina no `core-data`; a interface e o repositório vêm com a SPEC-018.
- Entidades são formas de persistência, separadas dos modelos de domínio (que ainda não existem). Nenhum dado de cartão entra no banco nem nas preferências.
- **Como testar sem aparelho** (Robolectric, JUnit 4, em `src/test`):
  - **DAOs:** banco em memória, um teste por DAO (inserir e ler, `Flow` incluído).
  - **Restrições:** chave composta de `app_snapshot` recusa duplicata; a chave estrangeira recusa `scanId` inexistente; apagar um `scan_run` por SQL direto no teste apaga os snapshots em cascata.
  - **Migração inicial:** `MigrationTestHelper` com arquivo e driver, criando e validando a versão 1.
  - **Preferências:** os quatro padrões na primeira leitura, escrita e releitura, e `lastDefaultPaymentComponent` voltando a nulo quando apagado.
  - **Hilt:** teste no `core-data` que injeta banco, DAOs e DataStore.
  - **Manifesto:** o `MergedManifestTest` estendido.

## Tasks
1. [x] **Versões e plugins** (`chore:`): KSP, Hilt, Room, DataStore e as bibliotecas de teste no catálogo; plugins com `apply false` na raiz. `./gradlew build` verde.
2. [x] **Banco Room** (`feat:`): no `core-data`, plugins KSP e Room, as quatro entidades com tipos, chaves e cascata aprovados, os quatro DAOs (inserir e ler), o banco versão 1 com `exportSchema`, e o `schemas/.../1.json` gerado e commitado. Schemas nos assets do teste local.
3. [x] **Preferências** (`feat:`): fonte fina sobre `DataStore<Preferences>` com as quatro chaves e os padrões aprovados.
4. [x] **Hilt** (`feat:`): plugin Hilt no `core-data` e no `app`; módulo do Hilt no `core-data` (banco, DAOs, DataStore); `TapGuardApplication` com `@HiltAndroidApp` e `android:name` no manifesto.
5. [x] **Backup desligado** (dois commits):
   - `feat:` `allowBackup="false"`, `data_extraction_rules.xml` e `full_backup_content.xml` excluindo os nove domínios;
   - `ci: confere as regras de backup no manifesto`: passo no job "Regras do projeto" que falha se faltar `android:allowBackup="false"`, `android:dataExtractionRules` ou `android:fullBackupContent` no manifesto do `app`.
6. [x] **Testes** (`test:`, papel tester): DAOs, restrições e cascata, migração inicial, preferências, grafo do Hilt e o `MergedManifestTest` estendido (componentes exportados, backup, regras).
7. [ ] **Documentação** (`docs:`):
   - `core-data/README.md` (estado, tabelas, preferências, comandos; corrige o compileSdk 36 que ficou da SPEC-001) e `app/README.md`;
   - `CONTRIBUTING.md` e `runbook.md`;
   - `project-reference.md` (§1, §3, §5 com tipos, chaves e versão do schema, §8);
   - `conventions.md` (Database) e `context.md`;
   - `SKILLS.md`: Persistence; versões; só KSP; divergência das colunas com a skill de banco; divergência do DataStore sem interface no domínio; teste do Hilt no módulo dos tipos; `MigrationTestHelper` com driver; schemas nos assets do teste local; `MultiInstanceInvalidationService` em Permissions; backup desligado e o que o `MergedManifestTest` confere;
   - **ADR-014** em `decisions.md` (Room 2 em vez de Room 3, só KSP, colunas em camelCase, backup e transferência entre aparelhos desligados).
8. [ ] **Conferência dos critérios:** localmente, desfazendo sem commit:
   - religar o backup (`allowBackup="true"`) faz o `MergedManifestTest` falhar;
   - tirar um domínio de uma das regras faz o teste falhar;
   - exportar o serviço do Room faz o teste falhar;
   - mudar uma coluna de uma entidade sem subir a versão faz o teste de migração falhar contra o schema commitado;
   - tirar um dos três atributos de backup do manifesto faz o script do passo novo do CI falhar (rodado localmente).

## Database changes
- **Banco novo, versão 1**, com quatro tabelas:
  - `scan_run` (`id` PK autoincremento, `startedAt`, `finishedAt`?, `trigger`, `totalApps`, `redCount`, `yellowCount`);
  - `app_snapshot` (PK `scanId` + `packageName`, FK `scanId` → `scan_run.id` ON DELETE CASCADE, `label`, `versionCode`, `installer`?, `firstInstallTime`, `lastUpdateTime`, `signals`, `score`, `level`);
  - `user_decision` (PK `packageName`, `decision`, `decidedAt`);
  - `alert_event` (`id` PK autoincremento, `type`, `packageName`?, `versionCode`?, `level`?, `createdAt`, `readAt`?).

  O `?` marca coluna opcional.
- **Migração inicial:** não há `Migration` para a versão 1 (não existe versão anterior). O schema `1.json` commitado é a base, e o teste cria e valida a versão 1 a partir dele. Toda versão seguinte traz `Migration` e teste (`conventions.md`).
- **Nomes de arquivo (decididos):** banco `tapguard.db`; preferências `settings` (arquivo `settings.preferences_pb`).

## Risks & open questions
- **Índice da chave estrangeira:** a chave composta (`scanId`, `packageName`) começa por `scanId` e deve servir de índice. Se o Room avisar sobre a FK sem índice, a implementação para e pergunta, para não aceitar aviso novo.
- **`PRAGMA foreign_keys`:** a cascata depende das chaves estrangeiras ligadas na conexão. O teste da cascata confirma.
- **`MigrationTestHelper` clássico** falhou no Robolectric (experimento, item 2). O contrato usa o construtor com driver. O motivo da falha não foi identificado; não achei o erro registrado.
- **DataStore no Windows:** a segunda gravação no mesmo arquivo falha na JVM do Windows (renomear o `.tmp` por cima do existente). Os dois testes de escrita são ignorados no Windows e verificados no CI (Linux). Tentado e descartado: `createWithPath` (Okio), que na versão Android usa o mesmo armazenamento por arquivo.
- **DataStore singleton:** dois `DataStore` no mesmo arquivo derrubam o app. O Hilt fornece um só (`@Singleton`), e os testes criam o seu num arquivo temporário.
- **Transferência para iOS** (`<cross-platform-transfer>`, Android 16 QPR2): só acontece com configuração explícita de bundle e time do app iOS, que não existe aqui. Não entra nas regras.
- **Tamanho da PR:** estimativa de 600 a 800 linhas entre código e testes, acima das 500 do planner. Fica numa PR pela regra "um pull request por spec".

## Acceptance criteria
- [ ] Testes de DAO em memória passam para as quatro tabelas (AC-001 a AC-004)
- [ ] Chaves, chave estrangeira e cascata funcionam (AC-005 a AC-007)
- [ ] Migração inicial versionada: schema `1.json` commitado e versão 1 criada e validada a partir dele (AC-008, AC-009)
- [ ] Preferências disponíveis com os padrões aprovados (AC-010, AC-011)
- [ ] Hilt fornece banco, DAOs e DataStore; `TapGuardApplication` no manifesto (AC-012, AC-013)
- [ ] Só KSP, sem kapt; versões só no catálogo (AC-014)
- [ ] Backup desligado, inclusive a transferência entre aparelhos (AC-015, AC-016), e os três atributos conferidos pelo CI no manifesto (AC-023)
- [ ] Componentes exportados e permissões conferidos pelo `MergedManifestTest` (AC-017, AC-018)
- [ ] Nenhum dado de cartão no banco nem nas preferências (AC-019)
- [ ] ktlint, detekt, lint e testes passam (AC-020)
- [ ] Documentação atualizada, com o ADR-014 (AC-021)
- [ ] CI da PR verde (AC-022)
