# Architecture: SPEC-014 Sinais de controle (S4, S5, S6, S8)

> Entrada: `.agent/plans/spec-014-sinais-controle.md`, `.agent/specs/features/spec-014-sinais-controle.spec.toon` (FT-014 v1.1), `.agent/specs/acceptance/spec-014-sinais-controle.acceptance.toon` (v1.0)
> Contrato: `.agent/specs/contracts/spec-014-sinais-controle.contract.toon` (v1.0) · 2026-11-20

## Summary
Quatro coletores públicos, Kotlin puro, ficam em `core.detection.signal`:
- `OverlayPermissionCollector()`: `{OVERLAY_PERMISSION}` com `"android.permission.SYSTEM_ALERT_WINDOW"` em `requestedPermissions`;
- `SmsPermissionCollector()`: `{SMS_PERMISSION}` com `"android.permission.READ_SMS"` ou `"android.permission.RECEIVE_SMS"`;
- `RecentInstallOrUpdateCollector(clock: Clock)`: `{RECENTLY_INSTALLED_OR_UPDATED}` quando `maxOf(firstInstallTime, lastUpdateTime)` não é anterior a `clock.instant() - 7 dias`;
- `EnabledAccessibilityServiceCollector()`: `{ACCESSIBILITY_SERVICE_ENABLED}` quando `enabledAccessibilityServices` não está vazio.

O `InstalledApp` ganha `enabledAccessibilityServices: Set<String>`. O `PackageManagerAppInventory` o preenche com uma leitura de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` por varredura, separada por `:` e por `ComponentName.unflattenFromString`, e agrupada por pacote.

| Pacote / arquivo | Conteúdo | Visibilidade | Batch |
|---|---|---|---|
| `core.detection.signal` / `OverlayPermissionCollector.kt` | `class OverlayPermissionCollector : SignalCollector` | público | 01 |
| `core.detection.signal` / `SmsPermissionCollector.kt` | `class SmsPermissionCollector : SignalCollector` | público | 01 |
| `core.detection.signal` / `RecentInstallOrUpdateCollector.kt` | `class RecentInstallOrUpdateCollector(private val clock: Clock) : SignalCollector`; janela num `companion` privado | público (a janela é privada) | 01 |
| `core.domain` testFixtures / `DomainFixtures.kt` | `anInstalledApp` com `firstInstallTime` e `lastUpdateTime` (01) e `enabledAccessibilityServices` (02) | público no `testFixtures` | 01 e 02 |
| `core.domain.inventory` / `InstalledApp.kt` | campo `enabledAccessibilityServices` depois de `cardEmulationServices` | público (como os demais campos) | 02 |
| `core.detection.inventory` / `PackageManagerAppInventory.kt` | leitura e separação da chave | a classe continua pública; o resto é `private` | 02 |
| `core.detection.signal` / `EnabledAccessibilityServiceCollector.kt` | `class EnabledAccessibilityServiceCollector : SignalCollector` | público | 02 |

**API pública do `core-detection` depois desta spec:** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores`, `NfcPermissionCollector`, `CardEmulationServiceCollector`, `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector` e `EnabledAccessibilityServiceCollector`. Nenhum tipo do Android nos novos. Nenhum `internal` novo: `Settings.Secure.getString` não tem ramo por versão, então não vai para `PackageManagerVersionCompat.kt`.

## Revisão do plano (architect)

