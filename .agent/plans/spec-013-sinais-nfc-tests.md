# Test Plan: SPEC-013 Sinais de NFC (S2, S3)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-013-sinais-nfc.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-013-sinais-nfc.acceptance.toon` (versão 1.0) · 2026-11-19
> São 5 alvos e 22 cenários (3 + 5 + 1 + 5 + 8), um método de teste por cenário.
> - O parametrizado das permissões relacionadas roda 3 casos.
> - Os 8 cenários do inventário rodam nos cinco níveis de API.
> - Execuções novas ou alteradas: 3 no `core-domain` (alteradas), 13 nos coletores e 40 no inventário (8 × 5; o cenário 8 já existia e ganha uma asserção).

## Scope
- **Modelo:** `InstalledApp.cardEmulationServices` é lido de volta, copiado e recusa alteração por cast, como `requestedPermissions` e `services` (AC-015).
- **Coletor de S2:**
  - `NfcPermissionCollector` gera S2 só com `android.permission.NFC`, com comparação exata;
  - as permissões relacionadas não geram S2;
  - nunca devolve outro sinal (AC-001 a AC-004, AC-008).
- **Coletor de S3:**
  - `CardEmulationServiceCollector` gera S3 quando `cardEmulationServices` não está vazio;
  - um serviço comum em `services` não gera S3;
  - não depende de S2 e nunca devolve outro sinal (AC-005 a AC-008).
- **Inventário:** consulta de HCE no `PackageManager` do Robolectric, nos cinco níveis de API (AC-009 a AC-014). Cobre:
  - filtro e permissão;
  - off-host e NFC-F;
  - desligados;
  - critério mais largo que o do sistema;
  - isolamento por pacote e app do sistema.
- **Onde roda:**
  - `core-domain`: JUnit 5 (Kover);
  - coletores: JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`;
  - inventário: Robolectric com o `@Config(sdk = [26, 28, 30, 33, 36])` da classe.
  - Tudo roda no `./gradlew build`.
- **Fora (contrato, `excluded`):**
  - `MATCH_DISABLED_COMPONENTS` em aparelho real (SPEC-022);
  - erro do sistema na consulta (sem `try`, propaga; revisão);
  - corrida entre a lista e a consulta;
  - ausência de `import android.*` e de Hilt nos coletores (inspeção, AC-016).
  - AC-017 e AC-018 são conferidos pelo build e pelo `git status`.

## Padrões seguidos (pattern analysis)
- **`InstallSourceCollectorTest`, `…TrustedStoreTest` e `…UntrustedInstallerTest` (SPEC-012):**
  - `runTest` em função `suspend`;
  - asserções do JUnit 4;
  - constantes num `private companion object`;
  - parametrizado com o parâmetro no construtor e `@JvmStatic @Parameterized.Parameters(name = "{0}")`;
  - lista literal no teste.
- **`PackageManagerAppInventoryTest` (SPEC-011):** `install(info, installer)`, `aPackageInfo(...)`, `aService(...)`, pacotes `com.example.a…`/`b…`/`c…` em ordem alfabética e `@Config` com cinco níveis na classe.
- **`InstalledAppTest` (SPEC-010):** JUnit 5, laço sobre as coleções expostas com cast para `MutableSet`.
- `skills/android/testing.md` › Adapter Tests — Robolectric (gaps conhecidos do shadow) e Naming Convention for Tests (frase com crases).

## Unit tests

### Modelo — `core-domain/src/test/.../inventory/InstalledAppTest.kt` (JUnit 5, alterado)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | fields-are-read-back-including-card-emulation-services | `cardEmulationServices` igual ao passado | AC-015 |
| 2 | mutating-source-collections-does-not-change-the-model | limpar o `MutableSet` de origem não muda o campo | AC-015 |
| 3 | exposed-collections-reject-mutation-through-cast | `add`, `remove` e `clear` por cast lançam `UnsupportedOperationException` | AC-015 |

### Coletor de S2 — `core-detection/.../signal/NfcPermissionCollectorTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | nfc-permission-yields-s2 | `NFC` com `RECEIVE_SMS` → `{NFC_PERMISSION}` | AC-001 |
| 2 | no-permissions-yields-nothing | conjunto vazio | AC-002 |
| 3 | unrelated-permission-yields-nothing | só `RECEIVE_SMS` → conjunto vazio | AC-002 |
| 4 | nfc-permission-in-other-case-yields-nothing | `ANDROID.PERMISSION.NFC` e `android.permission.nfc` → conjunto vazio | AC-004 |
| 5 | yields-only-s2-for-app-with-hce-and-null-installer | exatamente `{NFC_PERMISSION}` | AC-008 |

