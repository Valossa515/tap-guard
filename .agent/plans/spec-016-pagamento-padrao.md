# Plan: SPEC-016 Pagamento padrão (S7)

> Issue #12 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-013 (#9, fechada) e SPEC-015 (#11, fechada) · Especificação RF-102, §5 (S7, lista de carteiras) e §8 (linha S7) · ADR-016, ADR-018, ADR-019, ADR-020
> Spec: `.agent/specs/features/spec-016-pagamento-padrao.spec.toon` (FT-016 v1.1, `interface: library`, `estimate-lines: 435`, um PR) · Aceite: `.agent/specs/acceptance/spec-016-pagamento-padrao.acceptance.toon` (v1.1)
> Arquitetura: `.agent/plans/spec-016-pagamento-padrao-architecture.md` · Contrato: `.agent/specs/contracts/spec-016-pagamento-padrao.contract.toon`
> Branch: `spec-016-pagamento-padrao` (a partir da `main` em `f91ac3e`) · Planejado em 2026-11-24, aprovado no mesmo dia com dois acréscimos (emenda 1.1) · Um PR, com `Closes #12`

## Summary
Sinal S7 ("é o app de pagamento padrão e não está na lista de carteiras conhecidas"), no formato do S3 e do S4:
- **Inventário:** uma etapa nova por varredura. Uma segunda consulta de HCE, **sem** `MATCH_DISABLED_COMPONENTS`, diz quais serviços estão ligados. Para cada um que também está exportado e em `cardEmulationServices` de um app de usuário, o inventário pergunta ao sistema `CardEmulation.isDefaultServiceForCategory(ComponentName(pacote, classe), CATEGORY_PAYMENT)`. Os que voltam `true` vão para o campo novo `InstalledApp.defaultPaymentServices`.
- **Coletor:** `DefaultPaymentAppCollector(knownWallets)`, com o conjunto obrigatório e copiado, como o S1 e o S9. Devolve `{UNKNOWN_DEFAULT_PAYMENT_APP}` quando o campo não está vazio e o pacote não está no conjunto. Não chama o Android.

A chave `Settings.Secure` `nfc_payment_default_component`, que o S4 levaria a imitar, **não serve**: da API 31 em diante um app comum recebe `SecurityException` ao lê-la (pesquisa abaixo).

## Scope
- **Módulos afetados:**
  - `core-domain`: campo novo no `InstalledApp`, teste do modelo e parâmetro novo na fixture `anInstalledApp`;
  - `core-detection`: etapa nova no inventário, coletor de S7, shadows de teste e testes;
  - documentação.
  - `core-data` não muda.
- **Novos arquivos:**
  - `DefaultPaymentAppCollector.kt` e `DefaultPaymentAppCollectorTest.kt` (JUnit 4, sem Robolectric);
  - `CardEmulationShadows.kt` (três shadows do Robolectric, só nos testes).