| Ponto | Resultado |
|---|---|
| **Nome do campo** | `enabledAccessibilityServices`. É o nome da chave (`ENABLED_ACCESSIBILITY_SERVICES`) e o do `Signal.ACCESSIBILITY_SERVICE_ENABLED`, e diz o que a fonte entrega: o que o usuário ligou, inclusive um serviço que caiu. `activeAccessibilityServices` prometeria serviço rodando, o que o `Settings.Secure` não garante. `accessibilityServices` pareceria a lista de declarados, ao lado de `services`. Guarda nomes de classe completos (`String`), como `cardEmulationServices`; `ComponentName` levaria uma classe do Android para o `core-domain` |
| **Posição e obrigatoriedade** | Depois de `cardEmulationServices`, obrigatório e sem padrão. A fixture tem padrão vazio. A fábrica passa a 10 parâmetros; o `@Suppress("LongParameterList")` continua dentro da exceção do ADR-015 (fábrica que espelha o construtor) |
| **Subconjunto de `services`?** | Não é garantido nem validado (BR-011). O KDoc não promete subconjunto |
| **Nomes dos coletores** | Seguem a forma curta dos existentes (`InstallSource`, `NfcPermission`, `CardEmulationService`). `RecentInstallOrUpdateCollector`: `RecentInstallCollector` esconderia a atualização, e `RecentlyInstalledOrUpdatedCollector` destoa dos outros. `EnabledAccessibilityServiceCollector`: casa com o campo, como `CardEmulationServiceCollector` com `cardEmulationServices`. `AccessibilityServiceCollector` esconderia que serviço só declarado não conta |
| **O que fica privado** | A janela de 7 dias (`Duration.ofDays(7)` no `companion`), as listas de permissões (literais em `const val`) e a separação da chave (`private fun` no inventário). A janela só vira parâmetro se a SPEC-017 decidir calibrá-la junto dos pesos |
| **`Clock`** | `java.time.Clock` no construtor, obrigatório. `java.time` existe desde a API 26, o minSdk, sem desugaring. `class`, não `object`, como os demais. A SPEC-018 liga `Clock.systemUTC()` pelo Hilt (`skills/android/SKILL.md`). A comparação é entre `Instant`s, sem fuso |
| **Etapas do inventário** | Passam a quatro: lista leve, consulta de HCE, leitura da chave de acessibilidade, detalhe de cada app. A leitura fica dentro do `withContext(ioDispatcher)`, antes do laço. O KDoc da classe, a linha "Inventário" da §8 e o `SKILLS.md` mudam de "três" para "quatro etapas" |
| **Nome `PackageManagerAppInventory`** | Fica, embora a classe leia também o `Settings`. Renomear quebraria a API pública sem ganho. O construtor não muda (o `ContentResolver` vem do `Context` que ela já recebe), então a SPEC-018 não é afetada |
| **Cancelamento** | A leitura vem antes do laço; o `ensureActive()` continua antes de cada app. O teste de cancelamento conta leituras do rótulo, que a leitura da chave não toca: continua `[2, 1, 1]` |
| **Erros** | Sem `try` novo. A `SecurityException` propaga. A falha do provedor chega como `null` (o framework pega a `RemoteException`), e isso corrige o motivo 2 do ADR-019 (ver o plano) |
| **Lint** | `Settings.Secure.getString` não tem checagem de lint conhecida. Nenhuma supressão nova prevista. O build confere (AC-022) |
| **Documentação por batch** | O rascunho punha "Docs e ADR-019" no BATCH-02. A DoD pede a documentação do módulo em cada PR, então o BATCH-01 leva a dos seus coletores e cria o ADR-019 com todas as decisões (S4 marcado para o BATCH-02). O BATCH-02 muda a §8 e conclui o ADR-019 |
| **Branch do BATCH-02** | Lacuna nas regras: "uma branch por spec" e "um PR por batch". **Decidido pelo usuário:** `spec-014-sinais-controle-batch-02`, a partir da `main` depois do merge do BATCH-01 |
| **Estimativa** | 624 linhas pelos arquivos do contrato (tabela abaixo), contra 600 no plano. **Emenda 1.1:** `estimate-lines: 625`, BATCH-01 395 e BATCH-02 230. Os dois abaixo de 500 |

### Estimativa pelos arquivos do contrato (código e testes)

| Arquivo | Ação | Batch | Linhas |
|---|---|---|---|
| `core-detection/.../signal/OverlayPermissionCollector.kt` | create | 01 | 30 |
| `core-detection/.../signal/SmsPermissionCollector.kt` | create | 01 | 36 |
| `core-detection/.../signal/RecentInstallOrUpdateCollector.kt` | create | 01 | 42 |
| `core-domain/src/testFixtures/.../DomainFixtures.kt` | modify | 01 | 4 |
| `core-detection/src/test/.../signal/OverlayPermissionCollectorTest.kt` | create | 01 | 60 |
| `core-detection/src/test/.../signal/SmsPermissionCollectorTest.kt` | create | 01 | 75 |
| `core-detection/src/test/.../signal/SmsPermissionCollectorUnrelatedPermissionTest.kt` | create | 01 | 36 |
| `core-detection/src/test/.../signal/RecentInstallOrUpdateCollectorTest.kt` | create | 01 | 110 |
| **Subtotal BATCH-01** | | | **393** |
| `core-domain/.../inventory/InstalledApp.kt` | modify | 02 | 7 |
| `core-domain/src/testFixtures/.../DomainFixtures.kt` | modify | 02 | 2 |
| `core-domain/src/test/.../inventory/InstalledAppTest.kt` | modify | 02 | 10 |
| `core-detection/.../inventory/PackageManagerAppInventory.kt` | modify | 02 | 30 |
| `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt` | modify | 02 | 100 |
| `core-detection/.../signal/EnabledAccessibilityServiceCollector.kt` | create | 02 | 30 |
| `core-detection/src/test/.../signal/EnabledAccessibilityServiceCollectorTest.kt` | create | 02 | 52 |
| **Subtotal BATCH-02** | | | **231** |
| **Total** | | | **624** |

