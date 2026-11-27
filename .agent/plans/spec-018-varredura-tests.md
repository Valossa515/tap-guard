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
