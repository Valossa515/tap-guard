# Architecture: SPEC-018 — Caso de uso de varredura

> Entrada: `.agent/plans/spec-018-varredura.md`, `.agent/specs/features/spec-018-varredura.spec.toon` (FT-018 v1.1), `.agent/specs/acceptance/spec-018-varredura.acceptance.toon` (v1.1).
> Contrato: `.agent/specs/contracts/spec-018-varredura.contract.toon` (v1.0) · revisão do architect em 2026-11-27.

**Resultado da revisão: aprovada sem mudança de comportamento.** As cinco decisões do usuário e os quatro batches ficam. Muda um nome (`TrustedAppsRepository` → `TrustedAppRepository`) e entra o AC-029 pedido na aprovação. Pela instrução do usuário, o fluxo segue direto para o BATCH-01.

## Summary

O `DefaultScanDevice` (Kotlin puro, `core-detection`) orquestra inventário, coletores e motor e grava por duas portas do `core-domain`. O `core-data` implementa as portas com Room: o `RoomScanRepository` grava `scan_run`, os `app_snapshot` e aplica a retenção num único `withTransaction`. O `app` faz a primeira ligação do Hilt para detecção. Nenhuma mudança de schema.

## Pattern Analysis

| Padrão existente | Exemplos (3+) | Seguido em |
| --- | --- | --- |
| Implementação de porta nomeada pelo "como" + porta | `PackageManagerAppInventory : AppInventory`, `WeightedRiskScorer : RiskScorer`, coletores `*Collector : SignalCollector`; a skill fixa `RoomScanRepository` | `RoomScanRepository`, `RoomTrustedAppRepository`, `DefaultScanDevice` |
| Porta no singular | `ScanRunDao`, `AppSnapshotDao`, `UserDecisionDao`; a skill fixa `ScanRepository` | `ScanRepository`, `TrustedAppRepository` |
| Classe do `core-data` pública com `@Inject constructor` | `SettingsDataSource`; DAOs públicos fornecidos pelo `DataModule` | repositórios públicos com `@Inject constructor`, ligados por `@Binds` |
| Construtor que copia coleções recebidas | `InstallSourceCollector`, `DefaultPaymentAppCollector`, `BankLookalikeCollector` (`Collections.unmodifiableSet(LinkedHashSet(...))`) | `DefaultScanDevice` copia a lista de coletores (`Collections.unmodifiableList(ArrayList(...))`) |
| Testes Room em memória com Robolectric `@Config(sdk = [36])` | `ScanRunDaoTest`, `AppSnapshotDaoTest`, `UserDecisionDaoTest` | `RoomScanRepositoryTest`, testes novos do `ScanRunDaoTest` |
| Grafo do Hilt testado no módulo dono dos tipos | `DataModuleTest` | `DataModuleTest` (portas), `app/di/DetectionModuleTest` |
| Dispatcher injetado provado com `CountingDispatcher` | `PackageManagerAppInventoryTest` | `DefaultScanDeviceTest` |

## Revisão dos nomes

| Nome | Veredito | Motivo |
| --- | --- | --- |
| `DefaultScanDevice` | Mantido | Única implementação, sem tecnologia (Kotlin puro); "Default" diz que é a implementação padrão que o SDK pode trocar. Não há "como" melhor: ele só orquestra |
| `ScanRepository` | Mantido | Nome da skill; porta no singular |
| `TrustedAppsRepository` | **Renomeado para `TrustedAppRepository`** | Singular, como as outras portas e os DAOs. O conceito do glossário é "App confiável" |
| `@IoDispatcher`, `@DefaultDispatcher` | Mantidos | Um qualificador por dispatcher, nome do `Dispatchers.*` que entrega; `@Qualifier` com `@Retention(BINARY)` |
| `ScanRunDao.deleteOlderThanLatest(keep)` | Novo (nome da skill) | O exemplo da skill ordena por `startedAt`; aqui "latest" é o maior `id` (decisão 6), dito na KDoc |
| `RoomScanRepository.RETAINED_SCANS` | Novo | `private const val` = 10 (detekt `MagicNumber`); os testes escrevem 10 como literal |