### Permissões relacionadas — `core-detection/.../signal/NfcPermissionCollectorRelatedPermissionTest.kt` (parametrizado, 3 casos)
| # | Scenario | Parameters | Expected result | AC |
|---|----------|------------|-----------------|----|
| 1 | related-permission-yields-nothing | `NFC_TRANSACTION_EVENT`, `NFC_PREFERRED_PAYMENT_INFO`, `BIND_NFC_SERVICE` | conjunto vazio | AC-003 |

### Coletor de S3 — `core-detection/.../signal/CardEmulationServiceCollectorTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | hce-service-yields-s3 | `{CARD_EMULATION_SERVICE}` | AC-005 |
| 2 | no-services-yields-nothing | conjunto vazio | AC-006 |
| 3 | ordinary-service-without-hce-yields-nothing | `services` com `com.example.PaymentService` e `cardEmulationServices` vazio → conjunto vazio | AC-006 |
| 4 | hce-without-nfc-permission-yields-s3 | `{CARD_EMULATION_SERVICE}` | AC-007 |
| 5 | yields-only-s3-for-app-with-nfc-and-null-installer | exatamente `{CARD_EMULATION_SERVICE}` | AC-008 |

## Integration tests (Robolectric, APIs 26, 28, 30, 33 e 36)

### Inventário — `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt` (alterado)
| # | Scenario | Setup | Expected result | AC |
|---|----------|-------|-----------------|----|
| 1 | fills-card-emulation-services-with-hce-service-and-keeps-it-in-services | serviço com a ação de HCE e `BIND_NFC_SERVICE` | `cardEmulationServices` = {serviço}; `services` contém o serviço | AC-009 |
| 2 | skips-hce-filter-without-permission-and-permission-without-filter | um com o filtro e sem permissão; outro com a permissão e sem filtro | `cardEmulationServices` vazio; os dois em `services` | AC-010 |
| 3 | skips-off-host-and-nfcf-services | ação `OFF_HOST_APDU_SERVICE` e ação `HOST_NFCF_SERVICE`, os dois com a permissão | `cardEmulationServices` vazio | AC-011 |
| 4 | includes-disabled-hce-services | um com `enabled = false`; outro desligado por `setComponentEnabledSetting` | os dois em `cardEmulationServices` | AC-012 |
| 5 | includes-hce-service-of-disabled-app | app com `applicationInfo.enabled = false` e serviço de HCE | o serviço em `cardEmulationServices` | AC-012 |
| 6 | includes-hce-service-not-exported-without-meta-data-in-app-without-nfc | `exported = false`, sem meta-data, sem permissão `NFC` no app | o serviço em `cardEmulationServices` | AC-013 |
| 7 | keeps-hce-services-per-package-and-ignores-system-app | `USER_APP` com HCE, `OTHER_APP` sem, app do sistema com HCE | só `USER_APP` com o serviço; `OTHER_APP` vazio; app do sistema ausente | AC-014 |
| 8 | returns-empty-sets-without-permissions-or-services (existente) | app sem nada | também `cardEmulationServices` vazio | AC-014 |

## Edge cases
- Mesmo pacote com HCE e com serviço sem a permissão: coberto pelos cenários 1 e 2 juntos, em apps separados para ficar claro qual falha.
- Comparação exata no S2 (cenário 4 do S2).
- S3 sem S2 (cenário 4 do S3).

## Test data & fixtures
- **`anInstalledApp(requestedPermissions = …, services = …, cardEmulationServices = …, installerPackageName = …)`**, do `testFixtures` do `core-domain`.
- **`installService(service, action)`**, novo no `PackageManagerAppInventoryTest`: `shadow.addOrUpdateService(service)` e, se `action` não for nulo, `shadow.addIntentFilterForService(ComponentName, IntentFilter(action))`. É chamado depois de `install(...)`. O KDoc explica por que o serviço não vai no `PackageInfo` do `installPackage`: ali ele não entra na resolução de intents (experimento do plano).
- **`aService(...)`** ganha `permission: String? = null` e `exported: Boolean = true`.
- **Constantes:**
  - ações escritas à mão (`android.nfc.cardemulation.action.HOST_APDU_SERVICE`, `…OFF_HOST_APDU_SERVICE`, `…HOST_NFCF_SERVICE`);
  - `android.permission.BIND_NFC_SERVICE`.
  - O teste não lê as constantes do framework usadas na produção: trocar a ação na produção derruba o teste.
- Pacotes sintéticos (`com.example…`).

## Infrastructure
- Nenhuma biblioteca nova: `junit4`, `robolectric`, `androidx.test:core`, `kotlinx-coroutines-test` e `testFixtures(project(":core-domain"))` já estão no `core-detection`; JUnit 5 no `core-domain`.
