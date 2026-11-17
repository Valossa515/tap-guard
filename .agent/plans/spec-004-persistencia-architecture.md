# Architecture: SPEC-004 Base de persistência e injeção

> Entrada: `.agent/plans/spec-004-persistencia.md`, `.agent/specs/features/spec-004-persistencia.spec.toon` (FT-004), `.agent/specs/acceptance/spec-004-persistencia.acceptance.toon`
> Contrato: `.agent/specs/contracts/spec-004-persistencia.contract.toon` · 2026-11-17

## Summary
A persistência fica toda no `core-data`, no pacote `io.github.valossa515.tapguard.core.data`, em cinco subpacotes:

| Subpacote | Conteúdo |
|-----------|----------|
| `database` | o `TapGuardDatabase` |
| `database.entity` | as quatro entidades |
| `database.dao` | os quatro DAOs |
| `settings` | a fonte das preferências |
| `di` | o módulo do Hilt |

Room e Hilt são processados por KSP. O plugin `androidx.room` exporta o schema em `core-data/schemas`.

O `app` ganha só a `TapGuardApplication` (`@HiltAndroidApp`), o manifesto com o backup desligado e as duas regras de backup, e o `MergedManifestTest` estendido. Um passo novo no job "Regras do projeto" confere no manifesto o que o teste não consegue ler.

Os testes rodam em `src/test`, com Robolectric e JUnit 4. A exceção é a fonte de preferências, testada em JUnit 4 puro sobre um arquivo temporário.

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. Cada módulo declara a própria configuração com aliases do catálogo e plugins `apply false` na raiz (`app/build.gradle.kts`, `core-data/build.gradle.kts`, `build.gradle.kts`).
  2. Testes Robolectric com `@Config(sdk = [36])`, nomes com crases, `org.junit.Assert`, sem classes-base nem fixtures compartilhadas (`MergedManifestTest`, `TapGuardNavHostTest`, `RiskLevelIndicatorTest`).
  3. Um teste lê o manifesto mesclado pelo `PackageManager` do Robolectric e confere uma lista explícita (`MergedManifestTest`). Passos do CI em shell, dentro dos jobs obrigatórios (`ci.yml`, "Nenhum manifesto declara INTERNET").
- **Padrão seguido:** os três acima. Para o código de persistência e de DI, que ainda não existe no repositório, vale a skill (`skills/android/SKILL.md`: Persistence — Room, DataStore, Dependency Injection — Hilt; `skills/android/testing.md`: Room Tests), com as divergências aprovadas. O projeto descartável da SPEC-004 validou exatamente essa estrutura.

## Layer Decisions

| Camada (pacote) | Decisão | Justificativa |
|------|---------|---------------|
| build | catálogo (KSP, Hilt, Room, DataStore, bibliotecas de teste); plugins `ksp`, `hilt` e `room` com `apply false` na raiz; `core-data` aplica `ksp`, `hilt` e `room`; `app` aplica `ksp` e `hilt` | BR-009; mesmo padrão das specs anteriores |
| CI | passo "Regras de backup no manifesto" no job "Regras do projeto" (`grep -qF` nos três atributos) | BR-015; o `MergedManifestTest` não consegue ler `dataExtractionRules` e `fullBackupContent`, que não são API pública |
| `core.data.database.entity` | `ScanRunEntity`, `AppSnapshotEntity`, `UserDecisionEntity`, `AlertEventEntity`; nomes de tabela e coluna exatos da seção 9; tipos, opcionais, chaves e a FK com cascata como no plano | BR-001 a BR-004; nome `{Thing}Entity` (skill) |
| `core.data.database.dao` | `ScanRunDao`, `AppSnapshotDao`, `UserDecisionDao`, `AlertEventDao`, só com inserir e ler; conflito padrão (`ABORT`), que gera `ConstraintViolation` em duplicata | BR-005; nome `{Thing}Dao` (skill) |
| `core.data.database` | `TapGuardDatabase`: `@Database(version = 1, exportSchema = true)` com as quatro entidades e um acessor por DAO; constante com o nome `tapguard.db` | BR-006, BR-014 |
| `core.data.settings` | `SettingsDataSource` com `@Inject constructor(DataStore<Preferences>)`: um `Flow` por preferência com o padrão e um `suspend` de escrita por preferência; `null` em `lastDefaultPaymentComponent` remove a chave | BR-007; fonte fina sem regra (divergência aprovada) |
| `core.data.di` | `DataModule` (`object`, `SingletonComponent`): `@Singleton` banco (`Room.databaseBuilder(..., "tapguard.db")`), cada DAO, `@Singleton` `DataStore<Preferences>` (`PreferenceDataStoreFactory.create { preferencesDataStoreFile("settings") }`) | BR-008, BR-014; nome `{Scope}Module` (skill) |
| `app` (raiz) | `TapGuardApplication` com `@HiltAndroidApp`, sem lógica | BR-008 |
| manifesto e recursos do `app` | `android:name=".TapGuardApplication"`, `android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:fullBackupContent="@xml/full_backup_content"`; os dois XML excluem os nove domínios | BR-010 |
| gerado e commitado | `core-data/schemas/io.github.valossa515.tapguard.core.data.database.TapGuardDatabase/1.json` (gerado pelo KSP do Room) | BR-006 |
| docs | READMEs, CONTRIBUTING, runbook, project-reference, conventions, context, SKILLS, ADR-014 | AC-021 |

Dependência entre pacotes: `di → database, settings`; `database → database.entity, database.dao`. Nada aponta para o `app`. O `core-domain` não é tocado.

## Dependencies
- **Reusing:**
  - a configuração existente do `core-data` e do `app` (namespace, SDKs, toolchain, ktlint, detekt);
  - o `MergedManifestTest`, estendido;
  - o job "Regras do projeto";
  - Robolectric 4.18 e `androidx.test:core` 1.7.0, já no catálogo.