- **Arquivos modificados:** `InstalledApp.kt`, `InstalledAppTest.kt`, `DomainFixtures.kt`, `PackageManagerAppInventory.kt`, `PackageManagerAppInventoryTest.kt`; documentação: `core-detection/README.md`, `core-domain/README.md`, `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-021 e a pendência da §13), `.agent/project-reference.md`, `.agent/context.md`, `.agent/glossary.md`, `.agent/architecture.md`, `.agent/conventions.md`, `.agent/runbook.md`, `docs/especificacao.md` (§8 e §13); este plano.
- **Fora do escopo:**
  - pesos e regras de ajuste (SPEC-017);
  - ligação no Hilt e uso no `ScanDevice` (SPEC-018, #14);
  - o monitor do pagamento padrão e a gravação em `lastDefaultPaymentComponent` (SPEC-052, #28). Esta spec só fixa o formato e a recomendação no ADR-021;
  - conferência em aparelho (SPEC-022, #17);
  - bit 6 de `app_snapshot.signals` (SPEC-018).

## Decisões (usuário, 2026-11-24)

| Item | Decisão |
|------|---------|
| Erros da consulta | Sem NFC ou sem HCE, e serviço NFC morto: campo vazio em todos os apps, sem erro. Falta da permissão `NFC` no app hospedeiro: a `SecurityException` propaga, sem `try`, como no S4 |
| Campo novo | `InstalledApp.defaultPaymentServices: Set<String>`, com os nomes de classe dos serviços do app que o sistema diz serem o padrão. Mesmo formato de `cardEmulationServices` e `enabledAccessibilityServices` |
| Formato para a SPEC-052 | `ComponentName.flattenToString()` (forma completa `pkg/pkg.Cls`, a mesma que o sistema grava na chave). Com vários serviços (API 35+), o menor em ordem alfabética. Nenhum serviço quer dizer nulo |
| Detecção da troca (SPEC-052, emenda 1.1) | Recomendação registrada no ADR-021: a SPEC-052 detecta a troca **pelo pacote**. O `flattenToString` só serve para exibir e guardar em `lastDefaultPaymentComponent`. Assim, um serviço novo no app dono do papel de carteira não parece troca de app |
| Verificação por versão | Robolectric nas cinco APIs de sempre (26, 28, 30, 33, 36). O comportamento por versão vem das fontes do AOSP (8, 12, 14, 15 e 16), registradas no ADR-021. A conferência em aparelho fica na SPEC-022 (#17), como no S3 e no S4 |
| Serviços consultados (emenda 1.1) | Só os ligados e exportados. "Ligado" vem de uma **segunda consulta** `queryIntentServices` sem `MATCH_DISABLED_COMPONENTS`, não de `ServiceInfo.enabled` (motivo abaixo). `cardEmulationServices` continua com o critério largo do ADR-018 |

## Gap analysis

| # | Categoria | Lacuna | Classificação | Resolução |
|---|-----------|--------|---------------|-----------|
| 1 | Erros | O que fazer quando a consulta falha | 🔴 Bloqueante | Decidido (tabela acima) |
| 2 | Dados | Forma do campo novo | 🔴 Bloqueante | Decidido: `Set<String>` |
| 3 | Dados | Formato do componente para a SPEC-052 | 🔴 Bloqueante | Decidido: `flattenToString`, o menor; troca detectada pelo pacote |
| 4 | Testes | Como cumprir "verificado em Android 8, 12, 14 e na mais recente" | 🔴 Bloqueante | Decidido: fontes + Robolectric; aparelho na SPEC-022 |
| 5 | Técnico | De onde vem "ligado" | 🔴 Bloqueante | Decidido: segunda consulta sem `MATCH_DISABLED_COMPONENTS` |
| 6 | Regra | S7 para o app dono do serviço padrão | 🟡 Não bloqueante | A §5 diz "é o app de pagamento padrão": o S7 vai para o pacote do serviço |
| 7 | Regra | Padrão de app do sistema ou do próprio Tap Guard | 🟡 Não bloqueante | Regra 1 e RF-101: esses apps não entram no inventário, então nenhum app recebe S7 |
| 8 | Técnico | Ordem da etapa nova no inventário | 🟡 Não bloqueante | Depois da leitura da acessibilidade e antes do detalhe; precisa do resultado da consulta de HCE |

## Pesquisa: de onde vem o pagamento padrão

### As fontes públicas
`CardEmulation` não tem `getDefaultServiceForCategory` público em nenhuma versão. O método com esse nome existe só no serviço NFC (`CardEmulationManager`). As fontes possíveis para um app comum:

| Fonte | API 26 a 30 | API 31 a 34 | API 35 e 36 (papel de carteira) | Permissão | Em erro |
|---|---|---|---|---|---|
| `CardEmulation.isDefaultServiceForCategory(cn, CATEGORY_PAYMENT)` (pública desde a API 19) | `true` se `cn` é o componente da chave **e** está registrado no sistema NFC | igual | com o papel ligado, `true` para **qualquer** serviço registrado do app dono do papel, sem olhar a categoria | `android.permission.NFC` | até a API 35, `RemoteException` duas vezes vira `false`; na 36, `RuntimeException` com causa `RemoteException` |
| `Settings.Secure.getString(cr, "nfc_payment_default_component")` (chave `@hide`) | legível (só instant apps barrados, como no S4) | `SecurityException`: chave `@hide` sem `@Readable` | igual; e o sistema não consulta a chave quando o papel está ligado | nenhuma até a 30 | `null` se o provedor falha |
| `CardEmulation.getPreferredPaymentService(Context)` (API 36) | não existe | não existe | `@SystemApi`, exige `NFC_PREFERRED_PAYMENT_INFO` e lê a mesma chave | sistema | — |
| `getDescriptionForPreferredPaymentService`, `getAidsForPreferredPaymentService`, `getRouteDestinationForPreferredPaymentService` (API 30) | não existem na 26 a 29 | devolvem descrição, AIDs e rota, **não** o componente | igual | `NFC_PREFERRED_PAYMENT_INFO`, fora da lista de permissões | — |
| `RoleManager.isRoleHeld(ROLE_WALLET)` (API 35) | não existe | não existe | só diz se **o próprio app** tem o papel; `getRoleHolders` é `@SystemApi` | — | — |

**Escolha: `isDefaultServiceForCategory`**, a que a §8 já cita. É a única pública que identifica o app nas APIs 26 a 36, e a permissão `NFC` já está na lista do app.

### Por que não a chave do `Settings.Secure`, como no S4
- **API 31+:** `NFC_PAYMENT_DEFAULT_COMPONENT` é `@hide` e não tem `@Readable` em `Settings.java` nas tags [android-12.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r1/core/java/android/provider/Settings.java), android-14.0.0_r1, [android-16.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/core/java/android/provider/Settings.java) e na `main`. Na 16 e na `main` ela está `@Deprecated` ("please use `RoleManager#getRoleHolders(String)` with `ROLE_WALLET`").
- O cliente (`Settings.NameValueCache`) e o provedor ([`SettingsProvider.checkReadableAnnotation`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java)) lançam `SecurityException`: "From S+, settings keys annotated with @hide are restricted to system_server and system apps only, unless they are annotated with @Readable." A regra **não depende do targetSdk**: vale para todo app que não é do sistema, nem assinado com a chave da plataforma, nem `testOnly`.
- **API 35+ com o papel de carteira:** o sistema deixa de usar a chave para responder (abaixo). Mesmo que fosse legível, ela poderia estar velha.
- O Robolectric não aplica o `@Readable` (já visto no S4). Um teste com a chave passaria e o aparelho falharia.

