# Test Plan: SPEC-018 — Caso de uso de varredura

Segue o contrato FT-018 v1.0 (`test-strategy`). Uma seção por batch, registrada antes de escrever os testes do batch.

## BATCH-01 — Gravação e retenção

### Escopo

`ScanRunDao.deleteOlderThanLatest` (consulta de retenção), `RoomScanRepository` (mapeamento, contagens, transação com a retenção) e a ligação `ScanRepository` no grafo do `core-data`. A porta `ScanRepository` é interface e fica fora (contrato, `excluded`).

### Infraestrutura

- Robolectric `@Config(sdk = [36])`, Room em memória (`inMemoryDatabaseBuilder` com `allowMainThreadQueries`), `runTest`, JUnit 4, como `ScanRunDaoTest` e `AppSnapshotDaoTest`.
- `anInstalledApp` e `aRiskResult` do `testFixtures` do `core-domain` (`testImplementation(testFixtures(project(":core-domain")))` no `core-data`).
- Contagem de linhas por SQL direto (`openHelper.writableDatabase.query`), sem DAO novo só para teste (skill de testes, Room).
- Dados escritos à mão: datas fixas (`Instant.parse`), máscaras `7` (S1 S2 S3) e `263` (S1 S2 S3 S9) como literais, retenção `10` como literal. Os testes não chamam `toBitMask()` nem leem a constante de produção.

### Integração — `ScanRunDaoTest` (cenários novos)

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | 12 varreduras gravadas; `deleteOlderThanLatest(10)` | Ficam os ids 3 a 12 | AC-013 |
| 2 | 10 varreduras e uma 11ª com `startedAt` menor que todas; `deleteOlderThanLatest(10)` | A 11ª fica; sai a de menor id | AC-014 |
| 3 | 3 varreduras; `deleteOlderThanLatest(10)` | Nenhuma apagada | AC-015 |
| 4 | Varredura com snapshots apagada pela retenção | Os snapshots dela somem; os das que ficam continuam | AC-013 |
| 5 | `keep` 0 e -1 (correção do review) | `IllegalArgumentException` com a mensagem fixa; nenhuma varredura apagada | AC-013 |

### Integração — `RoomScanRepositoryTest`

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | Relatório `PERIODIC` com datas e apps vermelho, amarelo e verde | `scan_run` com `startedAt`/`finishedAt` em ms, `trigger` `PERIODIC`, `totalApps` 3, `redCount` 1, `yellowCount` 1 | AC-010 |
| 2 | App sem instalador com S1 S2 S3 e app com instalador e S1 S2 S3 S9 | Um `app_snapshot` por app com todos os campos; `signals` 7 e 263; `score` e `level` pelo nome | AC-011 |
| 3 | Confiável verde com score 65, um vermelho e dois amarelos | `redCount` 1 e `yellowCount` 2 | AC-012 |
| 4 | Confiável verde com score 65 | Snapshot com `score` 65 e `level` `GREEN` | AC-012 |
| 5 | Relatório sem apps | `scan_run` com `totalApps`, `redCount` e `yellowCount` 0, nenhum snapshot | AC-007 |
| 6 | 11 gravações pelo `save` | 10 varreduras; a primeira e os snapshots dela somem | AC-013 |
| 7 | 10 gravações e um relatório com pacote repetido | `SQLiteConstraintException`; ids iguais aos de antes (a mais antiga continua, logo a retenção não rodou); nenhum snapshot novo | AC-016 |

### Grafo — `DataModuleTest`

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | Injetar `ScanRepository` | Instância de `RoomScanRepository` | AC-020 |

### Conferência local (desfeita sem commit)

Depois da suíte verde, uma alteração por vez na produção; cada uma deve derrubar pelo menos um teste:

1. retenção por `startedAt` no lugar de `id`;
2. escritas fora do `withTransaction`;
3. `RETAINED_SCANS = 11`;
4. retenção antes dos inserts;
5. `RED` e `YELLOW` trocados nas contagens.

Restaurar os bytes originais e rodar a suíte de novo.

### Resultados do BATCH-01 — 2026-11-27

