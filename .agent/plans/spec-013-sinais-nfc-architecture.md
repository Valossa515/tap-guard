# Architecture: SPEC-013 Sinais de NFC (S2, S3)

> Entrada: `.agent/plans/spec-013-sinais-nfc.md`, `.agent/specs/features/spec-013-sinais-nfc.spec.toon` (FT-013 v1.1), `.agent/specs/acceptance/spec-013-sinais-nfc.acceptance.toon` (v1.0)
> Contrato: `.agent/specs/contracts/spec-013-sinais-nfc.contract.toon` (v1.0) · 2026-11-19

## Summary
O `InstalledApp` ganha `cardEmulationServices: Set<String>`, com os nomes de classe dos serviços de HCE do app. O `PackageManagerAppInventory` o preenche com uma consulta `queryIntentServicesCompat` por varredura: ação `HostApduService.SERVICE_INTERFACE`, flag `MATCH_DISABLED_COMPONENTS`, e só os serviços com `permission == Manifest.permission.BIND_NFC_SERVICE`, agrupados por pacote.

Dois coletores públicos, Kotlin puro, ficam em `core.detection.signal`:
- `NfcPermissionCollector` devolve `{NFC_PERMISSION}` quando `requestedPermissions` contém `"android.permission.NFC"`;
- `CardEmulationServiceCollector` devolve `{CARD_EMULATION_SERVICE}` quando `cardEmulationServices` não está vazio.

| Pacote / arquivo | Conteúdo | Visibilidade |
|---|---|---|
| `core.domain.inventory` / `InstalledApp.kt` | campo `cardEmulationServices: Set<String>` depois de `services`, no construtor e na fábrica | público (como os demais campos) |
| `core.detection` / `PackageManagerVersionCompat.kt` | `queryIntentServicesCompat(intent, flags)`, com ramo na API 33 | `internal` |
| `core.detection.inventory` / `PackageManagerAppInventory.kt` | consulta de HCE e preenchimento do campo | a classe continua pública; o resto é `private` |
| `core.detection.signal` / `NfcPermissionCollector.kt` | `class NfcPermissionCollector : SignalCollector` | público |
| `core.detection.signal` / `CardEmulationServiceCollector.kt` | `class CardEmulationServiceCollector : SignalCollector` | público |