## Onde fica cada classe

| Módulo · pacote | Arquivo | Visibilidade | Batch |
| --- | --- | --- | --- |
| `core-domain` · `scan` | `ScanRepository.kt` | pública (interface) | 01 |
| `core-domain` · `risk` | `TrustedAppRepository.kt` (ao lado do `ScoringContext`, que recebe o conjunto) | pública (interface) | 02 |
| `core-detection` · `scan` (pacote novo) | `DefaultScanDevice.kt` | pública, sem anotações | 03 |
| `core-data` · `database.dao` | `ScanRunDao.kt`, `UserDecisionDao.kt` (consultas novas) | públicas | 01, 02 |
| `core-data` · `repository` (pacote novo) | `RoomScanRepository.kt`, `RoomTrustedAppRepository.kt` | públicas, `@Inject constructor` | 01, 02 |
| `core-data` · `di` | `RepositoryModule.kt` (`abstract class`, `@Binds`) | pública | 01 (+02) |
| `core-data` · `settings` | `SettingsDataSource.kt` (`catch`) | pública | 02 |
| `app` · `di` (pacote novo) | `DispatcherQualifiers.kt`, `DispatchersModule.kt`, `ClockModule.kt`, `InventoryModule.kt`, `DetectionModule.kt` | públicas | 04 |

**Público no `core-detection`:** só entra o `DefaultScanDevice`, e a API pública passa de 15 para 16 classes. O construtor dele só usa tipos públicos (`AppInventory`, `SignalCollector`, `TrustedAppRepository`, `ScanRepository`, `Clock`, `CoroutineDispatcher`), que o módulo expõe por `api(core-domain)` e `api(kotlinx-coroutines-core)`. O módulo não ganha dependência nova.

**Por que os repositórios não são `internal`:** o `@Binds` público precisaria expor um tipo `internal`, e um `@Binds internal` tem nome deformado na JVM, que o Dagger gerado no `app` teria de enxergar. O padrão do módulo já é público (`SettingsDataSource`, DAOs).

## Transação da gravação com a retenção

```
RoomScanRepository(database, scanRunDao, appSnapshotDao).save(report) =
  database.withTransaction {
    id = scanRunDao.insert(report.toScanRun())          // finishedAt sempre preenchido
    appSnapshotDao.insertAll(report.apps.map { it.toSnapshot(id) })
    scanRunDao.deleteOlderThanLatest(RETAINED_SCANS)    // cascata apaga os snapshots
  }
```

- **Uma transação, três escritas, nessa ordem.** A retenção vem depois dos inserts, para a varredura nova contar entre as 10. Uma falha em qualquer passo desfaz tudo: sem varredura pela metade e sem retenção aplicada. O cancelamento durante o bloco também desfaz.
- **Construtor com os três** (`database` para a transação, os dois DAOs para as escritas), como o exemplo `RoomScanRepository` da skill.
- `withTransaction` vem do `room-runtime` 2.8.5 (conferido no clone; sem `room-ktx`). Os `@Insert` e o `@Query` `suspend` participam da mesma transação.
- Consulta: `DELETE FROM scan_run WHERE id NOT IN (SELECT id FROM scan_run ORDER BY id DESC LIMIT :keep)`. Plano conferido: usa a chave primária e o índice da chave composta na cascata; nenhum índice novo.
- Mapeamento em funções `private` do repositório: `Instant.toEpochMilli()`, `ScanTrigger.name`, `RiskLevel.name`, `Set<Signal>.toBitMask()` (do `core-domain`) e as contagens `RED` e `YELLOW` pelo nível final.

## Estimativa contra os arquivos do contrato