- **Testes novos:** 12, todos passaram: 4 no `ScanRunDaoTest` (agora 7), 7 no `RoomScanRepositoryTest` e 1 no `DataModuleTest` (agora 3). Nenhum ignorado.
- **Build completo:** `./gradlew build` passou em 1min35s (535 tarefas, 93 executadas): ktlint, detekt, Android lint, testes e Kover. Relatórios: core-domain 75, core-detection 406, core-data 29 (2 ignorados no Windows), core-nfc 1, app 32, decoy-app 1. São 544 casos, 542 aprovados, 2 ignorados e 0 falhas.
- **Ignorados no Windows (rodam no CI):** os dois já existentes do `SettingsDataSourceTest`, `written values are read back` e `writing null removes last default payment component`. Nenhum teste novo é ignorado.
- **Schema:** `core-data/schemas` sem diferença depois do build.
- Não executado em aparelho nem no CI remoto (sem push).

### Conferência local do BATCH-01 (todas desfeitas sem commit)

Cada mutação foi aplicada sozinha na produção; a suíte do repositório, do DAO e do grafo rodou, e os bytes originais foram restaurados (`git diff` vazio no fim).

| Mutação | Testes que falharam |
| --- | --- |
| Retenção por `startedAt` no lugar de `id` | `delete older than latest keeps the newest run even with the oldest start time`; `eleventh save keeps the ten most recent scans` |
| Escritas fora do `withTransaction` | `failed save rolls back the scan and does not apply retention` |
| `RETAINED_SCANS = 11` | `eleventh save keeps the ten most recent scans` |
| Retenção antes dos inserts | `eleventh save keeps the ten most recent scans` |
| `RED` e `YELLOW` trocados nas contagens | `counts follow the final level and a trusted app counts as green` |

### Correção do review — 2026-11-27

- `ScanRunDao.deleteOlderThanLatest` recusa `keep` menor que 1 (`require`, mensagem fixa). Teste novo no `ScanRunDaoTest` (agora 8): `keep` 0 e -1 lançam `IllegalArgumentException` com a mensagem escrita no teste, e as três varreduras continuam.
- Conferência: tirar o `require` derruba o teste novo (com `LIMIT 0`, as três varreduras seriam apagadas). Desfeita sem commit.
- Testes novos do BATCH-01: 13.

## BATCH-02 — Apps confiáveis e preferências

### Escopo

`UserDecisionDao.getTrustedPackageNames` e `RoomTrustedAppRepository` (leitura dos apps confiáveis), a ligação `TrustedAppRepository` no grafo do `core-data` e o tratamento de `IOException` nas quatro leituras do `SettingsDataSource`. A porta `TrustedAppRepository` é interface e fica fora (contrato, `excluded`). Segue o contrato 1.2.

### Infraestrutura

- **Confiáveis:** Robolectric `@Config(sdk = [36])`, Room em memória e `runTest`, como no BATCH-01. As linhas entram pelo `UserDecisionDao.insert` que já existe. A decisão é escrita como literal (`"TRUSTED"`, `"trusted"`).
- **Preferências:** JUnit 4 puro com DataStore real (`PreferenceDataStoreFactory.create(scope = backgroundScope)`), como o `SettingsDataSourceTest` existente. Sem fingir o store. Cada cenário só lê, sem gravar: roda também no Windows.
  - **Corrompido:** bytes que não são protobuf gravados no arquivo antes de criar o store.
  - **Pasta:** um diretório criado no caminho do arquivo.
  - **Outra exceção:** o produtor do arquivo lança `IllegalStateException("sem arquivo")`.
- Padrões escritos à mão: `false`, `true`, `6`, `null`.

### Integração — `RoomTrustedAppRepositoryTest`

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | `user_decision` com dois pacotes `TRUSTED` e um com `trusted` em minúsculas | `trustedPackages()` devolve só os dois `TRUSTED` | AC-009 |
| 2 | Tabela vazia | Conjunto vazio | AC-009 |

