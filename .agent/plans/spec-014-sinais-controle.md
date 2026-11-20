# Plan: SPEC-014 Sinais de controle (S4, S5, S6, S8)

> Issue #10 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-011 (#7, fechada) · Especificação RF-102, §5 (S4, S5, S6, S8) e §8 (como cada sinal é coletado) · ADR-016, ADR-017, ADR-018
> Spec: `.agent/specs/features/spec-014-sinais-controle.spec.toon` (FT-014 v1.1, `interface: library`, `estimate-lines: 625`, dois batches) · Aceite: `.agent/specs/acceptance/spec-014-sinais-controle.acceptance.toon`
> Arquitetura: `.agent/plans/spec-014-sinais-controle-architecture.md` · Contrato: `.agent/specs/contracts/spec-014-sinais-controle.contract.toon`
> Branches: `spec-014-sinais-controle` (BATCH-01) e `spec-014-sinais-controle-batch-02` (BATCH-02, criada a partir da `main` depois do merge do BATCH-01) · Planejado em 2026-11-20 · Primeira spec com batches (um PR por batch; só o do BATCH-02 leva `Closes #10`)

## Summary
Quatro coletores no `core-detection`, um por sinal, no formato dos coletores da SPEC-012 e da SPEC-013:
- S5 (pede sobreposição de tela): `SYSTEM_ALERT_WINDOW` em `requestedPermissions`;
- S6 (instalado ou atualizado nos últimos 7 dias): `firstInstallTime` e `lastUpdateTime`, com um `java.time.Clock` injetado;
- S8 (pede leitura ou recebimento de SMS): `READ_SMS` ou `RECEIVE_SMS` em `requestedPermissions`;
- S4 (serviço de acessibilidade ligado): o inventário lê `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` uma vez por varredura e guarda os serviços de cada app no campo novo `InstalledApp.enabledAccessibilityServices`. O coletor só lê o campo.

Os três primeiros usam dados que o `InstalledApp` já tem e vão no BATCH-01. O S4 precisa de campo novo e de mudança no inventário, e vai no BATCH-02.

## Scope
- **Módulos afetados:**
  - `core-domain`: campo novo no `InstalledApp` (BATCH-02), testes do modelo e parâmetros novos na fixture `anInstalledApp` (os dois batches);
  - `core-detection`: quatro coletores e testes; leitura do `Settings.Secure` no inventário (BATCH-02);
  - documentação.
  - `core-data` não muda.