- **New** (aprovadas no plano):
  - produção: KSP 2.3.14 (plugin), Hilt 2.60.1 (plugin, `hilt-android`, `hilt-compiler` por KSP), Room 2.8.5 (plugin `androidx.room`, `room-runtime`, `room-compiler` por KSP), DataStore Preferences 1.3.0;
  - teste: `hilt-android-testing` 2.60.1 (com `kspTest` do `hilt-compiler`), `room-testing` 2.8.5, `kotlinx-coroutines-test` 1.11.0.
- O `AndroidSQLiteDriver` do teste de migração vem do `androidx.sqlite:sqlite-framework`, que já chega pelo `room-runtime`. Não é dependência declarada.
- **Escopo `implementation`** no `core-data`: o `app` não usa tipos do Room nem do DataStore. O Hilt monta o grafo pelo processamento agregado, por isso o teste do grafo fica no `core-data` (experimento, item 4).

## Database Changes
- Banco novo `tapguard.db`, versão 1, quatro tabelas (detalhes no plano). Sem `Migration`: não há versão anterior.
- O schema `1.json` é gerado no build e commitado. É o contrato com as próximas versões.
- Schemas nos assets do teste local: `android.sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")` (forma não depreciada do AGP 9). O plugin do Room só cobre o `androidTest`.
- Índices: só os das chaves primárias. A chave composta de `app_snapshot` começa por `scanId`, a coluna da FK. Índices para retenção e duplicidade, se precisarem, vêm na SPEC-018 com a versão 2 e a primeira `Migration`.

## Test Strategy Rationale
- **DAOs em memória (Robolectric):** a SQLite real do Robolectric executa as consultas e as restrições geradas pelo Room. Mockar DAO é proibido pela skill. Um teste por DAO, mais os casos de restrição no `AppSnapshotDaoTest`. A cascata é verificada apagando o `scan_run` por SQL direto (`database.openHelper.writableDatabase.execSQL`), porque o contrato não dá aos DAOs operação de apagar.
- **Migração inicial:** `MigrationTestHelper(instrumentation, file, AndroidSQLiteDriver(), TapGuardDatabase::class)` cria a versão 1 a partir do `1.json` e valida. O construtor clássico falhou no experimento.
- **Preferências em JUnit 4 puro:** o `SettingsDataSource` só recebe um `DataStore<Preferences>`. O teste cria um com `PreferenceDataStoreFactory.create(scope, produceFile = arquivo temporário)`, sem Android. Mais rápido, e isola a regra de padrões.
- **Grafo do Hilt no `core-data`:** `@HiltAndroidTest` + `HiltTestApplication` + `HiltAndroidRule`. Injeta banco, quatro DAOs e DataStore e confere que banco e DataStore são singletons (mesma instância em duas injeções). O banco injetado aponta para `tapguard.db` no diretório do Robolectric, isolado por teste.
- **Manifesto mesclado:** o `MergedManifestTest` ganha:
  - todos os componentes exportados contra a lista, no lugar do teste "só a `MainActivity` exportada";
  - a flag de backup;
  - a leitura dos dois XML de regras;
  - `applicationInfo.className` igual a `io.github.valossa515.tapguard.TapGuardApplication` (ligação da `Application` do Hilt no manifesto).

  A lista de permissões não muda.
- **CI:** a ligação dos três atributos no manifesto é verificada pelo passo novo. O caso negativo roda localmente na conferência.
- **Excluídos:** entidades (dados sem lógica, cobertas pelos testes dos DAOs), `TapGuardDatabase` (coberto pelos DAOs e pela migração), `TapGuardApplication` (só a anotação; o grafo é coberto no `core-data` e a ligação no manifesto pelo `MergedManifestTest`), os XML de regras e o `1.json` (cobertos pelo `MergedManifestTest` e pelo teste de migração).

## Trade-offs & Alternatives Considered
- **Room 3 (`androidx.room3`):** fora, por decisão do usuário. A skill e as convenções seguem o Room 2.
- **`MigrationTestHelper` clássico:** falha no Robolectric. Teste instrumentado exigiria emulador no CI.
- **Teste do grafo do Hilt no `app`:** exigiria tornar Room e DataStore `api` no `core-data`, ou declará-los no `app`, só para o teste. Rejeitado.
- **`api` em vez de `implementation`** no `core-data`: rejeitado. O `app` não usa esses tipos, e o Hilt agrega o grafo sem isso.
- **Ler `ApplicationInfo.dataExtractionRulesRes` no teste:** não é API pública (não compila). Substituído pelo passo do CI e pela leitura dos XML.
- **DAO com operação de apagar só para testar a cascata:** rejeitado. Seria API sem uso nesta spec, contra a decisão "só inserir e ler".
- **`@Upsert` em `user_decision`:** rejeitado nesta spec (é decisão de repositório, da SPEC-018). O conflito padrão `ABORT` vale para todas as tabelas.

## Risks
- **Aviso do Room sobre FK sem índice:** se aparecer, o builder para e pergunta (o projeto não aceita aviso novo).
- **`PRAGMA foreign_keys`:** a cascata e a recusa do órfão dependem das chaves estrangeiras ligadas na conexão. Os dois testes confirmam.
- **Instâncias do DataStore:** o teste do grafo do Hilt e o `SettingsDataSourceTest` usam arquivos diferentes (`settings` no diretório do Robolectric e um arquivo temporário). Não há duas instâncias no mesmo arquivo.
- **Tamanho da PR:** estimativa de 600 a 800 linhas entre código e testes, acima das 500 do planner. Fica numa PR pela regra "um pull request por spec".