| Batch | Arquivo | Linhas |
| --- | --- | ---: |
| 01 | `ScanRepository.kt` | 15 |
| 01 | `ScanRunDao.kt` (consulta e KDoc) | 10 |
| 01 | `RoomScanRepository.kt` | 75 |
| 01 | `RepositoryModule.kt` | 20 |
| 01 | `core-data/build.gradle.kts` (`testFixtures`) | 1 |
| 01 | `RoomScanRepositoryTest.kt` | 210 |
| 01 | `ScanRunDaoTest.kt` (retenção) | 70 |
| 01 | `DataModuleTest.kt` | 6 |
| **01** | **total** | **~407** |
| 02 | `TrustedAppRepository.kt`, `UserDecisionDao.kt`, `RoomTrustedAppRepository.kt`, `RepositoryModule.kt` | 50 |
| 02 | `SettingsDataSource.kt` | 12 |
| 02 | `RoomTrustedAppRepositoryTest.kt`, `UserDecisionDaoTest.kt` | 80 |
| 02 | `SettingsDataSourceTest.kt` | 55 |
| 02 | `DataModuleTest.kt` | 5 |
| **02** | **total** | **~202** |
| 03 | `DefaultScanDevice.kt` | 85 |
| 03 | `DefaultScanDeviceTest.kt` (portas e coletores falsos, relógio que avança e volta) | 320 |
| **03** | **total** | **~405** |
| 04 | cinco arquivos de `di/` | 135 |
| 04 | `app/build.gradle.kts`, `app/detekt.yml` | 8 |
| 04 | `DetectionModuleTest.kt` | 150 |
| 04 | `ScanDeviceEndToEndTest.kt` (desempenho e AC-029) | 165 |
| **04** | **total** | **~458** |

Soma de cerca de 1.470 linhas; o `estimate-lines: 1500` fica. **Nenhum batch passa de 500, e não é preciso redividir.** O BATCH-04 é o mais justo (~458): o AC-029 somou cerca de 40 linhas, compensadas por tirar do `DetectionModuleTest` o caso "app com todos os sinais na lista" (AC-022 passa a conferir só as classes). Se o BATCH-04 passar de 500 na implementação, o `DetectionModuleTest` sai para um PR próprio antes do ponta a ponta.

## Tarefas do BATCH-01 (um commit cada)

1. `feat`: porta `ScanRepository` e consulta `deleteOlderThanLatest` no `ScanRunDao`.
2. `feat`: `RoomScanRepository` (mapeamento e transação) e `RepositoryModule`.
3. `test`: `testFixtures` no `core-data`; `RoomScanRepositoryTest`, retenção no `ScanRunDaoTest`, `DataModuleTest`; plano de testes.
4. `docs`: documentação do BATCH-01 e ADR-023 (a spec inteira).

## Test Strategy Rationale

- **Repositório e DAO separados:** o `ScanRunDaoTest` prova a semântica da consulta (por `id`, relógio atrasado, menos de 10, cascata) direto no DAO, como os testes da SPEC-004. O `RoomScanRepositoryTest` prova mapeamento, contagens, relatório vazio, que o `save` aplica a retenção e a atomicidade (pacote repetido desfaz tudo e mantém as 10 antigas).
- **Máscaras e contagens escritas à mão** (`7`, `263`), sem chamar `toBitMask()` no teste: uma troca do bit derruba o teste.
- **Sem medir tempo no BATCH-01:** o desempenho é medido no BATCH-04, pelo grafo real.
- **Conferência local (desfeita sem commit):** retenção por `startedAt` no lugar de `id`; inserts e retenção fora do `withTransaction`; `RETAINED_SCANS = 11`; retenção antes dos inserts; trocar `RED`/`YELLOW` nas contagens. Cada uma deve derrubar pelo menos um teste.

## Trade-offs & Alternatives Considered