**API pública do `core-detection` depois desta spec:** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores`, `NfcPermissionCollector` e `CardEmulationServiceCollector`. Nenhum tipo do Android nos novos. As permissões que não contam para o S2 não viram lista pública: o SDK não tem o que ampliar ali.

## Revisão do plano (architect)

| Ponto | Resultado |
|---|---|
| **Nome do campo** | `cardEmulationServices`. É o mesmo termo do `Signal.CARD_EMULATION_SERVICE` e do S3 na §5 ("serviço de emulação de cartão"). `hostApduServices` levaria o nome de uma classe do Android para o `core-domain`, e `hceServices` é sigla, contra a convenção de nomes por extenso. O KDoc define o critério: só HCE no próprio aparelho (`HOST_APDU_SERVICE` + `BIND_NFC_SERVICE`), inclusive desligados; off-host e NFC-F fora. **Emenda 1.1 na spec:** a entrada "servicos de HCE" passa a se chamar `cardEmulationServices` |
| **Posição e obrigatoriedade** | Depois de `services`, obrigatório e sem valor padrão, como os outros campos da fábrica. A fixture tem padrão vazio. O `@Suppress("LongParameterList")` da fábrica continua dentro da exceção do ADR-015 (fábrica que espelha o construtor) |
| **Subconjunto de `services`?** | Não é garantido nem validado. Os dois vêm de chamadas diferentes, e um app atualizado entre elas pode divergir. O KDoc não promete subconjunto |
| **Constantes no coletor** | O coletor de S2 usa o literal `"android.permission.NFC"` numa `const val` privada, não `android.Manifest.permission.NFC`: a AC-016 proíbe `import android.*` nos coletores. O inventário, que já é Android, usa `HostApduService.SERVICE_INTERFACE` e `Manifest.permission.BIND_NFC_SERVICE` |
| **`class` ou `object` nos coletores** | `class` sem parâmetros, a mesma forma do `InstallSourceCollector`. Um `object` seria uma segunda forma para o mesmo papel, e a SPEC-018 vai ligar os coletores no Hilt da mesma maneira |
| **Lint** | O experimento mostrou `QueryPermissionsNeeded` nos dois ramos de `queryIntentServices`. `@SuppressLint("QueryPermissionsNeeded")` em `queryIntentServicesCompat`, com comentário, igual a `installedPackagesCompat`. O ADR-016 citava só uma supressão; o ADR-018 e o `SKILLS.md` passam a citar as duas |
| **Etapas do inventário** | O KDoc da classe, a linha "Inventário" da §8 e o `SKILLS.md` diziam "duas etapas". Passam a dizer: lista leve, uma consulta de HCE por varredura, e o detalhe de cada app |
| **Cancelamento** | A consulta vem antes do laço. O `ensureActive()` continua antes de cada app. O teste de cancelamento conta leituras do rótulo, que a consulta não toca: continua `[2, 1, 1]` |
| **Documentação** | Lista do plano conferida. Acrescentado o README do `core-detection` com o `<queries>` da ação de HCE para quem usa o SDK sem `QUERY_ALL_PACKAGES` |
| **Estimativa** | 419 linhas pelos arquivos do contrato (tabela abaixo), contra 410 no plano. **Emenda 1.1:** `estimate-lines: 420`. Abaixo de 500: um PR só, sem batches e sem `single-pr-reason` |

### Estimativa pelos arquivos do contrato (código e testes)

| Arquivo | Ação | Linhas |
|---|---|---|
| `core-domain/.../inventory/InstalledApp.kt` | modify | 7 |
| `core-domain/src/testFixtures/.../DomainFixtures.kt` | modify | 2 |
| `core-domain/src/test/.../inventory/InstalledAppTest.kt` | modify | 10 |
| `core-detection/.../PackageManagerVersionCompat.kt` | modify | 17 |
| `core-detection/.../inventory/PackageManagerAppInventory.kt` | modify | 28 |
| `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt` | modify | 150 |
| `core-detection/.../signal/NfcPermissionCollector.kt` | create | 28 |
| `core-detection/.../signal/CardEmulationServiceCollector.kt` | create | 30 |
| `core-detection/src/test/.../signal/NfcPermissionCollectorTest.kt` | create | 60 |
| `core-detection/src/test/.../signal/NfcPermissionCollectorRelatedPermissionTest.kt` | create | 32 |
| `core-detection/src/test/.../signal/CardEmulationServiceCollectorTest.kt` | create | 55 |
| **Total** | | **419** |

Os testes são cerca de 73% das linhas (307). Para comparar, a SPEC-012 teve 277 linhas para um coletor, com 181 de teste. A documentação não entra na conta (regra do SDD 1.8.0: código e testes).

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Coletor Kotlin puro no `core.detection.signal`:** `InstallSourceCollector` (SPEC-012). Construtor público, `override suspend fun collect`, `companion` privado com o conjunto de resposta (`UNTRUSTED`), KDoc com o que gera o sinal e as limitações.
  2. **Ramo de versão num arquivo `internal` de extensões:** `installedPackagesCompat` e `packageInfoCompat` (`PackageInfoFlags` na 33) em `PackageManagerVersionCompat.kt`, com `@SuppressLint` no próprio elemento (ADR-016).
  3. **Coleção do modelo copiada com `unmodifiableCopy()` na fábrica:** `requestedPermissions` e `services` do `InstalledApp`, com testes de cópia e de recusa por cast (ADR-015).
  4. **Constantes de flags no `companion` do inventário:** `SYSTEM_FLAGS` e `DETAIL_FLAGS`.
  5. **Teste parametrizado com lista literal no teste, uma classe por grupo:** `InstallSourceCollectorTrustedStoreTest` e `InstallSourceCollectorUntrustedInstallerTest`.
- **Padrão seguido:** os cinco acima, mais `skills/android/testing.md` › Adapter Tests — Robolectric (cinco níveis de API).

## Layer Decisions

| Camada | Decisão | Justificativa |
|---|---|---|
| `core-domain` / modelo | `val cardEmulationServices: Set<String>` depois de `services`, no construtor privado e na fábrica `invoke`, com `cardEmulationServices.unmodifiableCopy()`. KDoc da classe: o que entra (HCE no próprio aparelho, inclusive desligados) e o que não entra (off-host, NFC-F) | BR-010. Mesmo idioma das outras coleções (ADR-015) |
| `core-domain` / fixture | `anInstalledApp(..., cardEmulationServices: Set<String> = emptySet())`, depois de `services` | BR-010. A fixture expõe só o que os testes trocam |
| `core-detection` / compat | `@SuppressLint("QueryPermissionsNeeded") internal fun PackageManager.queryIntentServicesCompat(intent: Intent, flags: Int): List<ResolveInfo>`: `queryIntentServices(intent, ResolveInfoFlags.of(flags.toLong()))` a partir da `TIRAMISU`, e `queryIntentServices(intent, flags)` abaixo. KDoc: a lista respeita a visibilidade de pacotes; a permissão fica com quem usa a biblioteca | BR-004, BR-011. Mesmo corte e mesma supressão do `installedPackagesCompat`. O ramo antigo não é depreciado, então não há `@Suppress("DEPRECATION")` |
| `core-detection` / inventário | Em `installedApps()`, dentro do `withContext(ioDispatcher)`: (1) lista leve filtrada, como hoje; (2) `val hce = cardEmulationServicesByPackage()`; (3) `readApp(packageName, hce[packageName].orEmpty())` com o `ensureActive()` de antes. `cardEmulationServicesByPackage()` privado: `queryIntentServicesCompat(Intent(HostApduService.SERVICE_INTERFACE), PackageManager.MATCH_DISABLED_COMPONENTS)`, `mapNotNull { it.serviceInfo }`, filtro `permission == Manifest.permission.BIND_NFC_SERVICE`, agrupado por `packageName` em `Set` de `name`. KDoc da classe com as três etapas e o critério do HCE (ADR-018) | BR-004, BR-005, BR-007, BR-008, BR-011. Uma chamada por varredura. Pacotes da consulta fora da lista (sistema, o próprio app, instalados no meio) ficam de fora pelo `hce[packageName]`. Erros do sistema na consulta não são capturados: propagam, como os demais |
| `core.detection.signal` / S2 | `class NfcPermissionCollector : SignalCollector`. `override suspend fun collect(app) = if (NFC_PERMISSION in app.requestedPermissions) DETECTED else emptySet()`. No `companion` privado: `const val NFC_PERMISSION = "android.permission.NFC"` e `val DETECTED = setOf(Signal.NFC_PERMISSION)`. KDoc: comparação exata; o que não conta e por quê (BR-002); não chama o `PackageManager` | BR-001, BR-002, BR-009 |
| `core.detection.signal` / S3 | `class CardEmulationServiceCollector : SignalCollector`. `override suspend fun collect(app) = if (app.cardEmulationServices.isNotEmpty()) DETECTED else emptySet()`, com `DETECTED = setOf(Signal.CARD_EMULATION_SERVICE)` no `companion` privado. KDoc: o critério vem do inventário (HCE no próprio aparelho com `BIND_NFC_SERVICE`, inclusive desligados); mais largo que o do sistema de propósito (sem exigir exportação, meta-data ou `NFC`); não depende de S2 (ADR-018) | BR-003, BR-006, BR-007, BR-009 |

## Dependencies
- **Reusing:**
  - `SignalCollector`, `Signal`, `InstalledApp` e `unmodifiableCopy()` (`core-domain`, SPEC-010);
  - `PackageManagerAppInventory` e `PackageManagerVersionCompat.kt` (SPEC-011);
  - `anInstalledApp` em `testFixtures` (SPEC-012);
  - `junit4`, `robolectric`, `androidx.test:core` e `kotlinx-coroutines-test` (já no `core-detection`).
- **New:** nenhuma. `HostApduService` e `Manifest.permission.BIND_NFC_SERVICE` são do framework (API 19).

## Database Changes
Nenhuma. `app_snapshot` não tem coluna de serviços. S2 e S3 vão nos bits 1 e 2 de `signals` pela SPEC-018. O `1.json` não muda (conferido pela AC-017 e pelo passo "Schema do Room commitado" do CI).

## Test Strategy Rationale
- **Inventário no Robolectric (cinco níveis):** a consulta tem ramo na API 33 e depende do shadow do `PackageManager`. Os serviços entram com um helper novo, `installService(service, action)`: `shadow.addOrUpdateService(service)` e depois `shadow.addIntentFilterForService(ComponentName, IntentFilter(action))`, ou sem filtro quando `action` é nulo. O helper é chamado depois de `install(...)`, que registra o pacote e o instalador. O KDoc do helper explica por que não basta pôr o serviço no `PackageInfo` (experimento do plano). Se o Robolectric mudar, os testes falham em vez de passar.
- **`aService` ganha `permission: String? = null` e `exported: Boolean = true`:** só o que os testes trocam.
- **Coletores em JUnit 4 puro:** não tocam em API do Android, como o do S1 (ADR-017). O grupo das permissões relacionadas é uma classe parametrizada com uma lista literal no teste. Os outros cenários ficam numa classe por coletor.
- **Modelo em JUnit 5:** os três testes do `InstalledAppTest` passam a cobrir o campo novo: leitura, cópia e recusa por cast.

## Trade-offs & Alternatives Considered
- Permissão `BIND_NFC_SERVICE` só pelo `getPackageInfo` (opção A): rejeitada pelo usuário. Conta off-host e NFC-F e não confere o filtro de intent.
- Meta-data com `GET_META_DATA` (opção A'): rejeitada. Não confere o filtro e aumenta a resposta de cada app.
- Consulta por app com `setPackage` (B'): rejeitada. N chamadas.
- Mudar `services` para pares nome + permissão: rejeitada. O filtro de intent não vem no `ServiceInfo`, e o coletor teria de repetir a regra do inventário.
- Um coletor para S2 e S3: rejeitada pelo usuário (comentário na #9).
- Lista pública das permissões que não contam: rejeitada. Não é uma lista que o SDK amplia; fica no KDoc e no teste.
- Validar que `cardEmulationServices` é subconjunto de `services`: rejeitada. As fontes são chamadas diferentes, e uma falha no modelo derrubaria a varredura.

## Risks
- **`MATCH_DISABLED_COMPONENTS` na consulta em aparelho real:** a documentação só o descreve para `PackageInfo`. Mitigação: conferir na SPEC-022 com um serviço de HCE desligado no app isca. Fica registrado no ADR-018.
- **App instalado entre a lista e a consulta:** fica sem S3 nessa varredura; a próxima corrige.
- **Mecanismo do Robolectric** (`addComponent` só cria a entrada de filtros para componente novo): documentado no helper e no `SKILLS.md`.