### Integração — `SettingsDataSourceTest` (cenários novos)

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | Arquivo com bytes inválidos (`CorruptionException`) | As quatro leituras devolvem os padrões, sem exceção | AC-019 |
| 2 | Diretório no lugar do arquivo (`IOException`) | As quatro leituras devolvem os padrões, sem exceção | AC-019 |
| 3 | Produtor do arquivo lança `IllegalStateException` | As quatro leituras propagam a `IllegalStateException` | AC-019 |

### Grafo — `DataModuleTest`

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | Injetar `TrustedAppRepository` | Instância de `RoomTrustedAppRepository` | AC-020 |

### Conferência local (desfeita sem commit)

Uma alteração por vez na produção. O script apaga os resultados XML antes de cada rodada, roda o Gradle de verdade (`./gradlew` no Git Bash) e só lê os XML gerados naquela rodada. Há também uma rodada de controle, sem mutação, que tem de passar com resultados novos. Cada mutação deve derrubar pelo menos um teste:

1. sem o `catch` da `IOException` (leituras direto do `dataStore.data`);
2. `catch` de qualquer exceção (emite vazio para tudo);
3. consulta dos confiáveis sem `WHERE decision = 'TRUSTED'`;
4. consulta com comparação sem distinguir maiúsculas (`decision = 'TRUSTED' COLLATE NOCASE`);
5. `RoomTrustedAppRepository.trustedPackages()` devolvendo sempre um conjunto vazio.

### Resultados do BATCH-02 — 2026-11-27

- **Testes novos:** 6, todos passaram, nenhum ignorado: 2 no `RoomTrustedAppRepositoryTest`, 3 no `SettingsDataSourceTest` (agora 6) e 1 no `DataModuleTest` (agora 4). Os três de preferências só leem e rodam no Windows.
- **Build completo:** `./gradlew build` passou em 1min38s (535 tarefas, 83 executadas): ktlint, detekt, Android lint, testes e Kover. Relatórios: core-domain 75, core-detection 406, core-data 36 (2 ignorados no Windows), core-nfc 1, app 32, decoy-app 1. São 551 casos, 549 aprovados, 2 ignorados e 0 falhas.
- **Ignorados no Windows (rodam no CI):** os dois que já existiam no `SettingsDataSourceTest`, `written values are read back` e `writing null removes last default payment component`.
- **Schema:** `core-data/schemas` sem diferença.
- Não executado em aparelho nem no CI remoto (sem push).

### Conferência local do BATCH-02 (todas desfeitas sem commit)

**O Gradle rodou de verdade a cada alteração.** O script (Git Bash) fazia, a cada rodada:
1. aplicar a mutação e mostrar o diff dela (`+1/-1` no arquivo esperado);
2. apagar `core-data/build/test-results/testDebugUnitTest/*.xml`;
3. criar um marcador de tempo e rodar `./gradlew :core-data:testDebugUnitTest` com os três testes do batch;
4. conferir no log que a tarefa `:core-data:testDebugUnitTest` executou e que os 3 XML existiam e eram mais novos que o marcador.

A rodada de controle, sem mutação, passou (exit 0, tarefa executada, 3 XML novos). No fim, `git status` limpo e `git diff` vazio.

| Mutação | Exit | Testes que falharam |
| --- | --- | --- |
| Controle (sem mutação) | 0 | nenhum |
| Sem o `catch` da `IOException` | 1 | `corrupted file reads the four defaults`; `directory in place of the file reads the four defaults` |
| `catch` de qualquer exceção | 1 | `exceptions other than IOException are rethrown by the four reads` |
| Consulta sem `WHERE decision = 'TRUSTED'` | 1 | `returns only packages whose decision is exactly TRUSTED` |
| Decisão com `COLLATE NOCASE` | 1 | `returns only packages whose decision is exactly TRUSTED` |
| Repositório devolve sempre vazio | 1 | `returns only packages whose decision is exactly TRUSTED` |

Cada rodada com mutação teve 12 testes (2 ignorados) e só as falhas acima.

## BATCH-03 — DefaultScanDevice

### Escopo

`DefaultScanDevice` (`core-detection`, `scan`): ordem das etapas, coletores em sequência, dispatcher injetado, cancelamento, erros, relógio que volta, apps confiáveis e o relatório. Segue o contrato 1.2 (`DefaultScanDeviceTest`, 10 cenários).