### Como o sistema responde (lado do serviço NFC)
- **API 26 a 34:** [`CardEmulationManager.isDefaultServiceForCategory`](https://android.googlesource.com/platform/packages/apps/Nfc/+/refs/tags/android-8.0.0_r1/src/com/android/nfc/cardemulation/CardEmulationManager.java) (tags 8.0.0_r1, 12.0.0_r1 e 14.0.0_r1, mesmo código):
  1. `NfcPermissions.enforceUserPermissions` → `enforceCallingOrSelfPermission(NFC)`. Sem a permissão, `SecurityException`, que atravessa o binder (o cliente só pega `RemoteException`);
  2. `isServiceRegistered(userId, service)`: o serviço tem de estar no cache do sistema NFC (exportado, pacote com `NFC`, `BIND_NFC_SERVICE`, meta-data válida e ligado; ADR-018). Se não está, o sistema **refaz o cache** e confere de novo;
  3. lê a chave `nfc_payment_default_component` (o sistema pode), faz `unflattenFromString` e compara com `equals`.
- **API 35** ([packages/apps/Nfc, android-15.0.0_r1](https://android.googlesource.com/platform/packages/apps/Nfc/+/refs/tags/android-15.0.0_r1/src/com/android/nfc/cardemulation/CardEmulationManager.java)) **e 36** ([packages/modules/Nfc, android-16.0.0_r1](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/tags/android-16.0.0_r1/NfcNci/src/com/android/nfc/cardemulation/CardEmulationManager.java)): depois dos passos 1 e 2, se `mWalletRoleObserver.isWalletRoleFeatureEnabled()` (a flag `android.permission.flags.wallet_role_enabled`), responde `service.getPackageName().equals(<dono do papel>)`. A categoria é ignorada. Sem dono do papel, `false`. Sem a flag, volta ao passo 3.
- O [guia de HCE](https://developer.android.com/develop/connectivity/nfc/hce) diz: "Android 15 and higher includes a default wallet app role that the user can select by navigating to Settings > Apps > Default Apps". `RoleManager.ROLE_WALLET` é da API 35. Não achei fonte que diga se a flag vem ligada em todo aparelho com Android 15. O código acima trata os dois casos; o aparelho confirma na SPEC-022.
- **Quem grava a chave:** `setDefaultServiceForCategoryChecked` grava `service.flattenToString()` (forma completa) em todas as tags lidas. É daí que vem o formato para a SPEC-052.

### Permissão e visibilidade de pacotes
- **Permissão:** `android.permission.NFC`, cobrada no serviço NFC. O app já a declara (lista de permissões). O `core-detection` continua sem declarar permissão (ADR-016): o app que usar o SDK declara `NFC`, ou recebe a `SecurityException` (decisão: propaga). O método não tem `@RequiresPermission` nas tags lidas, então o lint não deve acusar (confirmar no build).
- **Visibilidade:** a chamada recebe um `ComponentName` e consulta o cache do sistema NFC, sem filtro de pacotes. O filtro está antes: as duas consultas de HCE (SPEC-013 e a nova) só veem o que a regra da lista permite (ADR-016; sem `QUERY_ALL_PACKAGES`, o README já pede o `<queries>` com a ação de HCE, que vale para as duas). O S7 não pede `<queries>` a mais.

### Serviços consultados: ligados e exportados (emenda 1.1)
- **O pedido:** perguntar ao sistema só pelos serviços ligados e exportados. Um serviço desligado ou não exportado nunca é o padrão (o sistema NFC não o registra), e perguntar por ele faz o serviço NFC refazer o cache a cada consulta.
- **O que achei:** o `ServiceInfo.enabled` que o `queryIntentServices` devolve é o valor do **manifesto**, não o estado atual:
  - [`PackageInfoUtils`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r1/services/core/java/com/android/server/pm/parsing/PackageInfoUtils.java) das tags 14.0.0_r1 e 16.0.0_r1: `info.enabled = component.isEnabled()`, o componente lido do manifesto;
  - [`PackageParser.generateServiceInfo`](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/core/java/android/content/pm/PackageParser.java), tag 8.0.0_r1: copia o `ServiceInfo` do manifesto e só atualiza o `ApplicationInfo`;
  - o estado mudado com `setComponentEnabledSetting` só **filtra** a consulta feita sem `MATCH_DISABLED_COMPONENTS`. O app inteiro desligado aparece em `applicationInfo.enabled`.
- **Por que importa:** com `ServiceInfo.isEnabled()`, um serviço desligado no manifesto e ligado depois com `setComponentEnabledSetting` ficaria de fora, e o sistema NFC o usa. Um app de golpe escaparia do S7 assim (o ADR-018 já cita esse truque para o S3). E um serviço desligado em tempo de execução continuaria sendo consultado.
- **Decisão (usuário):** uma segunda consulta `queryIntentServices(Intent(HostApduService.SERVICE_INTERFACE), 0)`, sem `MATCH_DISABLED_COMPONENTS`. O `PackageManager` aplica o estado atual do componente e do app, como na consulta do próprio sistema NFC (`RegisteredServicesCache`, ADR-018). Fica com `exported` (valor do manifesto, que não muda em tempo de execução) e `permission == BIND_NFC_SERVICE`.
- **Como o inventário guarda isso entre as etapas:** em valores locais dentro de `installedApps()`, nada no `InstalledApp` nem em campos da classe:
  1. etapa 2 (como hoje): `cardEmulationServices: Map<String, Set<String>>`, pacote → classes, critério largo;
  2. etapa 4 (nova): monta os **candidatos** = (segunda consulta ∩ `exported` ∩ `BIND_NFC_SERVICE`) ∩ `cardEmulationServices` dos apps de usuário; pergunta ao sistema por cada candidato e devolve `defaultPaymentServices: Map<String, Set<String>>`;
  3. etapa 5 (detalhe): cada app recebe `cardEmulationServices[pkg].orEmpty()` e `defaultPaymentServices[pkg].orEmpty()`.
  A interseção com `cardEmulationServices` garante que o campo novo é subconjunto do antigo, mesmo se um app for instalado entre as duas consultas.
- **Atalhos sem custo:** sem app de usuário com serviço de HCE, a etapa 4 termina sem tocar no NFC nem fazer a segunda consulta. Sem NFC ou sem HCE no aparelho, termina antes da segunda consulta.
- **Conferido no Robolectric** (experimento, item 9): o estado em tempo de execução só aparece no filtro da consulta sem `MATCH_DISABLED_COMPONENTS`, igual ao AOSP.

### Aparelho sem NFC ou sem HCE
- **Sem `FEATURE_NFC` e sem `FEATURE_NFC_HOST_CARD_EMULATION`:** `NfcAdapter.getDefaultAdapter(context)` devolve `null` ([NfcAdapter e NfcManager, android-8.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/core/java/android/nfc/NfcManager.java): `getNfcAdapter` lança `UnsupportedOperationException` e o `NfcManager` a troca por `null`; na [16](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/tags/android-16.0.0_r1/framework/java/android/nfc/NfcAdapter.java), também `null` sem o serviço). **Etapa pulada; campo vazio em todos os apps; sem erro.**
- **Com NFC e sem HCE:** `CardEmulation.getInstance(adapter)` lança `UnsupportedOperationException` ("This device does not support card emulation"), nas tags 8.0.0_r1 a 16.0.0_r1. Também lança se o serviço NFC não tem a interface de emulação. **Mesmo tratamento: campo vazio, sem erro.**
- **NFC desligado:** o `NfcService` cria o `CardEmulationManager` no construtor quando o aparelho tem HCE, ligado ou não ([android-14.0.0_r1](https://android.googlesource.com/platform/packages/apps/Nfc/+/refs/tags/android-14.0.0_r1/src/com/android/nfc/NfcService.java), `getNfcCardEmulationInterface`). A consulta funciona com o NFC desligado. Conferir em aparelho na SPEC-022.
- **Serviço NFC morto:**
  - até a API 35, o `CardEmulation` tenta de novo e devolve `false` ([android-15.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/nfc/java/android/nfc/cardemulation/CardEmulation.java)). Não se distingue de "não é o padrão";
  - na API 36 ([módulo NFC, android-16.0.0_r1](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/tags/android-16.0.0_r1/framework/java/android/nfc/cardemulation/CardEmulation.java)), `callServiceReturn` tenta de novo e chama `rethrowAsRuntimeException`: `RuntimeException` com causa `RemoteException`. **Decisão: campo vazio em todos os apps nessa varredura, sem erro.** A captura olha a causa, para não engolir a `SecurityException` (que também é `RuntimeException`, sem causa).

### Diferenças entre a API 26 e a 36, resumidas
| | API 26 | API 36 |
|---|---|---|
| Onde está o `CardEmulation` | `frameworks/base` | módulo NFC (`packages/modules/Nfc`) |
| Resposta | componente da chave, se registrado | dono do papel de carteira, qualquer serviço registrado dele (com a flag ligada) |
| Falha do serviço NFC | `false` | `RuntimeException(RemoteException)` |
| Chave do `Settings.Secure` | legível | `SecurityException`; `@Deprecated` |
| `getInstance` sem HCE | `UnsupportedOperationException` | igual |

## Regra do S7
- **§5:** "É o app de pagamento padrão e não está na lista de carteiras conhecidas". A lista diz "pacote que pode ser o app de pagamento padrão sem gerar S7".
- **Regra:** `{UNKNOWN_DEFAULT_PAYMENT_APP}` quando `defaultPaymentServices` não está vazio **e** `packageName` não está no conjunto de carteiras. Comparação exata, com maiúsculas e minúsculas, como no S1 e no S9. Fora isso, vazio.
- **Para quem vai:** o app dono do serviço padrão (o pacote do `ComponentName`). Na API 35+ com o papel, o dono do papel, se ele tiver um serviço de HCE registrado.
- **Serviço padrão de app do sistema** (por exemplo, um serviço de pagamento pré-instalado): o inventário não lista apps do sistema (regra 1), nem consulta os serviços deles. Nenhum app recebe S7.
- **Serviço padrão do próprio Tap Guard:** o Tap Guard não entra no inventário (RF-101) e não tem serviço de HCE. Nenhum app recebe S7. Vale também para o app do banco que usar o SDK: ele sai da própria lista.
- **Carteira da lista instalada à mão:** não recebe S7, mas recebe S1 (e o que mais tiver). O S7 só olha o pacote.
- **Coletor:** `DefaultPaymentAppCollector(knownWallets: Set<String>)`, com o conjunto obrigatório, sem valor padrão, copiado (`Collections.unmodifiableSet(LinkedHashSet(...))`), como o `InstallSourceCollector` e o `BankLookalikeCollector`. Lista padrão: `KnownWallets.packageNames`. Conjunto vazio: todo app padrão recebe S7. Sem `android.*`.
- **Efeito no score (§5, para a SPEC-017):** o S7 tira o teto de 29 da loja confiável (regra 3) e conta como sinal de NFC para o vermelho (regra 4). Um banco com Pix por aproximação no próprio app que não está na lista, baixado da loja, com S2, S3 e S7, vai a 65, limitado a 59 pela regra 4: amarelo. É a limitação "listas embutidas" do ADR-020.

## Onde entra o dado

| Lugar | Impacto |
|---|---|
| `InstalledApp` | Campo `defaultPaymentServices: Set<String>`, obrigatório, depois de `enabledAccessibilityServices`, copiado com `unmodifiableCopy()` na fábrica. KDoc: serviços do app que o sistema diz serem o padrão de pagamento; só contém classes que também estão em `cardEmulationServices`. A fábrica passa a 11 parâmetros, dentro da exceção do ADR-015 |
| `PackageManagerAppInventory` | Passa a cinco etapas: lista leve, HCE, acessibilidade, **pagamento padrão**, detalhe. A etapa nova: sem app de usuário com HCE → vazio. `NfcAdapter.getDefaultAdapter(context)`; nulo → vazio. `CardEmulation.getInstance(adapter)`; `UnsupportedOperationException` → vazio. Segunda consulta de HCE sem `MATCH_DISABLED_COMPONENTS`, com `exported` e `BIND_NFC_SERVICE`, cruzada com `cardEmulationServices` dos apps de usuário. Para cada candidato, `isDefaultServiceForCategory(ComponentName(pacote, classe), CATEGORY_PAYMENT)`. `RuntimeException` com causa `RemoteException` → todos vazios. `SecurityException` propaga. Tudo no `ioDispatcher`. Construtor igual (guarda o `Context` da aplicação) |
| Fixtures | `anInstalledApp` ganha `defaultPaymentServices` com padrão vazio (10 parâmetros, dentro da exceção do ADR-019) |
| `core-data` | Sem mudança. O S7 vai no bit 6 de `signals` pela SPEC-018; o `1.json` não muda. `lastDefaultPaymentComponent` já existe (SPEC-004); quem grava é a SPEC-052 |

- **Chamadas por varredura:** uma consulta de HCE a mais (só se algum app de usuário tem HCE e o aparelho tem NFC com HCE) e uma pergunta ao sistema NFC por serviço candidato (costumam ser poucos). Na API 26 a 34 no máximo um componente é o padrão; na 35+, os do dono do papel. Não há atalho: perguntar por todos é o que dá o conjunto completo.
- **Custo que sobra:** um serviço ligado, exportado e com `BIND_NFC_SERVICE`, mas com meta-data inválida ou de pacote sem a permissão `NFC`, ainda não é registrado pelo sistema NFC e faz o serviço NFC refazer o cache a cada consulta. É uma declaração de HCE malfeita, rara. Filtrar também pela permissão `NFC` do pacote exigiria pôr a etapa depois do detalhe (é lá que `requestedPermissions` é lido); fica fora desta spec. A medição contra o RNF-03 (300 apps em 5 s) continua na SPEC-022.

## Ligação com a SPEC-052
- **Formato (decidido):** `ComponentName(packageName, className).flattenToString()`, a forma completa `pkg/pkg.Cls`, a mesma que o sistema grava em `nfc_payment_default_component`. Entre todos os serviços marcados na varredura, o menor por `String.compareTo`; nenhum = `null`. É o texto que a SPEC-052 exibe e guarda em `lastDefaultPaymentComponent`.
- **Recomendação para a SPEC-052 (emenda 1.1, no ADR-021):** detectar a troca do pagamento padrão **pelo pacote**. Compara o pacote atual com o pacote do valor guardado (`ComponentName.unflattenFromString(lastDefaultPaymentComponent)?.packageName`). O `flattenToString` serve só para exibir e guardar. Assim, um serviço novo no app dono do papel de carteira (API 35+), que mudaria o menor componente, não parece uma troca de app. Segue o RF-303, que fala em "app".
- **Esta spec não escreve esse cálculo:** só garante o campo e registra formato e recomendação no ADR-021. Quem monta o texto e compara (um ajudante no `core-detection` ou o próprio monitor) fica para a SPEC-052.
- **Ressalvas para a SPEC-052** (no ADR-021):
  - `null` também quer dizer: aparelho sem NFC ou sem HCE, serviço NFC morto, padrão num app do sistema (fora do inventário) ou padrão num app cujo serviço o sistema não registrou. Trocar de uma carteira do sistema para outra do sistema não aparece;
  - se o dono do papel não tiver serviço de HCE próprio, a consulta não acha o padrão. O módulo NFC da API 36 tem a propriedade `PROPERTY_ALLOW_SHARED_ROLE_PRIORITY` ("associated role services"), o que sugere serviços de outro pacote ligados ao dono do papel. Não conferi como a Carteira do Google se registra. Conferir na SPEC-022.

## Experimento (clone descartável no scratchpad, 2026-11-24, Robolectric 4.18, APIs 26, 28, 30, 33 e 36)
Funcionou nas cinco APIs, sem reflexão. Os resultados foram iguais em todas:
1. **Sem features:** `hasSystemFeature(FEATURE_NFC)` e `(FEATURE_NFC_HOST_CARD_EMULATION)` são `false`, e `NfcAdapter.getDefaultAdapter(context)` devolve `null`. Os testes de inventário que já existem continuam iguais (etapa pulada, campo vazio).
2. **`ShadowNfcAdapter.setNfcHardwareExists(true)` sem feature:** ainda `null`. O shadow segue a feature, como o sistema.
3. **`FEATURE_NFC` sem HCE:** o adaptador existe; `CardEmulation.getInstance` lança `UnsupportedOperationException` (código real, o Robolectric não o substitui).
4. **Com as duas features:** `getInstance` funciona. `ShadowCardEmulation.setDefaultServiceForCategory(cn, CATEGORY_PAYMENT)`:
   - `isDefaultServiceForCategory(cn, CATEGORY_PAYMENT)` → `true`; outro componente → `false`; `cn` com `CATEGORY_OTHER` → `false`; antes de definir → `false`;
   - `ComponentName("com.scam.app", ".PayService")` → `false`: o shadow compara com `equals`, e a forma curta não é expandida. O inventário usa `ServiceInfo.name`, que é o nome completo.
5. **Só `FEATURE_NFC_HOST_CARD_EMULATION`:** o adaptador existe e `getInstance` funciona (igual ao `getNfcAdapter` da API 26, que aceita só HCE).
6. **Estado entre testes:** o mapa do shadow volta vazio no começo de cada teste.
7. **Erros com shadow próprio** (`@Implements(CardEmulation::class)` com `@Config(shadows = [...])` no teste): `RuntimeException(RemoteException)` e `SecurityException` saem de `isDefaultServiceForCategory` nas cinco APIs, e o teste seguinte volta ao shadow padrão. É assim que os caminhos de erro são testados.
8. **Chave do `Settings.Secure`:** `putString`/`getString` de `nfc_payment_default_component` funcionam no Robolectric nas cinco APIs. É o motivo para **não** usá-la: o Robolectric não aplica o `@Readable` e o teste passaria onde o aparelho falha. `flattenToString()` dá `com.scam.app/com.scam.app.PayService`, `flattenToShortString()` dá `com.scam.app/.PayService`, e as duas voltam ao mesmo `ComponentName`.
9. **Estado ligado e desligado (emenda 1.1):** cinco serviços de HCE num pacote (ligado no manifesto; desligado no manifesto; ligado no manifesto e desligado com `setComponentEnabledSetting`; desligado no manifesto e ligado com `setComponentEnabledSetting`; não exportado):
   - com `MATCH_DISABLED_COMPONENTS`: os cinco voltam. `enabled` é o do manifesto: o desligado em tempo de execução volta com `enabled=true`, e o ligado em tempo de execução volta com `enabled=false`;
   - sem a flag: voltam o ligado no manifesto, o ligado em tempo de execução e o não exportado (com `exported=false`). Saem o desligado no manifesto e o desligado em tempo de execução;
   - com o app inteiro desligado (`setApplicationEnabledSetting`): com a flag, todos voltam com `isEnabled()=false`; sem a flag, nenhum.
   Igual ao AOSP. É o que faz a segunda consulta ser a fonte de "ligado".
10. **Detekt 1.23.8 nas formas da captura e dos shadows (emenda 1.1):**
    - produção: `catch (expected: RuntimeException) { if (expected.cause !is RemoteException) throw expected … }` e `catch (expected: UnsupportedOperationException)` passam (o nome `expected` é liberado em `TooGenericExceptionCaught` e `SwallowedException`; `InstanceOfCheckForException` não acusa o teste da causa);
    - teste: `throw RuntimeException(RemoteException(...))` cai em `TooGenericExceptionThrown`, que vale também nos testes. `throw RemoteException(...).rethrowAsRuntimeException()` passa e é o que o próprio framework faz;
    - teste: os parâmetros do método do shadow precisam ser usados (`UnusedParameter`). Os shadows os usam na mensagem ou na resposta.

**O que o Robolectric não simula:** o registro do serviço no sistema NFC, o papel de carteira, a permissão `NFC` e a diferença de resposta entre as APIs. Por isso a conferência em aparelho fica na SPEC-022.

## Approach
- Seguir:
  - o formato dos coletores das SPECs 012 a 015 (ADR-017 a ADR-020): `class` pública, `override suspend fun collect`, `companion` privado com o conjunto de resposta, KDoc com o que conta e o que não conta;
  - o do S1 e do S9 para o conjunto recebido (obrigatório, sem padrão, copiado);
  - `skills/android/SKILL.md` › Reading installed packages e Dispatchers in adapters;
  - `skills/android/testing.md` › Adapter Tests — Robolectric (cinco APIs).
- **Inventário:** a etapa nova fica em funções privadas que devolvem `Map<String, Set<String>>` (pacote → classes), como as das etapas 2 e 3. As duas consultas de HCE usam o `queryIntentServicesCompat` que já existe. Detalhes no contrato.
- **Coletor:** sem `android.*`; não compara categorias nem componentes, só lê o campo e o pacote.
- **Testes:**
  - coletor em JUnit 4 puro, com uma lista pequena escrita à mão (não `KnownWallets.packageNames`);
  - inventário no Robolectric, nas cinco APIs, com as features, `ShadowNfcAdapter.setNfcHardwareExists(true)`, `ShadowCardEmulation.setDefaultServiceForCategory` e três shadows próprios (serviço NFC morto, sem permissão, papel de carteira);
  - o `ShadowCardEmulation` responde `true` para o componente definido como padrão mesmo se o serviço estiver desligado (ele não simula o registro). Por isso "serviço desligado não é consultado" se prova definindo o serviço desligado como padrão e conferindo que o campo fica vazio: se o inventário perguntasse, o campo o teria.

## Tasks (um PR "SPEC-016 Pagamento padrão (S7)", `Closes #12`)
1. [ ] **Modelo, inventário e coletor** (`feat:`):
   - `defaultPaymentServices` no `InstalledApp`, com cópia que não pode ser alterada, e parâmetro na `anInstalledApp`;
   - etapa do pagamento padrão no `PackageManagerAppInventory`, com a segunda consulta e os três tratamentos de erro;
   - `DefaultPaymentAppCollector` com o conjunto de carteiras, com KDoc;
   - `./gradlew build` verde.
2. [ ] **Testes** (`test:`, papel tester):
   - `InstalledAppTest` com o campo novo;
   - `CardEmulationShadows.kt` com os três shadows;
   - inventário nas cinco APIs: padrão de um app de usuário entre dois com HCE; padrão fora dos serviços consultados (app do sistema, classe sem o filtro de HCE); vários serviços do mesmo app (papel de carteira); **serviço desligado em tempo de execução, desligado no manifesto, não exportado e de app desligado não são consultados**; **serviço desligado no manifesto e ligado em tempo de execução é consultado**; sem NFC; NFC sem HCE; serviço NFC morto; sem permissão (propaga);
   - coletor: padrão fora da lista, padrão na lista, não é o padrão (com HCE), maiúsculas, conjunto vazio, conjunto copiado, só S7.
3. [ ] **Conferência dos critérios**, local e desfeita sem commit:
   - capturar qualquer `RuntimeException` (sem olhar a causa) derruba o teste da `SecurityException`;
   - não capturar a `UnsupportedOperationException` derruba o teste de NFC sem HCE;
   - perguntar com `CATEGORY_OTHER` derruba o teste do padrão;
   - **usar como candidatos todos os serviços de `cardEmulationServices` (sem a segunda consulta e sem `exported`) derruba o teste dos serviços que não são consultados**;
   - **trocar a segunda consulta por `ServiceInfo.isEnabled()` da primeira derruba o teste do serviço ligado em tempo de execução e o do desligado em tempo de execução**;
   - comparar o pacote sem maiúsculas derruba o teste de maiúsculas do coletor;
   - `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
4. [ ] **Documentação** (`docs:`):
   - `core-detection/README.md`: inventário em cinco etapas (com a segunda consulta), coletor de S7, a permissão `NFC` para quem usa o SDK, testes com `ShadowCardEmulation` e shadows próprios;
   - `core-domain/README.md`: campo novo;
   - `docs/especificacao.md`: §8 (linhas "Inventário" e "S7 pagamento padrão") e §13 (pendência da API no Android 15+ resolvida);
   - `SKILLS.md`, `project-reference.md`, `context.md`, `glossary.md`, `architecture.md` (linha do S7), `conventions.md` (a falha degrada, menos a `SecurityException`), `runbook.md`;
   - **ADR-021** em `decisions.md`: fonte, chave rejeitada, papel de carteira, serviços consultados (segunda consulta e por que não `ServiceInfo.enabled`), erros, formato para a SPEC-052, **recomendação de detectar a troca pelo pacote**, ressalvas, alternativas rejeitadas; e a pendência "API de pagamento padrão no Android 15+" marcada como resolvida.

## Tamanho: cerca de 435 linhas, um PR

| Parte | Código | Testes |
|---|---|---|
| `InstalledApp` (campo, KDoc, fábrica) e fixture | ~10 | |
| `InstalledAppTest` | | ~15 |
| Inventário (etapa, segunda consulta, erros, KDoc) | ~60 | |
| Coletor (KDoc) | ~45 | |
| Shadows de teste (três) | | ~55 |
| Testes do inventário (9 cenários novos e 1 ajustado) | | ~165 |
| Testes do coletor | | ~85 |
| **Total** | **~115** | **~320** |

No plano aprovado eram 360. A revisão do architect contou pelos arquivos do contrato e somou os dois cenários da emenda 1.1, a segunda consulta e o arquivo dos shadows: **435** (tabela por arquivo no documento de arquitetura). Abaixo de 500: **um PR, sem batches**. A documentação não entra na conta (regra do SDD 1.8.0).

## Database changes
Nenhuma. `app_snapshot` não guarda serviços. O S7 vai no bit 6 de `signals` pela SPEC-018, e o `1.json` não muda (ADR-014). `lastDefaultPaymentComponent` já existe e não muda nesta spec.

## Risks & open questions
- **Robolectric não simula o sistema NFC:** registro do serviço, papel de carteira, permissão e diferenças entre APIs vêm das fontes do AOSP. Conferência em aparelho na SPEC-022 (Android 8, 12, 14 e a mais recente, como pede a issue).
- **Flag do papel de carteira no Android 15:** não achei fonte que diga se vem ligada em todo aparelho. O código trata os dois casos.
- **Custo que sobra da consulta:** só serviços ligados, exportados e com `BIND_NFC_SERVICE` mas com meta-data inválida ou de pacote sem `NFC` (declaração malfeita, rara) ainda fazem o sistema NFC refazer o cache. Uma consulta de HCE a mais por varredura, só com NFC e algum app com HCE.
- **Falha do serviço NFC até a API 35:** vira `false`, igual a "não é o padrão". O S7 some nessa varredura sem erro (mesma limitação do S4 com o `Settings.Secure`).
- **Pacote emprestado** (ADR-017, ADR-020): num aparelho sem a carteira, um app com o pacote dela não recebe S7. Instalado à mão, recebe S1. Defesa na issue #65.
- **Lint:** o método não tem `@RequiresPermission` nas tags lidas; se o lint acusar algo, o builder para e pergunta.
- **Detekt `LargeClass` (600 linhas de código) no `PackageManagerAppInventoryTest`:** hoje a classe tem cerca de 400; com os cenários novos, cerca de 545. Os shadows ficam em arquivo próprio. Se passar, o builder para e pergunta.
- **SPEC-052:** ressalvas do formato acima (`null` com vários significados, dono do papel sem serviço próprio); a troca pelo pacote resolve a troca falsa da API 35+.

## Acceptance criteria
- [ ] Padrão fora da lista gera S7; na lista, ou sem ser o padrão, não; comparação exata; conjunto obrigatório e copiado; só S7 (AC-001 a AC-006)
- [ ] Inventário marca só os serviços que o sistema diz serem o padrão, nos apps de usuário, nas cinco APIs; vários serviços do mesmo app (AC-007 a AC-009)
- [ ] Só serviços ligados (estado atual) e exportados são consultados; ligado em tempo de execução conta (AC-010, AC-011)
- [ ] Sem NFC, NFC sem HCE e serviço NFC morto: campo vazio sem erro; sem a permissão `NFC`: propaga (AC-012 a AC-015)
- [ ] Campo novo copiado e que não pode ser alterado; cobertura do `core-domain` acima de 80% (AC-016)
- [ ] Coletor sem API do Android; sem permissão nova na biblioteca; schema do Room sem diff (AC-017, AC-018)
- [ ] ktlint, detekt, lint e testes passam (AC-019)
- [ ] Documentação do módulo, §8, ADR-021 com formato e recomendação para a SPEC-052 (AC-020)
- [ ] CI do PR verde (AC-021)
