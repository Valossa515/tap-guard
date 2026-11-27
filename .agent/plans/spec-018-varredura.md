# Plan: SPEC-018 — Caso de uso de varredura

> Planejado em 2026-11-27 · [Issue #14](https://github.com/Valossa515/tap-guard/issues/14) · RF-101 a RF-103, RNF-03 · fecha a Etapa 1.
> A #14 foi movida com `gh` de "Pronta" para **"Em andamento"** no [Project 2](https://github.com/users/Valossa515/projects/2) antes deste plano; status confirmado por nova consulta.
> FT-018 v1.1, `interface: library`, **`feature.estimate-lines: 1500`**, **quatro batches** (um PR cada). Dependências SPEC-004, SPEC-010 a SPEC-017 e SPEC-019, todas fechadas.
> **Aprovado pelo usuário em 2026-11-27** (cinco decisões e quatro batches). **Emenda 1.1** (mesma data): o usuário pediu o AC-029 no BATCH-04, e a revisão do architect (`spec-018-varredura-architecture.md`) trocou `TrustedAppsRepository` por `TrustedAppRepository` (singular, como `ScanRepository` e os DAOs). Nenhuma decisão de comportamento mudou.
> Entrega deste planejamento: plano, feature spec e critérios de aceite. Sem código no repositório. Os experimentos rodaram num clone descartável no scratchpad, fora do repositório.

## Resumo

Implementar o `ScanDevice` da §7: pede o inventário, roda os nove coletores em cada app, calcula score e nível com o `WeightedRiskScorer`, respeitando os apps confiáveis do usuário (regra 2), grava a varredura (`scan_run` e um `app_snapshot` por app) e mantém só as 10 varreduras mais recentes, tudo numa transação. A spec também faz a primeira ligação de verdade no Hilt (inventário, coletores montados, `ScanDevice`, `Clock`, dispatchers), cria os dois repositórios de que o `ScanDevice` precisa e trata a `IOException` nas leituras das preferências (comentário da #14).

Fontes: issue #14 e comentário; §5, §7, §8, §9 e §10 de `docs/especificacao.md`; ADR-014 a ADR-022; "Ligação com a SPEC-052" do plano da SPEC-016 e "Contrato esperado do coletor e integração futura" do plano da SPEC-019; `.agent/SKILLS.md` e as skills Android, testes, banco, anti-invenção, lacunas, padrões, validação de specs e DoR.

## Análise de lacunas

| # | Ponto | Resultado |
| --- | --- | --- |
| 1 | Onde mora a implementação do `ScanDevice` | Definido: `core-detection` (§7, project-reference). Persistência por portas do `core-domain` |
| 2 | Apps confiáveis | O dado existe (`user_decision`, SPEC-004); falta a leitura. Porta e repositório criados aqui; marcar e desfazer ficam na SPEC-033 |
| 3 | Comparação com a anterior (§7, passo 5) | Fora: SPEC-050 (função pura) e SPEC-051 (worker) |
| 4 | Retenção de alertas (90 dias) | Fora: critério da SPEC-053. O project-reference dizia SPEC-018 e será corrigido |
| 5 | `lastDefaultPaymentComponent` | Fora: SPEC-052 (ADR-021). O relatório devolvido já leva `defaultPaymentServices` |
| 6 | "Mais recentes" na retenção | **Decisão do usuário:** pela ordem de gravação (`id`), não por `startedAt` |
| 7 | Relógio que volta durante a varredura (`ScanReport` recusa fim antes do início) | **Decisão do usuário:** `finishedAt` = o maior entre `startedAt` e o agora |
| 8 | Onde tratar a `IOException` das preferências | **Decisão do usuário:** nas quatro leituras do `SettingsDataSource`; `SettingsRepository` no `core-domain` fica para a primeira spec que usar preferências |
| 9 | Construtor com 7 dependências (detekt recusa 7; `@Suppress` proibido) | **Decisão do usuário:** o `ScanDevice` cria o `WeightedRiskScorer`, como o ADR-022 já prevê ("o `ScanDevice` e o SDK o constroem"). Ficam 6 |
| 10 | `@Provides` do `ScanDevice` com 6 parâmetros (detekt recusa 6 em função) | **Decisão do usuário:** `LongParameterList` com `ignoreAnnotated: ['Provides']` em `app/detekt.yml`, na forma da isenção do `@Composable` |

Nenhuma lacuna bloqueante restante. Nomes de tipos e arquivos abaixo são propostas; o architect fixa no contrato.

## Escopo

| Módulo | Muda |
| --- | --- |
| `core-domain` | Duas portas novas: `ScanRepository` (`suspend fun save(report: ScanReport)`) e `TrustedAppRepository` (`suspend fun trustedPackages(): Set<String>`). Interfaces não contam no Kover |
| `core-detection` | `DefaultScanDevice` (público, sem `android.*`, sem Hilt) em `core.detection.scan`. API pública passa de 15 para 16 classes |
| `core-data` | Consultas novas nos DAOs, `RoomScanRepository`, `RoomTrustedAppRepository`, `RepositoryModule` (`@Binds`), tratamento de `IOException` no `SettingsDataSource`. Schema e `1.json` **sem mudança** |
| `app` | Primeiros módulos do Hilt do app (`di/`), isenção do detekt para `@Provides`, dependências de teste do Hilt |
| Sem mudança | Coletores, listas, inventário, motor, modelos, permissões, manifestos, telas |

## ScanDevice

**Onde:** a interface fica no `core-domain` (`scan/ScanDevice`, SPEC-010); a implementação `DefaultScanDevice` fica no `core-detection`, ao lado dos coletores (§7, ADR-017). Não usa API do Android, então os testes são JUnit 4 na JVM. Não tem anotação do Hilt nem `javax.inject`: o `core-detection` vai virar SDK (ADR-016), e o app a constrói num `@Provides`. Quem usar o SDK passa as próprias implementações das duas portas (uma em memória basta).

**Construtor (6):** `inventory: AppInventory`, `collectors: List<SignalCollector>` (copiada), `trustedApps: TrustedAppRepository`, `scans: ScanRepository`, `clock: Clock`, `dispatcher: CoroutineDispatcher`. O motor é `WeightedRiskScorer()` criado dentro, tipado como `RiskScorer`.

**Ordem do `invoke(trigger)`:**
1. `startedAt = clock.instant()`.
2. `inventory.installedApps()` (o inventário troca sozinho para o dispatcher de I/O).
3. `trustedApps.trustedPackages()`, uma leitura por varredura, vira `ScoringContext(trustedPackages)`.
4. No `dispatcher`, para cada app na ordem do inventário: `ensureActive()`, os coletores em sequência (união dos sinais), `scorer.score(app, sinais, contexto)`, `ScannedApp(app, risco)`.
5. `finishedAt = maxOf(startedAt, clock.instant())` (decisão 7).
6. `ScanReport(trigger, startedAt, finishedAt, apps)`.
7. `scans.save(report)`: grava e aplica a retenção numa transação.
8. Devolve o relatório.

**`ScanTrigger` e `ScanReport`:** os dois já existem (SPEC-010). O gatilho só passa adiante: vai para o relatório e para `scan_run.trigger` (`name`), e não muda o comportamento. O relatório leva todos os apps, inclusive os confiáveis (verdes) e os `InstalledApp` completos, na ordem do inventário (ordenar é da interface, RF-105). Sem id da varredura: nada nesta spec precisa dele.

**Sequencial, não paralelo:** os nove coletores só leem o `InstalledApp` em memória e não suspendem. Medido no clone: 300 apps × 9 coletores + motor levaram 87 ms na primeira rodada e 41 ms na terceira (JVM). Paralelizar só traria coordenação e ordem não determinística. O custo de I/O fica no inventário. O laço roda no `dispatcher` injetado (`Dispatchers.Default` no app) para não ocupar a thread principal quando o ViewModel chamar.

**Cancelamento:** cooperativo, pela concorrência estruturada de quem chama (ViewModel ou worker). O inventário já confere antes de cada app; o `ScanDevice` chama `ensureActive()` antes de cada app do laço. Cancelado antes da gravação, nada é gravado; cancelado durante a gravação, o `withTransaction` do Room desfaz tudo. A `CancellationException` nunca é engolida.

**Erros:** sem `try`. Erro do inventário (por exemplo, `SecurityException` sem a permissão `NFC`, ADR-021), de um coletor, da leitura dos confiáveis ou da gravação propaga para quem chama, e nada fica gravado. A interface trata o erro (SPEC-030).

**Concorrência entre varreduras:** sem trava. Duas varreduras ao mesmo tempo (abrir o app durante o ciclo da vigia) viram duas transações que o Room serializa; cada uma é inteira.

## Apps confiáveis (regra 2)

O dado já existe: tabela `user_decision` (`packageName`, `decision` = `TRUSTED`, `decidedAt`), com o DAO só de inserir e ler um pacote (SPEC-004). Esta spec cria:

- a porta `TrustedAppRepository` no `core-domain`, só com `trustedPackages()`;
- `UserDecisionDao`: consulta dos pacotes com `decision = 'TRUSTED'`;
- `RoomTrustedAppRepository` no `core-data`, ligado por `@Binds`.

Marcar, listar e desfazer (escrita e tela) ficam na SPEC-033, que amplia a mesma porta. O `ScanDevice` só passa o conjunto ao motor: a regra 2 continua no `WeightedRiskScorer` (só o nível vira verde; o score passa pelas regras 3 e 4, ADR-022). Um app marcado durante a varredura vale na seguinte (critério da SPEC-033).

## Gravação

`RoomScanRepository.save(report)` faz, num `database.withTransaction` (disponível no `room-runtime` 2.8.5, conferido):

| Tabela | Campo | Valor |
| --- | --- | --- |
| `scan_run` | `startedAt`, `finishedAt` | `toEpochMilli()`; `finishedAt` nunca fica nulo, porque a linha só é gravada no fim |
| | `trigger` | `ScanTrigger.name` |
| | `totalApps` | `apps.size` |
| | `redCount`, `yellowCount` | contagem pelo nível final; confiável conta como verde |
| `app_snapshot` (um por app) | `packageName`, `label`, `versionCode` | do `InstalledApp` |
| | `installer` | `installerPackageName` (pode ser nulo) |
| | `firstInstallTime`, `lastUpdateTime` | `toEpochMilli()` |
| | `signals` | `risk.signals.toBitMask()` (SPEC-010; bit = `Signal.bit`) |
| | `score`, `level` | `risk.score`, `risk.level.name` |

Nada mais é gravado: permissões, serviços, serviços de acessibilidade e `defaultPaymentServices` ficam só no relatório. **`lastDefaultPaymentComponent` não é gravado aqui:** o ADR-021 e o plano da SPEC-016 dão a gravação à SPEC-052, que calcula o componente a partir do `ScanReport` devolvido pelo worker. O `ScanDevice` não recebe o `SettingsDataSource`.

## Retenção

- **Onde:** dentro do `RoomScanRepository.save`, na mesma transação, depois dos inserts. Uma consulta nova no `ScanRunDao`: `DELETE FROM scan_run WHERE id NOT IN (SELECT id FROM scan_run ORDER BY id DESC LIMIT :keep)`. Os `app_snapshot` caem pela cascata da FK (ADR-014; `PRAGMA foreign_keys = 1` conferido). A constante 10 fica no `core-data` (política de armazenamento, não de score).
- **Ordem por `id` (decisão 6):** o autoincremento segue a ordem de gravação e não depende do relógio. Por `startedAt`, um relógio atrasado apagaria a varredura recém-gravada. Consequência para a SPEC-030: ler a última varredura também pelo `id` (o `observeAll` de hoje ordena por `startedAt`).
- **Atomicidade:** se um insert falhar, nada entra e a retenção não roda.
- **Índice:** nenhum novo; o `1.json` não muda. `EXPLAIN QUERY PLAN` no clone: a subconsulta por `id` usa a chave primária (por `startedAt` precisaria de uma árvore temporária, irrelevante com 11 linhas); a cascata usa o índice da chave composta (`SEARCH app_snapshot USING COVERING INDEX sqlite_autoindex_app_snapshot_1 (scanId=?)`). A consulta por `packageName` entre varreduras faz `SCAN app_snapshot` (até 3.300 linhas): se a SPEC-050 precisar, o índice entra lá pelo ADR-014.
- **Medido no clone:** 300 snapshots + retenção numa transação: 101 ms na primeira, cerca de 10 ms depois (Robolectric). Depois de 12 gravações, ficaram 10 varreduras (ids 3 a 12) e 3.000 snapshots.
- Alertas por 90 dias: SPEC-053.

## IOException nas preferências

As quatro leituras do `SettingsDataSource` passam por `.catch { if (it is IOException) emit(emptyPreferences()) else throw it }` antes do `map`, como a skill (DataStore) e o comentário da #14 pedem: valem os padrões. As gravações não mudam. Testes com DataStore real, uma leitura só (rodam no Windows), conferidos no clone:

- arquivo com bytes inválidos: `CorruptionException`, que é `IOException`; com o `catch`, volta o padrão;
- diretório no lugar do arquivo: `IOException` ("Inoperable file"); com o `catch`, volta o padrão.

Os testes cobrem as quatro preferências nos dois casos. A divergência registrada no `SKILLS.md` ("sem interface de repositório até a SPEC-018") passa a dizer "até a primeira spec que usar preferências" (SPEC-051 ou SPEC-054).

## Hilt

É a primeira ligação dos coletores. Módulos:

| Módulo do Gradle | Módulo do Hilt | Fornece |
| --- | --- | --- |
| `core-detection` | nenhum | Sem Hilt nem `javax.inject` (SDK, ADR-016) |
| `core-data` | `DataModule` (existe) | Banco, DAOs, DataStore |
| `core-data` | `RepositoryModule` (novo, `abstract`, `@Binds`) | `ScanRepository` → `RoomScanRepository`; `TrustedAppRepository` → `RoomTrustedAppRepository` |
| `app` | `DispatchersModule` + `DispatcherQualifiers.kt` | `@IoDispatcher` = `Dispatchers.IO` (inventário); `@DefaultDispatcher` = `Dispatchers.Default` (laço do `ScanDevice`). Os qualificadores ficam no `app`: só ele os usa (o Room tem executor próprio) |
| `app` | `ClockModule` | `Clock` = `Clock.systemUTC()`, um só para o S6 e o `ScanDevice` |
| `app` | `InventoryModule` | `AppInventory` = `PackageManagerAppInventory(@ApplicationContext, @IoDispatcher)`. Separado para o teste ponta a ponta trocá-lo por `@UninstallModules` |
| `app` | `DetectionModule` | `List<SignalCollector>` com os nove coletores montados; `ScanDevice` = `DefaultScanDevice(...)` |

**Coletores montados, não conjuntos soltos:** um `@Provides` devolve a lista dos nove. Os três conjuntos do S1 são montados ali dentro, com argumentos nomeados, e nunca entram no grafo como `Set<String>`: `InstallSourceCollector(trustedInstallers = TrustedStores.packageNames, knownBankPackages = KnownBanks.apps.map { it.packageName }.toSet(), knownCloneApps = KnownCloneApps.packageNames)`. Os demais: `DefaultPaymentAppCollector(KnownWallets.packageNames)`, `BankLookalikeCollector(KnownBanks.apps)`, `RecentInstallOrUpdateCollector(clock)` e os cinco sem parâmetro. Sem escopo: tudo é barato e sem estado.

**Conferido no clone:** com `List<SignalCollector>` como parâmetro de um `@Provides`, o Kotlin gera `List<? extends SignalCollector>` e o Dagger falha (`MissingBinding`); com `@JvmSuppressWildcards` no ponto de uso, o grafo compila e o teste passa. O `@Provides` do `ScanDevice` tem 6 parâmetros: com `ignoreAnnotated: ['Provides']` no `LongParameterList` de `app/detekt.yml`, o detekt aceita o provider e continua recusando uma função comum de 6 parâmetros.

**Testes do grafo** (como na SPEC-004):
- `core-data/DataModuleTest`: injeta também `ScanRepository` e `TrustedAppRepository`;
- `app/di/DetectionModuleTest` (Robolectric, `HiltTestApplication`): injeta `ScanDevice`, `AppInventory`, a lista, o `Clock` e os dois dispatchers. Confere: um coletor de cada uma das nove classes; `Clock` igual a `Clock.systemUTC()`; dispatchers iguais aos de `Dispatchers`; **os conjuntos do S1 não estão trocados** (cada pacote de `KnownBanks.apps` instalado por cada clonador não tem S1; um app comum instalado por cada clonador tem S1; um app comum da Play não tem; um pacote de banco instalado pelo shell tem); S7 com `KnownWallets` e S9 com `KnownBanks.apps`. É o teste do mapeamento real que a SPEC-019 deixou para cá.
- O `app` ganha `hilt-android-testing`, `kspTest(hilt-compiler)`, `kotlinx-coroutines-test` e `testFixtures(project(":core-domain"))` como dependências de teste; o `core-data` ganha `testFixtures(project(":core-domain"))`.

## Desempenho (300 apps em até 5 s)

- **Teste:** `app/.../ScanDeviceEndToEndTest`, pelo grafo real: `@UninstallModules(InventoryModule::class)` e `@BindValue` com um inventário falso de 300 apps sintéticos; coletores, listas, motor, Room (Robolectric) e dispatchers reais.
- **Nove sinais pelo grafo real (AC-029, pedido na aprovação):** no mesmo `ScanDeviceEndToEndTest`, um inventário falso com um app que dispara os nove sinais; o `ScanReport` do `ScanDevice` injetado traz os nove nesse app (score 100, vermelho). Prova que nenhum coletor falta na lista do `DetectionModule` e que o `ScanDevice` usa essa lista.
- **Apps sintéticos pesados de propósito:** todos os rótulos passam pela comparação de nomes inteira (variantes e distância contra os 29 nomes); um terço sem instalador, metade com NFC e SMS, um quinto com HCE, alguns padrão de pagamento e alguns bancos copiados por clonador.
- **Medição sem instabilidade:** só o `scanDevice(MANUAL)` entra no tempo (`System.nanoTime`), sem a subida do Robolectric e sem aquecimento, como a primeira varredura ao abrir o app. Medido no clone: cerca de 90 ms de coletores e motor e cerca de 100 ms de gravação a frio, perto de 0,2 s contra 5 s (margem de cerca de 25 vezes). Sem nova tentativa: se falhar, é regressão de verdade. O teste também confere que o relatório tem 300 apps.
- **O que o teste não prova:** o custo do `PackageManager` (uma chamada por app), que domina em aparelho. A medição em aparelho intermediário fica com a SPEC-022 (ADR-016 e ADR-021 já a mandam para lá).

## Batches

| Batch | Escopo | Linhas | PR |
| --- | --- | --- | --- |
| BATCH-01 | Gravação e retenção: porta `ScanRepository`; consulta de retenção no `ScanRunDao`; `RoomScanRepository` (mapeamento e transação); `RepositoryModule`; `testFixtures` no `core-data`; testes do repositório, do DAO e do grafo | ~400 (130 + 270 de teste) | Refs #14 |
| BATCH-02 | Apps confiáveis e preferências: porta `TrustedAppRepository`; consulta no `UserDecisionDao`; `RoomTrustedAppRepository` e ligação; `IOException` no `SettingsDataSource`; testes | ~240 (70 + 170) | Refs #14 |
| BATCH-03 | `DefaultScanDevice` no `core-detection` com testes JUnit 4 na JVM (portas falsas, motor real, `CountingDispatcher`, relógio que volta, cancelamento, erros) | ~420 (80 + 340) | Refs #14 |
| BATCH-04 | Hilt no `app` (quatro módulos e qualificadores), isenção do detekt, dependências de teste, teste do grafo, ponta a ponta com desempenho e documentação | ~420 (110 + 310) | Closes #14 |

Total ~1.480; declarado 1.500. **Por que quatro e não três:** a divisão sugerida (persistência e retenção; Hilt; `ScanDevice` e desempenho) põe os dois repositórios e as preferências num batch de cerca de 640 linhas. A ordem também muda: o Hilt vem por último, porque liga o que os três primeiros entregam. Ligar os coletores antes do `ScanDevice` deixaria um grafo que o batch seguinte mudaria de novo, e o teste de desempenho pelo grafo real só existe com tudo ligado. Cada batch compila, passa nos testes e pode ser revisado sozinho.

## Tarefas

1. [x] BATCH-01 — `ScanRepository`; retenção no `ScanRunDao`; `RoomScanRepository`; `RepositoryModule`; testes (mapeamento de todos os campos, máscara escrita à mão, contagens com confiável, relatório vazio, 12 → 10 por `id`, relógio atrasado, menos de 10, cascata, rollback com pacote duplicado); `DataModuleTest`.
2. [ ] BATCH-02 — `TrustedAppRepository`; consulta no `UserDecisionDao`; `RoomTrustedAppRepository`; `catch` nas quatro leituras; testes (TRUSTED e vazio; corrompido e diretório nas quatro preferências); `DataModuleTest`.
3. [ ] BATCH-03 — `DefaultScanDevice` e `DefaultScanDeviceTest` (ordem, união, confiável verde com score 65, campos e relógio, zero apps, dispatcher, cancelamento sem gravação, quatro erros propagados sem gravação).
4. [ ] BATCH-04 — módulos do Hilt do app, `app/detekt.yml`, dependências de teste, `DetectionModuleTest`, `ScanDeviceEndToEndTest`; documentação abaixo; `./gradlew build` e validação do SDD.

## Documentação (BATCH-04, salvo o que cada batch já toca)

- **ADR-023** em `decisions.md`: `ScanDevice` no `core-detection` com duas portas; ordem; sequencial; cancelamento e erros; `finishedAt` limitado; motor criado dentro (ADR-022); retenção por `id` na mesma transação, sem índice; `lastDefaultPaymentComponent` na SPEC-052; Hilt (módulos, qualificadores, `Clock`, coletores montados, `@JvmSuppressWildcards`, isenção dos `@Provides`); desempenho e o que fica para a SPEC-022; alternativas recusadas.
- `SKILLS.md`: Persistence (repositórios, retenção por `id`, divergência do DataStore), Detection (API pública com 16 classes, Hilt feito), Build (isenção do `LongParameterList` para `@Provides`), Clock.
- `project-reference.md` (retenção de alertas → SPEC-053; portas; `ScanDevice`), `context.md`, `architecture.md`, `glossary.md`, `conventions.md`, `backlog_rules.md`.
- `core-data/README.md`, `core-detection/README.md`.
- `docs/especificacao.md`: §7, passo 5 ("comparado com a anterior" pela SPEC-050); §9, retenção "pela ordem de gravação".

## Banco

Nenhuma mudança de schema: sem coluna, sem índice, `1.json` igual. Duas consultas novas nos DAOs (retenção e pacotes confiáveis). `scan_run.finishedAt` continua anulável, mas o `ScanDevice` sempre o preenche.

## Riscos e pontos em aberto

- **Desempenho em aparelho:** o teste não inclui o `PackageManager`. Medição na SPEC-022.
- **Preferências corrompidas:** a leitura volta aos padrões, mas uma gravação ainda falha, porque o `edit` lê o arquivo antes. Um `ReplaceFileCorruptionHandler` (que apaga o arquivo) seria decisão da spec que grava preferências (SPEC-054); fora daqui.
- **SDK:** quem usar o `DefaultScanDevice` implementa as duas portas. Fica no README do `core-detection`.
- **SPEC-030:** ler a última varredura pelo `id`, coerente com a retenção.
- **Testes só no CI:** nenhum previsto. Os testes novos de preferências fazem uma leitura só e rodam no Windows; se algum precisar gravar duas vezes, o PR lista.

## Critérios de aceite (resumo; detalhe em `spec-018-varredura.acceptance.toon`)

- [ ] Ordem, união dos sinais, dispatcher injetado e relatório (AC-001 a AC-003, AC-006, AC-007)
- [ ] Cancelamento e erros sem gravação parcial (AC-004, AC-005, AC-016)
- [ ] Apps confiáveis respeitados (AC-008, AC-009)
- [ ] Gravação de `scan_run` e `app_snapshot`, máscara e contagens (AC-010 a AC-012)
- [ ] Retenção das 10 mais recentes por `id`, atômica, sem índice novo (AC-013 a AC-017)
- [ ] `lastDefaultPaymentComponent` fora (AC-018)
- [ ] `IOException` nas preferências (AC-019)
- [ ] Grafo do Hilt, coletores montados e conjuntos do S1 não trocados (AC-020 a AC-024)
- [ ] Nove sinais no `ScanReport` pelo grafo real (AC-029, `ScanDeviceEndToEndTest`, BATCH-04)
- [ ] 300 apps simulados em até 5 s (AC-025)
- [ ] Limites, documentação e qualidade (AC-026 a AC-028)

## Conferência do BATCH-01 — 2026-11-27

- Tarefas em commits próprios: porta e consulta de retenção (tarefa 1), `RoomScanRepository` e `RepositoryModule` (tarefa 2), testes e plano de testes (tarefa 3), documentação e ADR-023 (tarefa 4).
- Tamanho real: 416 linhas de código e testes (estimativa 407), abaixo de 500.
- `./gradlew build` verde; schema igual; conferência local com cinco mutações, todas derrubando testes e desfeitas sem commit. Detalhes em `spec-018-varredura-tests.md`.
- Nenhuma decisão de comportamento mudou.