### Infraestrutura

- JUnit 4 na JVM, sem Robolectric (o `DefaultScanDevice` não usa API do Android), `runTest` e `anInstalledApp` do `testFixtures`. Motor real (`WeightedRiskScorer`, criado dentro do `DefaultScanDevice`).
- **Falsos escritos no teste, sem MockK** (o módulo não tem MockK), todos gravando numa lista de eventos em ordem:
  - inventário com lista fixa, ou que lança;
  - repositório de confiáveis com conjunto fixo, ou que lança;
  - repositório de varreduras que guarda os relatórios, ou que lança;
  - coletor que devolve sinais por pacote e pode lançar, cancelar ou suspender (`yield()`) num pacote.
- **Relógio:** um `Clock` que devolve instantes de uma lista, em ordem, para o relógio que avança e para o que volta.
- **Dispatcher:** `StandardTestDispatcher(testScheduler)`. No cenário do dispatcher, embrulhado num `CountingDispatcher` (padrão do `PackageManagerAppInventoryTest`).
- **Cancelamento como o de quem chama:** a varredura roda num `launch`, e o coletor, ao ver o segundo app, chama `cancel()` nesse job e **volta normalmente**, sem lançar. Assim só o `ensureActive()` do laço impede o terceiro app. Um coletor que lançasse `CancellationException` faria a exceção sair mesmo sem o `ensureActive`, e o teste não pegaria a falta dele.
- **Exceção de teste própria** (`ScanTestException`), conferida por identidade.

### Unitários — `DefaultScanDeviceTest`

| # | Cenário | Esperado | Aceite |
| --- | --- | --- | --- |
| 1 | Dois apps e dois coletores, o primeiro suspendendo (`yield()`) | Eventos: inventário, confiáveis, coletor 1 (início e fim) e coletor 2 do primeiro app, os mesmos do segundo, gravação. Um coletor só começa depois que o anterior termina, e a gravação vem uma vez, no fim | AC-001 |
| 2 | O relatório gravado e o devolvido | O mesmo objeto, gravado uma vez | AC-001 |
| 3 | Coletores com sinais distintos e repetidos | `signals` é a união (S1 S2 S3), score 65, vermelho | AC-002 |
| 4 | `CountingDispatcher` | Contagem maior que zero | AC-003 |
| 5 | Cancelamento no segundo de três apps | Job cancelado; nenhum coletor no terceiro app; gravação não chamada | AC-004 |
| 6 | Inventário, coletor (no segundo app), confiáveis ou gravação lançam | A mesma exceção chega a quem chama; nos três primeiros casos a gravação não é chamada | AC-005 |
| 7 | Relógio que avança e relógio que volta | Início e fim vêm do relógio; com o relógio que volta, o fim é igual ao início, sem exceção | AC-006 |
| 8 | Inventário vazio | Relatório vazio, gravado uma vez | AC-007 |
| 9 | Dois apps com S1 S2 S3 e um deles confiável | Confiável verde com score 65; o outro vermelho com 65 | AC-008 |
| 10 | App com `defaultPaymentServices` | O relatório traz o mesmo `InstalledApp`, com o campo preservado | AC-018 |

### Conferência local (desfeita sem commit)

Mesmo cuidado do BATCH-02: a cada rodada, aplicar a mutação e mostrar o diff, apagar os XML de resultado, criar um marcador de tempo, rodar `./gradlew :core-detection:testDebugUnitTest` com o `DefaultScanDeviceTest` e conferir que a tarefa executou e que o XML é mais novo que o marcador. Uma rodada de controle, sem mutação, tem de passar. Cada mutação deve derrubar pelo menos um teste:

1. gravar antes de terminar todos os apps (um relatório parcial gravado depois de cada app);
2. ignorar os confiáveis (`ScoringContext(emptySet())`);
3. tirar o `ensureActive()`;
4. fim sem a correção do relógio (`finishedAt = clock.instant()`);
5. coletores em paralelo (`async` por coletor, com `awaitAll`).
