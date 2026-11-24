# Architecture: SPEC-016 Pagamento padrão (S7)

> Entrada: `.agent/plans/spec-016-pagamento-padrao.md`, `.agent/specs/features/spec-016-pagamento-padrao.spec.toon` (FT-016 v1.1), `.agent/specs/acceptance/spec-016-pagamento-padrao.acceptance.toon` (v1.1)
> Contrato: `.agent/specs/contracts/spec-016-pagamento-padrao.contract.toon` (v1.1) · 2026-11-24

> **Emenda 1.1 do contrato (tarefa 1, 2026-11-24):** com `paymentCandidates` como função própria, o detekt acusou `TooManyFunctions` no `PackageManagerAppInventory` (11 funções; o limite é 11). Os candidatos passaram para dentro de `defaultPaymentServicesByPackage`, e a segunda consulta continua fora do `try`. O parâmetro se chama `userServices`: com o nome longo, a assinatura numa linha passaria de 120 colunas (detekt), e o ktlint recusa a forma de várias linhas. O `LargeClass` não acusou o `PackageManagerAppInventoryTest`, então o plano B aprovado (ajudantes num arquivo de suporte e `PackageManagerAppInventoryDefaultPaymentTest`) não foi preciso.

## Summary
- **Modelo:** o `InstalledApp` ganha `defaultPaymentServices: Set<String>`.
- **Inventário:** o `PackageManagerAppInventory` passa a cinco etapas. A nova, entre a leitura da acessibilidade e o detalhe, faz três coisas:
  1. uma segunda consulta de HCE sem `MATCH_DISABLED_COMPONENTS`;
  2. cruza o resultado com `exported` e com o `cardEmulationServices` dos apps de usuário;
  3. pergunta `CardEmulation.isDefaultServiceForCategory(cn, CATEGORY_PAYMENT)` por candidato.
- **Coletor:** um público novo, Kotlin puro, `DefaultPaymentAppCollector(knownWallets: Set<String>)`, devolve `{UNKNOWN_DEFAULT_PAYMENT_APP}` quando o campo não está vazio e o pacote não está no conjunto.
- **Shadows:** três shadows do Robolectric, só nos testes, cobrem os erros e o papel de carteira.

| Pacote / arquivo | Conteúdo | Visibilidade |
|---|---|---|
| `core.domain.inventory` / `InstalledApp.kt` | campo `defaultPaymentServices` depois de `enabledAccessibilityServices` | público (como os demais campos) |
| `core.domain` testFixtures / `DomainFixtures.kt` | `anInstalledApp` com `defaultPaymentServices` (padrão vazio) | público no `testFixtures` |
| `core.detection.inventory` / `PackageManagerAppInventory.kt` | etapa do pagamento padrão | a classe continua pública, com o mesmo construtor; o resto é `private` |
| `core.detection.signal` / `DefaultPaymentAppCollector.kt` | `class DefaultPaymentAppCollector(knownWallets: Set<String>) : SignalCollector` | público |
| `core.detection.inventory` (test) / `CardEmulationShadows.kt` | `DeadNfcServiceCardEmulationShadow`, `MissingNfcPermissionCardEmulationShadow`, `WalletRoleCardEmulationShadow` | só no source set de teste |

