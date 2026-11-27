# core-data

Biblioteca Android com o banco local (Room), as preferências (DataStore), os repositórios e o histórico de varreduras, decisões e alertas.

> **Estado (SPEC-018 BATCH-02):** banco Room versão 1 com as quatro tabelas da seção 9, preferências com os padrões, o módulo do Hilt (SPEC-004), o repositório de varreduras com a retenção (`RoomScanRepository`, BATCH-01), a leitura dos apps confiáveis (`RoomTrustedAppRepository`) e as preferências que voltam aos padrões com o arquivo ilegível (BATCH-02).

## Dependências

- **Módulos:** `core-domain` (e o `testFixtures` dele nos testes).
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), `com.google.devtools.ksp`, `com.google.dagger.hilt.android`, `androidx.room`, ktlint e detekt.
- **Bibliotecas:** Room 2.8.5 (`room-runtime`, `room-compiler` por KSP), DataStore Preferences 1.3.0, Hilt 2.60.1 (`hilt-android`, `hilt-compiler` por KSP). Sem kapt.
- **Testes:** JUnit 4, Robolectric 4.18, `androidx.test:core`, `room-testing` 2.8.5, `kotlinx-coroutines-test` 1.11.0, `hilt-android-testing` 2.60.1.
- **Namespace:** `io.github.valossa515.tapguard.core.data` · minSdk 26 · compileSdk 37.

## Estrutura

| Pacote | Conteúdo |
|--------|----------|
| `database` | `TapGuardDatabase` (versão 1, arquivo `tapguard.db`) |
| `database.entity` | `ScanRunEntity`, `AppSnapshotEntity`, `UserDecisionEntity`, `AlertEventEntity` |
| `database.dao` | `ScanRunDao`, `AppSnapshotDao`, `UserDecisionDao`, `AlertEventDao` (inserir e ler; o `ScanRunDao` também aplica a retenção) |
| `repository` | `RoomScanRepository` (porta `ScanRepository`) e `RoomTrustedAppRepository` (porta `TrustedAppRepository`), do `core-domain` |
| `settings` | `SettingsDataSource` (DataStore `settings`) |
| `di` | `DataModule` (Hilt, `SingletonComponent`): banco e DataStore únicos, e cada DAO; `RepositoryModule` (`@Binds`): `ScanRepository` → `RoomScanRepository` e `TrustedAppRepository` → `RoomTrustedAppRepository` |

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

## Gravação e retenção (SPEC-018, ADR-023)

`RoomScanRepository.save(report)` faz tudo num `database.withTransaction`, nesta ordem:

1. insere a linha de `scan_run`: datas em milissegundos (`finishedAt` sempre preenchido), `trigger` pelo nome, `totalApps`, e `redCount` e `yellowCount` pelo nível final (um app confiável conta como verde);
2. insere um `app_snapshot` por app: campos do `InstalledApp`, `signals` pela máscara (`toBitMask()`), `score` e `level` pelo nome;
3. aplica a retenção: `ScanRunDao.deleteOlderThanLatest(10)` mantém as 10 varreduras de **maior `id`** (ordem de gravação). Não ordena por `startedAt`, porque com o relógio do aparelho atrasado a varredura recém-gravada seria apagada. Os snapshots das apagadas caem pela cascata. `keep` menor que 1 lança `IllegalArgumentException` (mensagem fixa) sem apagar nada; por isso o `ScanRunDao` é `abstract class`, com a consulta `protected` atrás do `require`.

Uma falha em qualquer passo desfaz os três e propaga a exceção. Permissões, serviços e o pagamento padrão não são gravados (`lastDefaultPaymentComponent` é da SPEC-052). Sem índice novo: a subconsulta usa a chave primária, e a cascata usa o índice da chave composta. Alertas por 90 dias: SPEC-053.

## Apps confiáveis (SPEC-018, ADR-023)

`RoomTrustedAppRepository.trustedPackages()` devolve, num conjunto que não pode ser alterado, os pacotes de `user_decision` com `decision = 'TRUSTED'` (`UserDecisionDao.getTrustedPackageNames()`). A comparação é exata: `trusted` em minúsculas não conta. Só leitura; marcar e desfazer entram na SPEC-033.

## Preferências (DataStore `settings`)

| Chave | Tipo | Padrão |
|-------|------|--------|
| `onboardingDone` | booleano | falso |
| `monitoringEnabled` | booleano | verdadeiro |
| `scanIntervalHours` | inteiro | 6 (a validação de 1, 6, 12 ou 24 fica para a primeira spec que usar preferências, SPEC-051 ou SPEC-054) |
| `lastDefaultPaymentComponent` | texto | nulo quando nunca registrado; gravar nulo apaga |

Arquivo ilegível ou corrompido (`IOException`, inclusive a `CorruptionException` do DataStore): as quatro leituras devolvem os padrões, como pede o comentário da #14. Outra exceção é propagada. As gravações não mudam; com o arquivo corrompido, elas ainda falham.

## Regras

- Nenhum dado de cartão entra no banco nem nas preferências.
- Um único DataStore por arquivo: o `DataModule` fornece um só (`@Singleton`).
- `fallbackToDestructiveMigration()` é proibido em release.

## Testes

- DAOs em banco em memória (Robolectric), com chave duplicada e snapshot órfão recusados e a cascata; retenção do `ScanRunDao` por `id` (12 → 10, relógio atrasado, menos de 10, cascata, `keep` 0 e -1 recusados).
- `RoomScanRepositoryTest` (Room em memória): colunas de `scan_run` e `app_snapshot` com máscaras escritas à mão, contagens com app confiável, relatório vazio, retenção pelo `save` e rollback com pacote repetido (as 10 antigas continuam).
- Migração inicial: `MigrationTestHelper` com arquivo e driver (`AndroidSQLiteDriver`) cria e valida a versão 1.
- `SettingsDataSourceTest` em JUnit 4 puro com DataStore real. **No Windows**, os dois testes que gravam mais de uma vez são ignorados (`Assume`), porque a JVM do Windows não consegue renomear o arquivo temporário do DataStore por cima do existente. Eles rodam no CI (Linux). Os três testes de leitura do BATCH-02 (arquivo corrompido, pasta no lugar do arquivo, outra exceção relançada) só leem e rodam também no Windows.
- `RoomTrustedAppRepositoryTest` (Room em memória): só os pacotes `TRUSTED`, com `trusted` em minúsculas ignorado; tabela vazia.
- Grafo do Hilt (`DataModuleTest`, `HiltTestApplication`): banco, DAOs, DataStore, `ScanRepository` e `TrustedAppRepository`, com singletons.

## Comandos

```bash
./gradlew :core-data:build
./gradlew :core-data:testDebugUnitTest   # DAOs, repositório, migração, preferências e Hilt (Robolectric)
```
