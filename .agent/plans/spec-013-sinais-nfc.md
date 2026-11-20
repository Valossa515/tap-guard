# Plan: SPEC-013 Sinais de NFC (S2, S3)

> Issue #9 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-011 (#7, fechada) · Especificação RF-102, §5 (S2, S3) e §8 (como cada sinal é coletado) · ADR-014, ADR-015, ADR-016, ADR-017
> Spec: `.agent/specs/features/spec-013-sinais-nfc.spec.toon` (FT-013 v1.1, `interface: library`, `estimate-lines: 420`) · Aceite: `.agent/specs/acceptance/spec-013-sinais-nfc.acceptance.toon`
> Arquitetura: `.agent/plans/spec-013-sinais-nfc-architecture.md` · Contrato: `.agent/specs/contracts/spec-013-sinais-nfc.contract.toon`
> Branch: `spec-013-sinais-nfc` · Planejado em 2026-11-19 · Primeira spec com o SDD 1.8.0

## Summary
Implementar no `core-detection` os coletores dos sinais S2 (declara a permissão de NFC) e S3 (declara serviço de emulação de cartão, HCE). Para o S3, o inventário (SPEC-011) passa a descobrir os serviços de HCE de cada app, porque `InstalledApp.services` só guarda nomes de classe. Para isso, faz uma consulta `queryIntentServices` por varredura com a ação `HostApduService.SERVICE_INTERFACE` e confere a permissão `BIND_NFC_SERVICE`. O resultado vai para o campo novo `InstalledApp.cardEmulationServices`. Os dois coletores só leem o `InstalledApp`, no formato do `InstallSourceCollector` (SPEC-012).

## Scope
- **Módulos afetados:**
  - `core-domain`: campo novo no `InstalledApp`, testes do modelo e a fixture `anInstalledApp`;
  - `core-detection`: inventário, ramo de versão da consulta, dois coletores e testes;
  - documentação.
  - `core-data` não muda.