**API pública do `core-detection` depois desta spec (catorze classes):** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores`, `NfcPermissionCollector`, `CardEmulationServiceCollector`, `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector`, `EnabledAccessibilityServiceCollector`, `KnownWallets`, `KnownBankApp`, `KnownBanks`, `BankLookalikeCollector` e **`DefaultPaymentAppCollector`**.
- Nenhum `internal` novo: `NfcAdapter`, `CardEmulation` e a segunda consulta não têm ramo por versão novo (a consulta reusa o `queryIntentServicesCompat`), então nada vai para `PackageManagerVersionCompat.kt`.
- Nenhum `@Suppress` nem `@SuppressLint` novo.

## Pattern Analysis
- **Padrões existentes:**
  1. `InstallSourceCollector(trustedInstallers)` (SPEC-012): coletor com conjunto obrigatório, copiado com `Collections.unmodifiableSet(LinkedHashSet(...))`, comparação exata;
  2. `BankLookalikeCollector(knownBankApps)` (SPEC-015): mesmo formato, com a lista padrão num `object` (`KnownBanks`);
  3. `EnabledAccessibilityServiceCollector` e `enabledAccessibilityServicesByPackage()` (SPEC-014): campo `Set<String>` preenchido por uma leitura por varredura no inventário, coletor que só lê o campo;
  4. `cardEmulationServicesByPackage()` (SPEC-013): consulta de HCE agrupada por pacote; `catch (expected: PackageManager.NameNotFoundException)` em `readApp` (nome `expected`).
- **Padrão seguido:** 3 e 4 para o inventário e o campo; 1 e 2 para o coletor.

## Revisão do plano (architect)

| Ponto | Resultado |
|---|---|
| **Nome do campo** | `defaultPaymentServices` (o mesmo da decisão do usuário). Diz o que a fonte entrega: os serviços que o sistema diz serem o padrão de pagamento, no plural porque na API 35+ o dono do papel pode ter vários. `defaultPaymentCardEmulationServices` repetiria o que o KDoc já diz. `isDefaultPaymentApp` (`Boolean`) foi recusado pelo usuário. Guarda nomes de classe completos (`String`), como `cardEmulationServices`; `ComponentName` levaria uma classe do Android para o `core-domain` |
| **Posição e obrigatoriedade** | Depois de `enabledAccessibilityServices`, obrigatório e sem padrão. A fábrica passa a 11 parâmetros; o `@Suppress("LongParameterList")` dela continua dentro da exceção do ADR-015. A fixture passa a 10 parâmetros, dentro da exceção do ADR-019. O KDoc promete **subconjunto de `cardEmulationServices`**, e o inventário garante isso pela interseção |
| **Nome do coletor** | `DefaultPaymentAppCollector`, e não `UnknownDefaultPaymentAppCollector` (sugestão do plano). Segue o S1: o coletor que recebe uma lista de isenção tem o nome do dado observado (`InstallSourceCollector`, sinal `UNTRUSTED_INSTALL_SOURCE`), porque "desconhecido" depende da lista que quem usa o SDK passa. Os outros coletores sem lista usam o nome do traço (`CardEmulationService`, `BankLookalike`) |
| **Parâmetro** | `knownWallets: Set<String>`, como `trustedInstallers` e `knownBankApps`. Obrigatório, sem padrão, copiado. Lista padrão `KnownWallets.packageNames`, citada no KDoc. O conjunto vazio faz todo app padrão receber S7, documentado como no S1 |
| **O que fica privado** | No inventário: `hceServices(flags)`, `defaultPaymentServicesByPackage(...)` (com os candidatos dentro dela, emenda 1.1), `askDefaultPaymentServices(...)` e `cardEmulationOrNull()`. No coletor: o conjunto copiado e a resposta no `companion` |
| **Duas consultas, não uma** | A primeira continua com `MATCH_DISABLED_COMPONENTS` (critério largo do ADR-018). A segunda vai sem a flag, para o `PackageManager` aplicar o estado atual. As duas usam `hceServices(flags)`, que faz a consulta, tira `serviceInfo` e filtra `BIND_NFC_SERVICE`. `ServiceInfo.enabled` não é usado: é o valor do manifesto (plano, emenda 1.1) |
| **Como os dados passam entre as etapas** | Valores locais em `installedApps()`: `cardEmulationServices` (mapa de todos os pacotes, como hoje); `userCardEmulationServices` (só os pacotes de usuário com serviço, montado a partir de `userPackages`); `defaultPaymentServices` (mapa pacote → classes). Nada novo em campos da classe além do `Context` da aplicação |
| **Ordem dentro da etapa** | 1) `userCardEmulationServices` vazio → `emptyMap()` (não toca no NFC); 2) `cardEmulationOrNull()` nulo → `emptyMap()` (sem a segunda consulta); 3) segunda consulta e candidatos; 4) perguntas, dentro do `try`. A segunda consulta fica **fora** do `try`: erro do `PackageManager` propaga, como na SPEC-011 |
| **Erros** | `cardEmulationOrNull()`: `NfcAdapter.getDefaultAdapter(applicationContext)` nulo → `null`; `CardEmulation.getInstance` dentro de `try` com `catch (expected: UnsupportedOperationException)` → `null`. `askDefaultPaymentServices`: `catch (expected: RuntimeException)`, relança quando `expected.cause !is RemoteException` (a `SecurityException` sai por aí), senão `emptyMap()`. Formas conferidas no detekt (plano, experimento 10) |
| **`ReturnCount` do detekt (máximo 2)** | Por isso a etapa fica em duas funções: `defaultPaymentServicesByPackage` decide com uma expressão (`if`/`when`) e no máximo dois `return`; `askDefaultPaymentServices` é um `try` como expressão |
| **Constantes** | Produção usa as do Android (`CardEmulation.CATEGORY_PAYMENT`, `HostApduService.SERVICE_INTERFACE`, `Manifest.permission.BIND_NFC_SERVICE`), como o inventário de hoje. Os testes escrevem `"payment"` como literal (`PAYMENT_CATEGORY`), como fazem com a chave de acessibilidade: trocar a categoria na produção derruba os testes |
| **Etapas e KDoc** | O KDoc da classe passa a cinco etapas e diz: só ligados e exportados são consultados; sem NFC ou sem HCE e serviço NFC morto → campo vazio; `SecurityException` sem a permissão `NFC` propaga; quem usa o SDK declara `android.permission.NFC` |
| **Cancelamento e dispatcher** | A etapa nova roda antes do laço, dentro do `withContext(ioDispatcher)`. O `ensureActive()` continua antes de cada app. O teste de cancelamento conta leituras do rótulo; a etapa não as toca, e os testes atuais não ligam as features de NFC (etapa termina no passo 1 ou 2) |
| **Lint** | `isDefaultServiceForCategory` e `getDefaultAdapter` não têm `@RequiresPermission` nas tags lidas. A segunda consulta usa o `queryIntentServicesCompat`, que já tem o `@SuppressLint("QueryPermissionsNeeded")` |
| **Shadows** | Em `CardEmulationShadows.kt`, no pacote de teste `core.detection.inventory`, um por cenário. São públicos (sem modificador), com `@Implements(CardEmulation::class)` e um `@Implementation fun isDefaultServiceForCategory(service: ComponentName, category: String): Boolean` que usa os dois parâmetros (`UnusedParameter` do detekt). Entram por `@Config(shadows = [...])` no método de teste e não vazam para o teste seguinte (conferido) |
| **Prova de "não consultado"** | O `ShadowCardEmulation` responde `true` para o componente definido como padrão, mesmo desligado. Definir o serviço desligado como padrão e ver o campo vazio prova que o inventário não perguntou. Não precisa de shadow que grave as perguntas |
| **`LargeClass` (600)** | O `PackageManagerAppInventoryTest` tem cerca de 400 linhas de código; com os cenários novos, cerca de 545. Os shadows ficam fora da classe. Ajudantes novos curtos (`enableCardEmulation`, `installHceService`, `setDefaultPaymentService`) evitam repetição |
| **Estimativa** | **435** linhas pelos arquivos do contrato (tabela abaixo), contra 360 no plano aprovado. Sobe pela emenda 1.1 (segunda consulta, dois cenários) e pelo arquivo dos shadows. Abaixo de 500: um PR. A spec já está em 1.1 com `estimate-lines: 435` |

### Estimativa pelos arquivos do contrato (código e testes)

| Arquivo | Ação | Linhas |
|---|---|---|
| `core-domain/.../inventory/InstalledApp.kt` | modify | 8 |
| `core-domain/src/testFixtures/.../DomainFixtures.kt` | modify | 2 |
| `core-detection/.../inventory/PackageManagerAppInventory.kt` | modify | 60 |
| `core-detection/.../signal/DefaultPaymentAppCollector.kt` | create | 45 |
| **Subtotal código** | | **115** |
| `core-domain/src/test/.../inventory/InstalledAppTest.kt` | modify | 15 |
| `core-detection/src/test/.../inventory/CardEmulationShadows.kt` | create | 55 |
| `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt` | modify | 165 |
| `core-detection/src/test/.../signal/DefaultPaymentAppCollectorTest.kt` | create | 85 |
| **Subtotal testes** | | **320** |
| **Total** | | **435** |

Base: o PR do S4 (SPEC-014 BATCH-02) teve 216 linhas com campo, etapa no inventário (42), coletor (31), teste do inventário (68), teste do coletor (57), `InstalledApp` (11), `InstalledAppTest` (15) e fixture (2). O S7 tem uma etapa maior (duas consultas, três erros), coletor com parâmetro, o dobro de cenários no inventário e o arquivo dos shadows.

## Dependencies
- **Reusa:** `SignalCollector`, `Signal.UNKNOWN_DEFAULT_PAYMENT_APP`, `InstalledApp` e `unmodifiableCopy()` (core-domain); `KnownWallets.packageNames` como lista padrão (só no KDoc e no README); `queryIntentServicesCompat` e `cardEmulationServicesByPackage` do inventário; os ajudantes `install`, `installService`, `aPackageInfo` e `aService` do `PackageManagerAppInventoryTest`; `anInstalledApp`; `ShadowCardEmulation` e `ShadowNfcAdapter` do Robolectric 4.18 (já em `testImplementation`).
- **Novas:** nenhuma. `android.nfc` faz parte do SDK; nenhuma biblioteca nova.

## Database Changes
Nenhuma. O S7 vai no bit 6 de `signals` pela SPEC-018; `lastDefaultPaymentComponent` (SPEC-004) é da SPEC-052.

## Test Strategy Rationale
- **Coletor:** JUnit 4 sem Robolectric, porque não usa API do Android. A lista é escrita à mão no teste (`com.wallet.one`, `com.wallet.two`), não `KnownWallets.packageNames`, para que mexer na lista de produção não mude o teste.
- **Inventário:** Robolectric nas cinco APIs da classe (`@Config(sdk = [26, 28, 30, 33, 36])`). Um cenário por critério do aceite (AC-007 a AC-015). Os resultados do Robolectric são iguais nas cinco APIs. A diferença real entre versões é do sistema NFC e fica nas fontes do AOSP e na SPEC-022.
- **Modelo:** JUnit 5 no `core-domain`, cópia e imutabilidade do campo novo, como nos campos anteriores.
- **Fora dos testes:** o que o Robolectric não simula (registro do serviço, papel de carteira de verdade, permissão `NFC`), o desempenho e o formato para a SPEC-052, que não tem código aqui.

## Trade-offs & Alternatives Considered
- **`ServiceInfo.isEnabled()` da primeira consulta** (pedido inicial): rejeitado pelo usuário depois da pesquisa. `enabled` é o valor do manifesto; um serviço ligado em tempo de execução escaparia do S7.
- **`getComponentEnabledSetting` por serviço:** N chamadas e repetiria a regra do `PackageManager` (inclusive `DISABLED_UNTIL_USED`). Rejeitado.
- **Pôr os candidatos no `InstalledApp`** (por exemplo, `enabledCardEmulationServices`): campo novo sem sinal que o use. Rejeitado; ficam em valores locais.
- **Pôr a etapa depois do detalhe** para filtrar também pela permissão `NFC` do pacote: mudaria a ordem do inventário e o laço por app. Fica fora (o custo que sobra é raro).
- **Um shadow que grava as perguntas** para provar "não consultado": desnecessário com o `ShadowCardEmulation` respondendo `true` a desligados.
- **`throw RuntimeException(RemoteException(...))` no shadow:** recusado pelo detekt (`TooGenericExceptionThrown`). `RemoteException(...).rethrowAsRuntimeException()` passa e é o que o framework faz.
- **Testes do S7 numa classe nova** (para fugir do `LargeClass`): duplicaria os ajudantes de instalação. Fica na classe atual, com a margem acima.

## Risks
- **`LargeClass`:** se o detekt acusar o `PackageManagerAppInventoryTest`, o builder para e pergunta (não suprime).
- **Lint ou detekt na forma da captura:** conferidos no projeto descartável; se o build acusar algo novo, o builder para e pergunta.
- **Robolectric diferente do aparelho:** conferência na SPEC-022 (Android 8, 12, 14 e a mais recente).
- **Custo que sobra:** serviço ligado e exportado com meta-data inválida ou de pacote sem `NFC` ainda faz o sistema NFC refazer o cache. Raro; medido na SPEC-022.