- **Repositórios `internal`:** recusado pelos motivos acima.
- **`@Transaction` num método `open` de DAO abstrato:** recusado, porque a skill põe a escrita em várias tabelas no repositório com `withTransaction` e os DAOs do projeto são interfaces.
- **Retenção numa porta separada (`applyRetention`):** recusado, porque o `ScanDevice` teria de chamar dois métodos e a atomicidade dependeria dele.
- **Repositório com só o `database`, tirando os DAOs dele:** equivalente; ficou o formato da skill.
- **Qualificador único com enum (`@Dispatcher(IO)`):** recusado; dois qualificadores simples bastam.

## Risks

- **BATCH-04 perto do limite:** mitigação descrita acima.
- **SQLite do Robolectric diferente do aparelho:** os testes de atomicidade dependem do rollback do SQLite, que é o mesmo motor. O tempo é só indicativo; a SPEC-022 mede em aparelho.

## Conferência do BATCH-01 — 2026-11-27

| Arquivo | Estimado | Real |
| --- | ---: | ---: |
| `ScanRepository.kt` | 15 | 11 |
| `ScanRunDao.kt` | 10 | 8 |
| `RoomScanRepository.kt` | 75 | 68 |
| `RepositoryModule.kt` | 20 | 16 |
| `core-data/build.gradle.kts` | 1 | 1 |
| `RoomScanRepositoryTest.kt` | 210 | 236 |
| `ScanRunDaoTest.kt` | 70 | 65 |
| `DataModuleTest.kt` | 6 | 11 |
| **Total** | **407** | **416** |

Diff de código e testes desde o commit dos artefatos: 416 linhas adicionadas, 0 removidas. Os nomes, os arquivos e a transação seguiram o contrato sem emenda.

### Emenda da correção do review — 2026-11-27

O usuário pediu `require(keep > 0)` na retenção. Um `@Query` de interface não tem corpo, então o `ScanRunDao` passou a `abstract class`: `deleteOlderThanLatest(keep)` é público, valida e chama `deleteOutsideLatest(keep)`, `protected abstract` com o `@Query`. Assim não existe caminho público sem a validação. O Room 2.8.5 aceita essa forma por KSP (conferido no build). Sem mudança de comportamento para `keep` maior que 0.

| | Linhas |
| --- | ---: |
| `ScanRunDao.kt` (forma e `require`) | +21 −6 |
| `ScanRunDaoTest.kt` (teste novo) | +14 |
| **BATCH-01 com a correção** | **449 adicionadas, 4 removidas = 453** (estimativa 407) |

## BATCH-02 — emenda 1.2 e tarefas (2026-11-27)

**Emenda 1.2 do contrato (sem mudança de comportamento):** a estratégia do BATCH-02 não derrubava duas das mutações pedidas para a conferência.
- **Catch de qualquer exceção:** só havia cenários com `IOException`. Entra `other-exceptions-are-rethrown-AC-019`: um DataStore real (`PreferenceDataStoreFactory.create`) cujo produtor do arquivo lança `IllegalStateException`. Conferido num clone: a exceção chega a quem lê; com o `catch` só de `IOException` ela passa, e com um `catch` de tudo ela some. Cobre o "relançam o resto" que a BR-010 já pedia, sem fingir o store (skill de testes, DataStore).
- **Consulta sem filtro:** o cenário dos confiáveis grava também uma linha com `decision` em minúsculas (`trusted`), que não conta. Prova a comparação exata sem inventar outro valor de decisão.

**Tarefas do BATCH-02 (um commit cada):**
1. `feat`: porta `TrustedAppRepository`, consulta `getTrustedPackageNames` no `UserDecisionDao`, `RoomTrustedAppRepository` e a ligação no `RepositoryModule`.
2. `fix`: `IOException` nas quatro leituras do `SettingsDataSource` (comentário da #14).
3. `test`: `RoomTrustedAppRepositoryTest`, `SettingsDataSourceTest` (corrompido, pasta, exceção relançada), `DataModuleTest`; plano de testes.
4. `docs`: documentação do BATCH-02 e status do ADR-023.