- **Novos arquivos (nomes no contrato):** os quatro coletores e os testes deles (JUnit 4, sem Robolectric).
- **Arquivos modificados:**
  - BATCH-01: `DomainFixtures.kt`;
  - BATCH-02: `InstalledApp.kt`, `InstalledAppTest.kt`, `DomainFixtures.kt`, `PackageManagerAppInventory.kt`, `PackageManagerAppInventoryTest.kt`;
  - documentação nos dois batches: `core-detection/README.md`, `core-domain/README.md`, `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-019), `.agent/project-reference.md`, `.agent/context.md`, `.agent/glossary.md`; no BATCH-02 também `docs/especificacao.md` (§8);
  - este plano (marcar as tarefas).
- **Fora do escopo:**
  - pesos, faixas e regras de ajuste (SPEC-017);
  - ligação no Hilt, o `Clock` do grafo e o uso no `ScanDevice` (SPEC-018, #14);
  - variante `full` do app isca (Etapa 2);
  - conferência em aparelho real (SPEC-022, #17);
  - concessão da sobreposição pelo `AppOpsManager` (a §5 pede a permissão declarada);
  - bits 3, 4, 5 e 7 de `app_snapshot.signals` (SPEC-018).

## Decisões (usuário, 2026-11-20)

| Item | Decisão |
|------|---------|
| Onde entra o S4 | Opção A: campo no `InstalledApp`, preenchido pelo inventário numa leitura por varredura. O coletor fica puro, como o do S3 |
| Fonte do S4 | `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, no lugar do `AccessibilityManager.getEnabledAccessibilityServiceList` que a §8 citava. A §8 muda no BATCH-02. Motivos no ADR-019 (abaixo, com uma correção) |
| Formato da chave | Texto `pkg/cls:pkg/cls`. Cada entrada passa por `ComponentName.unflattenFromString`, que aceita o formato curto (`pkg/.Cls`), o mesmo que o sistema grava (`flattenToShortString`). Entradas vazias ou que voltam nulas são ignoradas sem derrubar a varredura, com teste. Agrupado por pacote e cruzado com os apps de usuário, como o HCE |
| Chave nula | Nenhum serviço ligado: todos os apps ficam com `enabledAccessibilityServices` vazio, sem erro. String vazia vale o mesmo |
| Limite do S6 | Inclusivo: um app atualizado há exatamente 7 dias (168 h) conta |
| Horário no futuro | Conta como S6. Com o relógio do aparelho atrasado, todos os apps ganham S6 (ADR-019, com a ressalva abaixo) |
| S5 | Permissão declarada (`SYSTEM_ALERT_WINDOW` em `requestedPermissions`), não concedida: a §5 diz "pede". Comparação exata |
| S8 | Só `android.permission.READ_SMS` e `android.permission.RECEIVE_SMS`, com comparação exata. Não contam `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`, `READ_CELL_BROADCASTS` e `BROADCAST_SMS` ([Manifest.permission](https://developer.android.com/reference/android/Manifest.permission)) |
| Batches | Dois, na ordem do rascunho: BATCH-01 com S5, S6 e S8; BATCH-02 com S4 |

### Pesquisa: a chave pode ser lida sem permissão, da API 26 à 36?
Sim. Fontes no AOSP:
- **API 26 a 30:** `SettingsProvider.enforceSettingReadable` só restringe instant apps; para os demais apps, volta logo depois de `if (!ai.isInstantApp()) return;`. Conferido nas tags [android-8.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java), android-9.0.0_r1, android-10.0.0_r1 e [android-11.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r1/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java). O `isSecureSettingAccessible` dessas versões só barra `bluetooth_address` e diz: "Secure settings can normally be read by any app".
- **API 31 em diante:** o `SettingsProvider` passa a chamar `checkReadableAnnotation` ([main](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java)). Ele lança `SecurityException` para chave sem `@Readable`, ou com `@Readable(maxTargetSdk = N)` quando o targetSdk do app passa de N. O cliente (`Settings.NameValueCache`) faz a mesma checagem.
- **A chave:** `ENABLED_ACCESSIBILITY_SERVICES` é pública e tem `@Readable` **sem** `maxTargetSdk` em [`Settings.java`](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/provider/Settings.java) nas tags android-12.0.0_r1, 13.0.0_r1, 14.0.0_r1, 15.0.0_r1 e 16.0.0_r1 e na `main`. Vale para targetSdk 36 (o do app) e acima (um app de banco com o SDK).
- **Visibilidade de pacotes:** o texto é global e não passa pelo filtro de pacotes. O SDK não precisa de `<queries>` para o S4. Os pacotes que ele não vê já ficam fora pela lista do inventário.

### Correção a um dos motivos do ADR-019
O motivo 2 dizia que o `AccessibilityManager` devolve lista vazia quando a chamada falha, e que o `Settings.Secure` não teria esse problema. **O `Settings.Secure` tem o mesmo problema.** `Settings.NameValueCache.getStringForUser` pega a `RemoteException` do provedor, escreve no log e devolve `null` ("Return null, but don't cache it"). O código é o mesmo em [android-8.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/core/java/android/provider/Settings.java), 16.0.0_r1 e `main`. Esse `null` não se distingue de "nenhum serviço ligado".
- Nenhuma das duas fontes deixa propagar a falha da chamada. A regra da SPEC-011 vale para o que a API lança: a `SecurityException` (se a chave deixar de ser legível) é propagada, sem `try`.
- Os motivos 1 (o sinal é o usuário ter ligado o serviço) e 3 (no Robolectric basta `putString`, sem reflexão) continuam valendo. O ADR-019 registra a correção.

### Ressalva sobre o relógio atrasado
O S6 sozinho vale 10 e não tira nenhum app do verde. **Somado a outros sinais, ele muda o nível de alguns apps** (pesos e regras da §5):
- app de loja confiável com S9 (20, verde): com S6 vai a 30, amarelo, porque o S9 tira o teto de 29 (regra 3);
- app de fora da loja com S1 e S3 (50, amarelo): com S6 vai a 60, vermelho, porque tem S1 e um sinal de NFC (regra 4);
- app de fora da loja com S1, S2 e S5 ou S8 (55, amarelo): com S6 vai a 65, vermelho.

O ADR-019 registra as duas coisas. Mudar o comportamento (por exemplo, ignorar horários no futuro) seria outra decisão sua.

### Experimento (clone descartável no scratchpad, 2026-11-20, Robolectric 4.18, APIs 26, 28, 30, 33 e 36)
Funcionou nas cinco APIs, sem reflexão:
1. **Chave não definida:** `Settings.Secure.getString` devolve `null`. O estado volta a `null` no começo de cada teste, então não vaza entre testes.
2. **`putString` e `getString`:** devolvem o texto como foi gravado. Formato completo e curto dão o mesmo `ComponentName` (`com.other.app/.Talk` vira `com.other.app.Talk`).
3. **Entradas malformadas**, com `unflattenFromString`:
   - `""`, `garbage`, `com.pkg/` e `/` voltam `null`;
   - `/OnlyClass` **não** volta `null`: devolve pacote vazio e classe `OnlyClass`. Fica fora porque nenhum app da lista tem pacote vazio;
   - ` com.space/.Svc ` (com espaços) devolve pacote com espaço, que também não casa com nenhum app;
   - `com.a/b/c` devolve pacote `com.a` e classe `b/c`, igual ao que o sistema faria.
4. **String vazia e dois-pontos no fim:** `""` volta `""`; `com.ok/.Svc:` dá uma entrada válida e uma vazia.
5. `putString(key, null)` volta `null`.

O sistema separa o texto do mesmo jeito: `AccessibilityManagerService.readColonDelimitedStringToSet` pula entradas vazias e as que `unflattenFromString` devolve nulas. O inventário segue a mesma regra.

## Approach
- Seguir:
  - o formato dos coletores da SPEC-012 e da SPEC-013 (ADR-017 e ADR-018): `class` pública, `override suspend fun collect`, `companion` privado com o conjunto de resposta, KDoc com o que conta e o que não conta;
  - `skills/android/SKILL.md` › regras puras com o relógio como parâmetro, e Dispatchers in adapters;
  - `skills/android/testing.md` › Adapter Tests — Robolectric (cinco níveis de API) e "inject a `Clock`".
- **S5 e S8:** comparação exata com literais em `const val` privadas, sem `import android.*`.
- **S6:** `RecentInstallOrUpdateCollector(clock: Clock)`, com o parâmetro obrigatório e sem padrão. S6 quando o mais recente entre `firstInstallTime` e `lastUpdateTime` não é anterior a `clock.instant()` menos 7 dias (`Duration.ofDays(7)`, 168 h corridas, sem fuso). O limite é inclusivo, e um horário no futuro conta sem regra a mais.
- **S4, inventário:**
  - depois da consulta de HCE, uma leitura de `Settings.Secure.getString(contentResolver, ENABLED_ACCESSIBILITY_SERVICES)`, no `ioDispatcher`;
  - separa por `:`, pula entradas vazias, passa cada uma por `ComponentName.unflattenFromString`, pula as nulas, agrupa os nomes de classe por pacote;
  - cada app de usuário recebe os seus; pacotes fora da lista (sistema, o próprio app, não instalado, pacote vazio) são ignorados;
  - sem `try`: a `SecurityException` propaga;
  - não cruza com `services`: um serviço ligado conta mesmo que a classe não esteja entre os declarados. É o mesmo critério mais largo do ADR-018.
- **S4, coletor:** `{ACCESSIBILITY_SERVICE_ENABLED}` quando `enabledAccessibilityServices` não está vazio.
- **Testes:** coletores em JUnit 4 puro, com `Clock.fixed` no S6; inventário no Robolectric nos cinco níveis, com `Settings.Secure.putString`; o caso parametrizado das permissões de SMS que não contam lê uma lista escrita à mão no teste.

## Tasks

### BATCH-01: S5, S6 e S8 (PR "SPEC-014 BATCH-01 …", `Refs #10`)
1. [x] **Coletores** (`feat:`): coletor de S5, coletor de S8 e coletor de S6 com `Clock`, com KDoc (o que conta, o que não conta e por quê; ADR-019). `./gradlew build` verde.
2. [x] **Testes dos coletores** (`test:`, papel tester):
   - `anInstalledApp` ganha `firstInstallTime` e `lastUpdateTime`, com os valores de hoje como padrão;
   - S5: com e sem a permissão, maiúsculas, só S5;
   - S8: `READ_SMS`, `RECEIVE_SMS`, as duas, sem nenhuma, maiúsculas, permissões que não contam (parametrizado), só S8;
   - S6: instalado dentro da janela, só a atualização recente, exatamente 7 dias, 7 dias e 1 ms, instalação e atualização no futuro, dois relógios para o mesmo app, só S6.
3. [x] **Conferência dos critérios**, local e desfeita sem commit:
   - trocar o limite inclusivo por exclusivo derruba o teste dos 7 dias exatos;
   - ler só o `firstInstallTime` derruba o teste da atualização recente com instalação antiga;
   - pôr `SEND_SMS` na lista do S8 derruba o caso parametrizado;
   - `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
   - 2026-11-20, local (Windows), cada alteração desfeita sem commit (árvore limpa depois de cada uma), rodando os testes de `core.detection.signal` (66 execuções):
     - `!latest.isBefore(...)` trocado por `latest.isAfter(...)` → 1 falha: `latest exactly seven days ago yields s6` (AC-010).
     - `latest` só com `firstInstallTime` → 2 falhas: `old install with update two days ago yields s6` (AC-009) e `old install with update in the future yields s6` (AC-011).
     - `SEND_SMS` aceito no coletor de S8 → 1 falha: o caso `[android.permission.SEND_SMS]` do parametrizado (AC-006).
     - `./gradlew clean build` verde em 2 min 24 s, sem aviso `w:` do Kotlin e sem diff em `core-data/schemas` (AC-021, AC-022).
       - Testes: `app` 32, `core-data` 17 (2 ignorados no Windows, como já previsto), `core-detection` 177 (151 de antes + 26 novos: 5 + 7 + 5 + 9), `core-domain` 32, `core-nfc` 1, `decoy-app` 1. Total: 260.
       - `LibraryManifestTest` e `MergedManifestTest` passam (sem permissão nova).
       - Kover do `core-domain`: 84 de 85 linhas (98,8%), igual à SPEC-013.
       - Lint sem achados em `app`, `core-data`, `core-detection` e `core-nfc`; o único aviso é o `MissingApplicationIcon` do `decoy-app`, que já existia. A depreciação do Gradle já aparecia na `main`.
   - **Desvios do contrato no BATCH-01:**
     - Na tarefa 2, o detekt recusou a `anInstalledApp` com 7 parâmetros (`LongParameterList` dispara a partir de 6; conferido localmente). Com a aprovação do usuário, a fixture recebeu `@Suppress("LongParameterList")` com KDoc, a exceção do ADR-015 foi estendida pelo ADR-019, e o contrato passou à versão 1.2.
     - Os testes de maiúsculas do S5 guardam a lista em `otherCases`, e o teste do S6 usa o ajudante `collectorAt(now)`: na forma de uma linha, o detekt (`MaxLineLength` de 120) recusaria.
4. [x] **Documentação** (`docs:`):
   - `core-detection/README.md` (coletores de S5, S6 e S8; o `Clock` do S6);
   - `core-domain/README.md` (parâmetros novos da fixture);
   - `SKILLS.md` (API pública e coletores), `project-reference.md`, `context.md`, `glossary.md` (janela de recência);
   - **ADR-019** em `decisions.md`, com as decisões dos quatro sinais e o S4 marcado para o BATCH-02.

### BATCH-02: S4 (PR "SPEC-014 BATCH-02 …", `Closes #10`)
5. [x] **Modelo, inventário e coletor** (`feat:`):
   - `enabledAccessibilityServices` no `InstalledApp`, com cópia que não pode ser alterada, e parâmetro na `anInstalledApp`;
   - leitura e separação do `Settings.Secure` no `PackageManagerAppInventory`;
   - coletor de S4, com KDoc;
   - `./gradlew build` verde.
6. [x] **Testes** (`test:`, papel tester):
   - `InstalledAppTest` com o campo novo;
   - inventário nos cinco níveis: formatos completo e curto, chave nula e vazia, entradas malformadas, pacotes que não se misturam (sistema, próprio app, não instalado);
   - coletor: com e sem serviço ligado, serviço só declarado, só S4.
7. [x] **Conferência dos critérios**, local e desfeita sem commit:
   - tirar o filtro das entradas nulas derruba o teste das malformadas;
   - trocar a chave lida derruba os testes de serviço ligado;
   - dar a cada app os serviços de todos os pacotes (sem agrupar) derruba o teste dos pacotes;
   - `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
   - 2026-11-20, local (Windows), cada alteração desfeita sem commit (árvore limpa depois de cada uma), rodando o `PackageManagerAppInventoryTest` (130 execuções):
     - `mapNotNull` trocado por `map` com `!!` → 5 falhas: `ignores malformed entries and keeps the valid one` nas 5 APIs (AC-017).
     - cada app recebendo os serviços de todos os pacotes → 10 falhas: `keeps enabled services per package and ignores system own and missing packages` e `ignores malformed entries and keeps the valid one` (a entrada `/OnlyClass`, de pacote vazio, iria para o app), nas 5 APIs (AC-018).
     - chave trocada por `Settings.Secure.ACCESSIBILITY_ENABLED` → 15 falhas: os três cenários com serviço ligado, nas 5 APIs (AC-015).
     - `./gradlew clean build` verde em 2 min 19 s, sem aviso `w:` do Kotlin e sem diff em `core-data/schemas` (AC-021, AC-022).
       - Testes: `app` 32, `core-data` 17 (2 ignorados no Windows; só rodam no CI), `core-detection` 201 (177 de antes + 20 do inventário, 4 cenários × 5 APIs, + 4 do coletor), `core-domain` 32 (3 alterados), `core-nfc` 1, `decoy-app` 1. Total: 284.
       - `LibraryManifestTest` e `MergedManifestTest` passam (sem permissão nova).
       - Kover do `core-domain`: 86 de 87 linhas (98,9%).
       - Lint sem achados em `app`, `core-data`, `core-detection` e `core-nfc` (a leitura do `Settings.Secure` não gera aviso); o único aviso é o `MissingApplicationIcon` do `decoy-app`, que já existia.
   - **Desvios do contrato no BATCH-02:**
     - Na tarefa 5, o `InstalledAppTest` ganhou só o argumento `enabledAccessibilityServices = emptySet()` para compilar (a fábrica passou a exigi-lo); as asserções vieram na tarefa 6, como na SPEC-013.
     - O teste "sem serviço ligado" do coletor guarda o app numa variável: na forma de uma linha, o detekt (`MaxLineLength` de 120) recusaria.
     - Na documentação, além dos arquivos do contrato, `.agent/architecture.md` também citava o `AccessibilityManager` como fonte do S4 (duas linhas) e foi corrigido, para não contradizer o ADR-019.
8. [x] **Documentação** (`docs:`):
   - READMEs do `core-detection` (inventário em quatro etapas, coletor de S4, sem `<queries>` para o S4) e do `core-domain` (campo novo);
   - §8 de `docs/especificacao.md` (linhas "Inventário" e "S4 acessibilidade");
   - `SKILLS.md` (Settings no Robolectric), `project-reference.md`, `context.md`, `glossary.md`;
   - ADR-019: S4 implementado.

## Tamanho: cerca de 600 linhas, em dois batches

| Batch | Conteúdo | Código | Testes | Total |
|---|---|---|---|---|
| **BATCH-01** | Coletores de S5, S6 e S8, parâmetros da fixture e testes | ~100 | ~260 | **~360** |
| **BATCH-02** | S4: campo, leitura no inventário, coletor e testes | ~70 | ~170 | **~240** |

O BATCH-02 foi refeito com o `Settings.Secure`. Sai o ajudante de reflexão que o `AccessibilityManager` exigia nos testes. Entram o teste das entradas malformadas, o da chave nula e vazia e a separação do texto no inventário. A documentação não entra na conta (regra do SDD 1.8.0).

Depois da revisão do architect, a conta pelos arquivos do contrato deu 624 (393 e 231). A spec passou a 1.1, com `estimate-lines: 625` e batches de 395 e 230.

### Revisão do architect (2026-11-20, emenda 1.1 da spec)
- Campo `enabledAccessibilityServices`, obrigatório, depois de `cardEmulationServices`; a fixture tem padrão vazio.
- Coletores públicos `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector(clock: Clock)` e `EnabledAccessibilityServiceCollector`, todos `class`. A janela de 7 dias, as listas de permissões e a separação da chave ficam privadas; nenhum `internal` novo.
- Inventário em quatro etapas; documentação de cada batch no próprio PR; ADR-019 criado no BATCH-01.
- Respostas do usuário (2026-11-20): o BATCH-02 vai na branch `spec-014-sinais-controle-batch-02`, criada a partir da `main` depois do merge do BATCH-01; o S4 não cruza com `services` (critério mais largo confirmado); "futuro conta" fica no S6, com os exemplos de mudança de nível no ADR-019; o ADR-019 registra a correção do motivo 2 e que a troca de fonte se mantém pelos motivos 1 e 3.
- Aprovados pelo usuário: o plano, a revisão do architect, a emenda 1.1 e a divisão da documentação entre os batches.
- Detalhes em `.agent/plans/spec-014-sinais-controle-architecture.md`.

## Database changes
Nenhuma. `app_snapshot` não guarda serviços nem relógio. S4, S5, S6 e S8 vão nos bits 3, 4, 5 e 7 de `signals` pela SPEC-018, e o `1.json` não muda (ADR-014).

## Risks & open questions
- **Falha do provedor de configurações:** o `Settings.Secure` devolve `null`, e o S4 some nessa varredura sem erro, igual a "nenhum serviço ligado". A API não expõe essa falha (ver a correção acima).
- **Relógio atrasado:** todos os apps ganham S6. Somado a outros sinais, o S6 muda o nível de alguns apps (ver a ressalva acima).
- **S6 e atualizações automáticas:** toda atualização da loja dispara o S6. Um app de loja confiável fica limitado a 29 pela regra 3, a não ser que tenha S7 ou S9.
- **Serviço ligado que o app não declara mais:** conta, porque o inventário não cruza com `services` (falso positivo raro, como no ADR-018; confirmado pelo usuário).
- **Robolectric:** não aplica o `@Readable`. A legibilidade vem das fontes do AOSP, e a conferência em aparelho fica para a SPEC-022.
- **Branch do BATCH-02 (decidido):** `spec-014-sinais-controle-batch-02`, criada a partir da `main` depois do merge do BATCH-01.

## Acceptance criteria
- [ ] S5 com `SYSTEM_ALERT_WINDOW`; sem ela, não; comparação exata (AC-001 a AC-003)
- [ ] S8 com `READ_SMS` ou `RECEIVE_SMS`; sem elas, ou só com as que não contam, não; comparação exata (AC-004 a AC-007)
- [ ] S6 dentro da janela, pela instalação ou pela atualização; limite inclusivo; futuro conta; relógio injetado (AC-008 a AC-012)
- [ ] Cada coletor só devolve o seu sinal ou nada (AC-013)
- [ ] S4 pelo campo novo; inventário com formatos completo e curto, chave nula ou vazia, malformadas ignoradas e pacotes que não se misturam (AC-014 a AC-018)
- [ ] Campo novo copiado e que não pode ser alterado; cobertura do `core-domain` acima de 80% (AC-019)
- [ ] Sem API do Android nos coletores; sem permissão nova; schema do Room sem diff (AC-020, AC-021)
- [ ] ktlint, detekt, lint e testes passam em cada batch (AC-022)
- [ ] Documentação de cada batch, com o ADR-019 e a §8 (AC-023)
- [ ] CI de cada PR verde (AC-024)