- **Novos arquivos (nomes no contrato):** coletor de S2, coletor de S3 e os testes deles (JUnit 4, sem Robolectric).
- **Arquivos modificados:**
  - `InstalledApp.kt`, `InstalledAppTest.kt`, `DomainFixtures.kt`;
  - `PackageManagerVersionCompat.kt`, `PackageManagerAppInventory.kt`, `PackageManagerAppInventoryTest.kt`;
  - `core-detection/README.md`, `core-domain/README.md`, `docs/especificacao.md` (§8);
  - `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-018), `.agent/project-reference.md`, `.agent/context.md`, `.agent/glossary.md`;
  - este plano (marcar as tarefas).
- **Fora do escopo:**
  - S7 e a consulta do pagamento padrão (SPEC-016, #12), que vai reaproveitar o campo novo;
  - variantes `nfcOnly` e `hce` do app isca (SPEC-020, #15, e SPEC-021, #16);
  - ligação no Hilt e uso no `ScanDevice` (SPEC-018, #14);
  - pesos e regra 4 (SPEC-017);
  - conferência em aparelho real (SPEC-022, #17).

## Decisões (usuário, 2026-11-19)

| Item | Decisão |
|------|---------|
| Permissões do S2 | Só `android.permission.NFC` em `requestedPermissions`, comparação exata. É permissão normal, concedida na instalação, e é a que o modo leitor do golpe exige. `NFC_TRANSACTION_EVENT` (API 28) e `NFC_PREFERRED_PAYMENT_INFO` (API 30) não leem nem emulam cartão sem a `NFC`. `BIND_NFC_SERVICE` é de assinatura: o app a usa para proteger o serviço, não a pede |
| Como o inventário acha o HCE | Opção B: `queryIntentServices(Intent(HostApduService.SERVICE_INTERFACE))` com `MATCH_DISABLED_COMPONENTS`, uma vez por varredura, e `ServiceInfo.permission == BIND_NFC_SERVICE`. É a descoberta que o próprio sistema NFC faz, e a §8 já a prescreve |
| Off-host e NFC-F | Não contam. No off-host os comandos vão para o chip seguro e não passam pelo app ([OffHostApduService](https://developer.android.com/reference/android/nfc/cardemulation/OffHostApduService)). NFC-F (FeliCa) não é o padrão dos cartões EMV no Brasil. Registrar no ADR-018 |
| Critério mais largo que o do sistema | Não exigir serviço exportado, meta-data nem a permissão `NFC` no pacote. Um falso positivo raro é melhor que deixar de avisar. Registrar no ADR-018 |
| Serviços desligados | Contam: componente desligado no manifesto, desligado em tempo de execução e app inteiro desligado. O app pode ligar o serviço depois com `setComponentEnabledSetting`. Segue a SPEC-011 ("inclusive desligados") |
| Um coletor ou dois | Dois, um por sinal, como o do S1: cada um devolve o seu sinal ou nada. As entradas são diferentes (`requestedPermissions` e o campo novo), e o SDK pode usar só um. Comentário na #9 |
| Modelo | `InstalledApp` ganha `cardEmulationServices: Set<String>`, com os nomes de classe dos serviços de HCE (nome escolhido na revisão do architect). `services` fica como está |
| Banco | Sem mudança: `app_snapshot` não guarda serviços, e o S3 vai no bit 2 de `signals` (SPEC-018). O `1.json` não muda |
| Tamanho | 420 linhas de código e testes (419 pelos arquivos do contrato; 410 no plano antes da revisão): um PR só, sem batches |

### Pesquisa (fontes)
- **Como o sistema NFC acha os serviços:** [`RegisteredServicesCache.getInstalledServices`](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/heads/main/NfcNci/src/com/android/nfc/cardemulation/RegisteredServicesCache.java) (AOSP, main).
  - Faz duas consultas `queryIntentServicesAsUser`, com `HostApduService.SERVICE_INTERFACE` e `OffHostApduService.SERVICE_INTERFACE`. Usa `GET_META_DATA` e não pede `MATCH_DISABLED_COMPONENTS`.
  - Pula o serviço quando:
    - ele não está exportado;
    - o pacote não tem a permissão `NFC`;
    - `si.permission` é diferente de `BIND_NFC_SERVICE`;
    - o meta-data falta ou não pode ser lido (`ApduServiceInfo` lança `XmlPullParserException`).
  - É off-host quando a segunda consulta o devolve.
- **Requisitos do serviço:**
  - [HostApduService](https://developer.android.com/reference/android/nfc/cardemulation/HostApduService): `android:permission="android.permission.BIND_NFC_SERVICE"`, ação `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e meta-data `android.nfc.cardemulation.host_apdu_service`. Os comandos chegam ao app por `processCommandApdu`.
  - `BIND_NFC_SERVICE` também é exigida por `OffHostApduService` e por [HostNfcFService](https://developer.android.com/reference/android/nfc/cardemulation/HostNfcFService) (ação `HOST_NFCF_SERVICE`). Por isso a permissão sozinha não basta.
- **Permissões:** [Manifest.permission](https://developer.android.com/reference/android/Manifest.permission).
  - `NFC`: "perform I/O operations over NFC", normal.
  - `BIND_NFC_SERVICE`: signature.
  - `NFC_TRANSACTION_EVENT`: API 28, normal.
  - `NFC_PREFERRED_PAYMENT_INFO`: API 30, normal.
- **Visibilidade de pacotes:** [package-visibility](https://developer.android.com/training/package-visibility).
  - A partir do targetSdk 30 a consulta é filtrada. O app tem `QUERY_ALL_PACKAGES`.
  - Para o SDK, `<queries><intent><action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE"/></intent></queries>` torna visíveis os serviços com esse filtro. Mesmo assim, o SDK continua precisando ver os pacotes na lista do inventário.

### Opções avaliadas para o S3

| Opção | Confiabilidade | Custo |
|---|---|---|
| A. `ServiceInfo.permission == BIND_NFC_SERVICE`, do `getPackageInfo` que já existe | Média: conta off-host e NFC-F; não confere o filtro de intent | Nenhuma chamada a mais |
| A'. A + chave do meta-data (`GET_META_DATA`) | Boa: separa os três tipos; não confere o filtro | Nenhuma chamada a mais; carrega o meta-data de todos os serviços de cada app |
| **B. `queryIntentServices` com a ação de HCE + permissão (escolhida)** | A maior: é a descoberta do sistema | Uma chamada por varredura; ramo de versão na API 33 (`ResolveInfoFlags`); mesma regra de visibilidade da lista; `MATCH_DISABLED_COMPONENTS` só documentado para `PackageInfo` (conferir no aparelho, SPEC-022); app instalado entre a lista e a consulta fica sem S3 nessa varredura |
| B'. A mesma consulta por app (`setPackage`) | A mesma de B | N chamadas a mais (rejeitada) |

### Experimento (clone descartável no scratchpad, 2026-11-19, Robolectric 4.18, APIs 26, 28, 30, 33 e 36)
1. **`getPackageInfo(GET_SERVICES | MATCH_DISABLED_COMPONENTS)`:** devolve `ServiceInfo.permission` e respeita `GET_META_DATA`. Sem `MATCH_DISABLED_COMPONENTS`, omite o serviço com `enabled=false` e o desligado por `setComponentEnabledSetting`.
2. **`queryIntentServices`:** um serviço posto no `PackageInfo` passado a `installPackage` **não entra na resolução de intents**: `addIntentFilterForService` lança `IllegalArgumentException: … doesn't exist`. No bytecode do `ShadowPackageManager`, `addComponent` só cria a entrada no mapa de filtros quando o componente é novo.
3. **O que funciona:** instalar o pacote sem serviços, depois `shadow.addOrUpdateService(serviceInfo)` e `shadow.addIntentFilterForService(ComponentName, IntentFilter(action))`. Então:
   - `queryIntentServices` devolve o serviço com `permission` preenchida, nos dois ramos de versão (`ResolveInfoFlags` na 33+);
   - o serviço sem a permissão também volta, porque o filtro é do código;
   - a consulta off-host não mistura com a de HCE;
   - `getPackageInfo(GET_SERVICES)` continua listando os serviços.
4. **Desligados:** com `MATCH_DISABLED_COMPONENTS`, a consulta devolve os quatro casos: `enabled=false`, `setComponentEnabledSetting(DISABLED)`, app com `enabled=false` e `setApplicationEnabledSetting(DISABLED)`. Sem o flag, não devolve nenhum deles.
5. **Lint:** `queryIntentServices` gera `QueryPermissionsNeeded` (aviso) nos dois ramos, como `getInstalledPackages`.

## Approach
- Seguir:
  - `skills/android/SKILL.md` › Reading installed packages (ramos de versão num arquivo `internal`, `ensureActive()` por app) e Dispatchers in adapters;
  - `skills/android/testing.md` › Adapter Tests — Robolectric (cinco níveis de API);
  - o formato dos coletores da SPEC-012 (ADR-017).
- **Inventário:**
  - Depois da lista leve, uma só consulta de HCE com `MATCH_DISABLED_COMPONENTS`. Ela fica só com os serviços que exigem `BIND_NFC_SERVICE` e os agrupa por pacote.
  - Cada app de usuário recebe os nomes de classe dos seus; os demais pacotes da consulta, inclusive os do sistema, são ignorados.
  - Erros do sistema na consulta são propagados, como os demais (SPEC-011).
- **Coletores:** Kotlin puro, sem `PackageManager`, sem dispatcher e sem Hilt. Cada um devolve um conjunto com o seu sinal, ou vazio.
- **Testes:**
  - o inventário no Robolectric, nos cinco níveis, registrando os serviços como no experimento;
  - os coletores em JUnit 4 puro;
  - o caso parametrizado das permissões que não contam lê uma lista escrita à mão no teste.

## Tasks
1. [ ] **Modelo e inventário** (`feat:`):
   - `cardEmulationServices` no `InstalledApp`, com cópia que não pode ser alterada, e parâmetro na `anInstalledApp`;
   - ramo de versão da consulta no `PackageManagerVersionCompat.kt`;
   - consulta e preenchimento no `PackageManagerAppInventory`;
   - `./gradlew build` verde.
2. [ ] **Testes do modelo e do inventário** (`test:`, papel tester): `InstalledAppTest` com `cardEmulationServices`; cenários de HCE no `PackageManagerAppInventoryTest` nos cinco níveis.
3. [ ] **Coletores** (`feat:`): coletor de S2 e coletor de S3, com KDoc (o que conta, o que não conta e por quê; ADR-018).
4. [ ] **Testes dos coletores** (`test:`, papel tester):
   - S2: com e sem `NFC`, permissões relacionadas (parametrizado), maiúsculas, só S2;
   - S3: com e sem serviço de HCE, serviço comum, sem `NFC`, só S3.
5. [ ] **Documentação** (`docs:`):
   - READMEs do `core-detection` (coletores, critério do HCE, visibilidade para o SDK) e do `core-domain` (`cardEmulationServices`);
   - §8 de `docs/especificacao.md` (linha do S3 com permissão e desligados);
   - `SKILLS.md` (API pública, coletores de S2 e S3, segundo `@SuppressLint`, registro de serviço no Robolectric);
   - `project-reference.md`, `context.md`, `glossary.md`;
   - **ADR-018** em `decisions.md`.
6. [ ] **Conferência dos critérios**, localmente e desfeita sem commit:
   - tirar o `MATCH_DISABLED_COMPONENTS` derruba os testes de desligados;
   - tirar a conferência da permissão derruba o teste do filtro sem permissão;
   - trocar a ação pela off-host derruba os testes de HCE;
   - `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.

### Feito no planejamento (2026-11-19)
- Issue #9 movida para "Em andamento" no Project 2.
- [Comentário na #9](https://github.com/Valossa515/tap-guard/issues/9#issuecomment-7027958269): a entrega virou dois coletores, e o motivo.

### Revisão do architect (2026-11-19, emenda 1.1 da spec)
- Campo `cardEmulationServices`, obrigatório, depois de `services`; a fixture tem padrão vazio.
- Coletores públicos `NfcPermissionCollector` e `CardEmulationServiceCollector` (`class` sem parâmetros); `queryIntentServicesCompat` `internal`, com `@SuppressLint("QueryPermissionsNeeded")`.
- Estimativa pelos arquivos do contrato: 419 linhas, `estimate-lines: 420`.
- Detalhes em `.agent/plans/spec-013-sinais-nfc-architecture.md`.

## Database changes
Nenhuma. `app_snapshot` não tem coluna de serviços. S2 e S3 vão para `app_snapshot.signals` (bits 1 e 2) pela SPEC-018, e o `1.json` não muda (ADR-014).

## Risks & open questions
- **`MATCH_DISABLED_COMPONENTS` no `queryIntentServices` de um aparelho de verdade:** o Robolectric o respeita, mas a documentação só o descreve para `PackageInfo` e `ComponentInfo`. Conferir na SPEC-022 com um serviço de HCE desligado no app isca.
- **App instalado entre a lista e a consulta:** fica sem S3 nessa varredura e recebe na seguinte.
- **Critério mais largo que o do sistema:** um serviço com o filtro e a permissão, mas sem exportação ou sem meta-data, gera S3 sem funcionar como HCE. É falso positivo raro e aceito (ADR-018).
- **Robolectric:** os testes dependem de `addOrUpdateService` criar a entrada de filtros. Se a biblioteca mudar, eles falham em vez de passar.
- **SDK sem `QUERY_ALL_PACKAGES`:** só vê o HCE dos pacotes visíveis. A regra já existe para a lista (ADR-016); o README passa a citar o `<queries>` com a ação de HCE.

## Acceptance criteria
- [ ] App com `android.permission.NFC` gera S2; sem ela, ou só com permissões relacionadas, não gera; comparação exata (AC-001 a AC-004)
- [ ] App com serviço de HCE gera S3; sem serviço, não gera; S3 não depende de S2 (AC-005 a AC-007)
- [ ] Cada coletor só devolve o seu sinal ou nada (AC-008)
- [ ] Inventário: HCE com filtro e permissão entra; sem um dos dois, fica fora; off-host e NFC-F ficam fora; desligados entram; critério mais largo que o do sistema; app do sistema e outros apps não se misturam (AC-009 a AC-014)
- [ ] Campo novo copiado e que não pode ser alterado; cobertura do `core-domain` acima de 80% (AC-015)
- [ ] Sem API do Android nos coletores; sem permissão nova; schema do Room sem diff (AC-016, AC-017)
- [ ] ktlint, detekt, lint e testes passam (AC-018)
- [ ] Documentação atualizada, com o ADR-018 (AC-019)
- [ ] CI da PR verde (AC-020)