- **Base da conta:** os arquivos da SPEC-013. `NfcPermissionCollector` tem 32 linhas e o teste dele 62 (5 cenários). Os cenários de HCE no `PackageManagerAppInventoryTest` somaram 133 (7 cenários e o ajudante de registro).
- **Testes:** cerca de 72% das linhas (449).
- **BATCH-01 (+33 sobre o plano):** o S6 tem 9 cenários com relógio fixo e o S8 tem 7.
- **BATCH-02 (−9):** a separação da chave cabe numa cadeia de 8 linhas, e o teste não precisa do ajudante de reflexão.
- A documentação não entra na conta (regra do SDD 1.8.0).

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Coletor Kotlin puro em `core.detection.signal`:** `InstallSourceCollector` (SPEC-012), `NfcPermissionCollector` e `CardEmulationServiceCollector` (SPEC-013). `class` pública, `override suspend fun collect`, `companion` privado com o conjunto de resposta (`DETECTED`), KDoc com o que conta e o que não conta, literais do Android em `const val`.
  2. **Campo do `InstalledApp` preenchido por uma consulta por varredura e agrupado por pacote:** `cardEmulationServices` (SPEC-013, `cardEmulationServicesByPackage()`), cruzado com a lista leve.
  3. **Teste parametrizado com lista literal das permissões que não contam:** `NfcPermissionCollectorRelatedPermissionTest` (SPEC-013).
  4. **Robolectric nos cinco níveis da classe:** `PackageManagerAppInventoryTest` (`@Config(sdk = [26, 28, 30, 33, 36])`).
  5. **Relógio injetado:** `skills/android/SKILL.md` (`provideClock()`) e `testing.md` ("inject a `Clock`"). É o primeiro uso no código.
- **Padrão seguido:** 1 nos quatro coletores, 2 no S4, 3 no S8 e 4 no inventário. O 5 entra no S6 como parâmetro de construtor, sem Hilt.

## Layer Decisions

| Camada | Decisão | Justificativa |
|---|---|---|
| `core-domain` (modelo) | Campo novo no `InstalledApp`, cópia com `unmodifiableCopy()` | Mesmo formato de `cardEmulationServices` (ADR-015, ADR-018) |
| `core-domain` (testFixtures) | Três parâmetros novos na `anInstalledApp`, padrões iguais aos valores de hoje | Os testes do S6 trocam as datas; os do S4, o campo novo |
| `core-detection` (inventário) | Uma leitura do `Settings.Secure` por varredura, `private fun enabledAccessibilityServicesByPackage()` | Mesmo formato da consulta de HCE; os coletores continuam puros |
| `core-detection` (coletores) | Quatro classes, uma por sinal | Opção A; entradas diferentes; o SDK pode usar só algumas (ADR-017, ADR-018) |

## Dependencies
- **Reusing:** `SignalCollector`, `Signal`, `InstalledApp`, `unmodifiableCopy` (SPEC-010); `PackageManagerAppInventory` e o seu teste (SPEC-011 e SPEC-013); `anInstalledApp` (SPEC-012).
- **Framework:** `android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` e `android.content.ComponentName.unflattenFromString` (API 1 e 4), só no inventário; `java.time.Clock`, `Duration` e `Instant` (API 26) no coletor de S6.
- **New:** nenhuma biblioteca.

## Database Changes
Nenhuma. `app_snapshot` não guarda serviços. Os sinais vão nos bits 3, 4, 5 e 7 de `signals` pela SPEC-018. O `1.json` não muda (ADR-014).

## Test Strategy Rationale
- **Coletores em JUnit 4 sem Robolectric:** não tocam o Android; o S6 recebe `Clock.fixed(NOW, ZoneOffset.UTC)`.
- **Inventário no Robolectric, cinco níveis:** `Settings.Secure.putString` funcionou nas cinco APIs no experimento, e a chave volta a `null` em cada teste. Os cenários de S4 entram no `PackageManagerAppInventoryTest` existente, com um ajudante `enableAccessibilityServices(value: String?)`.
- **Modelo em JUnit 5:** os três testes do `InstalledAppTest` ganham o campo novo.
- **Fora dos testes automáticos:** falha do provedor (o shadow não a simula; o framework devolve `null`); o `@Readable` (o Robolectric não o aplica; fontes e SPEC-022); imports dos coletores (inspeção no review).

## Trade-offs & Alternatives Considered
- **`AccessibilityManager.getEnabledAccessibilityServiceList`** (a §8 antiga): rejeitado pelo usuário. Só devolve os serviços conectados, e no Robolectric exige reflexão (`setComponentName`).
- **Separação com `TextUtils.SimpleStringSplitter`**, como o sistema: o `split(':')` do Kotlin com filtro de vazias tem o mesmo efeito e não depende de outra classe do Android.
- **Janela como parâmetro do construtor do S6:** rejeitado por ora; a §5 fixa 7 dias, e calibrar é da SPEC-017.
- **Cruzar o S4 com `services`:** rejeitado no plano (BR-011), pelo critério mais largo do ADR-018. Confirmado pelo usuário.
- **Ler a chave dentro do coletor:** é a opção C, rejeitada pelo usuário (N leituras ou cache, e `android.*` no coletor).

## Risks
- **Falha do provedor de configurações:** S4 some nessa varredura, sem erro. A API não dá como distinguir.
- **Relógio atrasado:** S6 em todos os apps; somado, pode mudar o nível (exemplos no plano e no ADR-019).
- **Robolectric não aplica o `@Readable`:** um teste não pega se a chave deixar de ser legível. As fontes cobrem a API 26 à 36; a SPEC-022 confere no aparelho.
- **Branch do BATCH-02:** as regras não diziam. Decidido: `spec-014-sinais-controle-batch-02`, criada a partir da `main` depois do merge do BATCH-01.
