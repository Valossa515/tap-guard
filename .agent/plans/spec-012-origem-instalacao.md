# Plan: SPEC-012 Origem da instalação (S1)

> Issue #8 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-011 (#7, fechada) · Especificação RF-102, §5 (S1, regra 3, lista de lojas) e §7 · ADR-015, ADR-016
> Spec: `.agent/specs/features/spec-012-origem-instalacao.spec.toon` (FT-012 v1.1, `interface: library`) · Aceite: `.agent/specs/acceptance/spec-012-origem-instalacao.acceptance.toon`
> Arquitetura: `.agent/plans/spec-012-origem-instalacao-architecture.md` · Contrato: `.agent/specs/contracts/spec-012-origem-instalacao.contract.toon`
> Branch: `spec-012-origem-instalacao` · Planejado em 2026-11-18

## Summary
Implementar o coletor do sinal S1 (instalado fora de uma loja confiável) no `core-detection`. Ele compara `InstalledApp.installerPackageName` com a lista de lojas confiáveis e devolve `{UNTRUSTED_INSTALL_SOURCE}` quando o instalador é nulo ou está fora da lista. Quando está na lista, devolve um conjunto vazio. Não chama o `PackageManager`: o `AppInventory` (SPEC-011) já lê o instalador nos dois caminhos de API.

Esta spec também entrega o arquivo da lista de lojas, no formato que a SPEC-015 vai seguir para carteiras e bancos. Além disso, passa as fixtures do domínio para `testFixtures` do `core-domain`, para o `core-detection` usar.

## Critério da issue atualizado
"Os dois caminhos de API cobertos por teste" passa a ser **cumprido pela SPEC-011**: AC-007 (instalador por `setInstallerPackageName` nas APIs 26 e 28 e por `setInstallSourceInfo` nas APIs 30, 33 e 36) e AC-008 (instalador ausente devolve `null`), no `PackageManagerAppInventoryTest`. A decisão está no comentário da issue #8 (2026-11-18). Aqui o critério vira AC-010, conferido pelo build.

## Scope
- **Módulos afetados:**
  - `core-detection`: coletor, lista de lojas e testes;
  - `core-domain`: só build e fixtures (nenhuma mudança em `src/main`);
  - documentação.
- **Novos arquivos (esperados; nomes e pacote no contrato):**
  - coletor de S1 e lista de lojas no `core-detection`;
  - teste do coletor (JUnit 4, sem Robolectric);
  - `core-domain/src/testFixtures/.../DomainFixtures.kt` (movido de `src/test`).
- **Arquivos modificados:**
  - `core-domain/build.gradle.kts`: plugin `java-test-fixtures`, `testFixtures` no detekt, fora do Kover;
  - `core-detection/build.gradle.kts`: `testImplementation(testFixtures(project(":core-domain")))`;
  - `core-detection/README.md`, `core-domain/README.md`;
  - `docs/especificacao.md` (§5, lista de lojas);
  - `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-017), `.agent/project-reference.md`, `.agent/conventions.md`, `.agent/context.md`, `.agent/glossary.md`, `.agent/backlog_rules.md`;
  - este plano (marcar as tarefas).
- **Fora do escopo:**
  - `initiatingPackageName` e confiar no instalador só se ele for app do sistema (issue pós-MVP, aberta neste planejamento);
  - listas de carteiras e bancos e o coletor de S9 (SPEC-015);
  - ligação no Hilt e uso no `ScanDevice` (SPEC-018);
  - limite da regra 3 (SPEC-017).

## Decisões já tomadas (usuário, 2026-11-18)

| Item | Decisão |
|------|---------|
| Local do coletor | `core-detection`. A §7 põe os coletores no `core-detection` e só as interfaces no `core-domain`. A regra "nada de lógica de score fora do `core-domain`" vale para o score; o S1 só detecta. Para o SDK não muda nada: o `core-detection` já expõe o `core-domain` como `api`. Todos os coletores ficam juntos com o `ScanDevice` |
| Lista de lojas | Google Play `com.android.vending`; Galaxy Store `com.sec.android.app.samsungapps`; Xiaomi GetApps `com.xiaomi.mipicks` e `com.xiaomi.market`; OPPO App Market `com.heytap.market` e `com.oppo.market`; vivo V-Appstore `com.vivo.appstore`; HONOR App Market `com.hihonor.appmarket`; Palm Store `com.transsnet.store`; Huawei AppGallery `com.huawei.appmarket`; Amazon Appstore `com.amazon.venezia`. As variantes da Xiaomi e da OPPO evitam falso positivo em celulares Xiaomi e realme. A Palm Store está na lista de verificação de desenvolvedores do Google |
| Fora da lista | Instaladores de pacote do sistema (`com.google.android.packageinstaller`, `com.android.packageinstaller` e equivalentes de fabricante, como `com.miui.packageinstaller`), porque são o que aparece num APK instalado à mão; F-Droid (o cliente aceita repositórios de terceiros); Aurora Store (cliente não oficial da Play, aparece como instaladora de tudo o que baixa); apps de clonagem (copiam também um APK de golpe); `com.motorola.installer` (função não confirmada) e lojas chinesas sem fonte de uso no Brasil |
| Amazon Appstore | Entra. Está na §5; a loja fechou em celulares Android comuns em 20/08/2025 e segue nos aparelhos Fire. Os apps instalados antes ainda mostram esse instalador |
| Risco de app ruim em loja confiável | Vem do limite da regra 3 (score até 29 sem S7 ou S9), que não é desta spec. Registrado no ADR-017 |
| Extensão pelo SDK | O coletor recebe o conjunto inteiro de instaladores confiáveis na construção. A lista padrão é pública: o banco usa a padrão mais as lojas dele, ou uma lista menor |
| Onde a lista mora | Arquivo próprio, só com as lojas, no `core-detection`, com nome, pacote e fonte em comentário por item. A SPEC-015 segue o mesmo formato e a mesma pasta para carteiras e bancos (um arquivo por tipo). Comentário publicado na #11 |
| Comparação | Exata, com maiúsculas e minúsculas (o nome de pacote diferencia as duas); sem `trim` |
| Instalador nulo | Gera S1 (issue #8). Acontece com `adb install` e quando o instalador foi desinstalado (documentação de `InstallSourceInfo`) |
| Nome de pacote emprestado | Limitação no ADR-017: o app que instala o golpe recebe S1; se ele se desinstalar, o instalador do golpe vira nulo e também gera S1. Issue pós-MVP "Confiar no instalador só se ele for app do sistema" |
| Apps de clonagem | Fora da lista. Risco no ADR-017 e na #11: um app de banco copiado para um celular novo pode ficar vermelho; quem resolve é a lista de bancos conhecidos da SPEC-015. Conferência em aparelho real no teste ponta a ponta |
| Fixtures | `anInstalledApp` e `aRiskResult` vão para `core-domain/src/testFixtures` (plugin `java-test-fixtures`). O `core-detection` as usa com `testImplementation(testFixtures(project(":core-domain")))`. `anInstalledApp` ganha só o parâmetro `installerPackageName` |
| detekt e Kover no `testFixtures` | `detekt { source.from(files("src/testFixtures/kotlin")) }` e `kover { currentProject { sources { excludedSourceSets.add("testFixtures") } } }` |
| Testes | JUnit 4 no `core-detection`, sem Robolectric e sem `@Config(sdk)` (não há ramo por versão). Um caso por loja (parametrizado), nulo, fora da lista, instaladores do sistema |

### Pesquisa (fontes)
- **Lojas da verificação de desenvolvedores** (em vigor desde 30/09/2026 no Brasil, Indonésia, Singapura e Tailândia): Google Play, HONOR App Market, OPPO App Market, Galaxy Store, Palm Store, vivo V-Appstore e Xiaomi GetApps. [Android Developers Blog, jun/2026](https://android-developers.googleblog.com/2026/06/android-developer-verification.html); [developer.android.com/developer-verification](https://developer.android.com/developer-verification).
- **Pacotes:**
  - [Talsec, Installation Sources Cookbook](https://docs.talsec.app/appsec-articles/glossary/sideloading/installation-sources-cookbook): lista todos os pacotes acima, menos `com.hihonor.appmarket`, mais os instaladores do sistema e os apps de clonagem;
  - HONOR App Market: [APKMirror](https://apkmirror.com/apk/honor/app-market-2/app-market-2-16-0-4-302-release/app-market-16-0-4-302-android-apk-download);
  - Galaxy Store: [bayton](https://sysapps.bayton.org/packages/com.sec.android.app.samsungapps);
  - `com.heytap.market`: [bayton](https://sysapps.bayton.org/packages/com.heytap.market);
  - GetApps como `com.xiaomi.market`: [APKMirror, GetApps 4.88](https://apkmirror.com/apk/xiaomi-inc/xiaomi-market-2/xiaomi-getapps-4-88-0-release/xiaomi-getapps-4-88-0-android-apk-download);
  - Amazon Appstore: [Stack Overflow](https://kiwix.lema.org/stackoverflow.com_en_all_2023-11/a/13293534).
- **Motorola:** não há fonte de loja própria no Brasil. `com.motorola.installer` ("Moto Installer") tem `INSTALL_PACKAGES`, mas a função não é descrita ([bayton](https://sysapps.bayton.org/packages/com.motorola.installer)).
- **Amazon:** a loja fechou em celulares Android comuns em 20/08/2025 ([9to5Google](https://9to5google.com/2025/02/20/amazon-appstore-android-shutting-down-2025/)).
- **Instalador falsificado:**
  - `setInstallerPackageName` lança `SecurityException` se o novo instalador não tiver o certificado de quem chama, ou se o atual for de outro certificado (fontes do SDK 34): um app comum não consegue se passar pela Play;
  - com shell isso é possível: `pm install -i com.android.vending` via adb ([GrapheneOS](https://discuss.grapheneos.org/d/24574-bank-app-doesnt-work-google-services-mirror-instaled-pixel-8pro)), e apps com Shizuku, como o Universal Installer, com a opção "Set Install Source" ([IzzyOnDroid](https://apt.izzysoft.de/packages/app.pwhs.universalinstaller/));
  - com root, o módulo AlterInstaller edita o registro de pacotes ([AlterInstaller](https://awesome.ecosyste.ms/projects/github.com%2Fchenxiaolong%2FAlterInstaller));
  - não foi achado relato de golpe de NFC que use essas técnicas;
  - `getInitiatingPackageName` (API 30+) não muda com `setInstallerPackageName` ([InstallSourceInfo](https://developer.android.com/reference/android/content/pm/InstallSourceInfo)).
- **Nome de pacote emprestado:** dedução, sem fonte. O nome de pacote é único por aparelho, então num aparelho sem uma das lojas um app comum pode usar o pacote dela e instalar outros apps.

### Experimento (clone descartável em `%TEMP%`, 2026-11-18)
Fixtures movidas para `core-domain/src/testFixtures`, com `java-test-fixtures`. O `core-detection` passou a usá-las num teste parametrizado com JUnit 4 sobre um coletor provisório.
1. **Compilação e testes:** ok, com 32 testes no `core-domain` e 2 casos parametrizados no `core-detection`.
2. **ktlint:** já cobre o `testFixtures`. Uma violação de propósito derrubou `ktlintTestFixturesSourceSetCheck`.
3. **detekt:** a tarefa `detekt` (a do `check`) **não analisava** o `testFixtures`; ignorou `MagicNumber` e `EmptyFunctionBlock` colocados lá. Só a `detektTestFixtures` (experimental, com resolução de tipos) o veria. Com `source.from(files("src/testFixtures/kotlin"))`, a `detekt` falhou como deveria.
4. **Kover:** contava `DomainFixturesKt` na cobertura. Com `excludedSourceSets.add("testFixtures")`, o relatório sai sem as fixtures e volta aos 98,8% de antes (82/83 linhas).
5. **`./gradlew clean build`:** verde em 4 min 07 s com as duas correções, sem aviso `w:`.
6. **`MagicNumber` no `testFixtures`:** vale ali (o padrão do detekt só o desliga em `test`). Hoje não afeta nada: as fixtures não têm número mágico.

## Approach
- Seguir `skills/android/SKILL.md`: implementação de porta do `core-domain` no `core-<capacidade>`, valores ajustáveis num arquivo só (Configuration), sem API do Android onde não é preciso. Seguir também `skills/android/testing.md`: JUnit 4 em módulo Android; Test Fixtures no `testFixtures` do `core-domain`; nomes com crases.
- O coletor é Kotlin puro dentro do `core-detection`: sem `PackageManager`, sem `Context`, sem dispatcher (não bloqueia).
- O conjunto recebido na construção é copiado. Mudar depois o conjunto de quem chamou não altera o coletor.
- O teste parametrizado usa uma lista literal no próprio teste, não a lista de produção: tirar uma loja da produção derruba o caso dela. Um teste à parte confere que a lista padrão tem exatamente as 11 entradas, para que acrescentar uma seja sempre de propósito.

## Tasks
1. [ ] **Fixtures** (`chore:`):
   - `java-test-fixtures` no `core-domain`;
   - `DomainFixtures.kt` para `src/testFixtures`, com `installerPackageName` na `anInstalledApp`;
   - `testFixtures` no detekt e fora do Kover;
   - dependência `testFixtures` no `core-detection`;
   - `./gradlew build` verde.
2. [ ] **Lista e coletor** (`feat:`): arquivo da lista de lojas com nome, pacote e fonte por item; o coletor de S1 com o conjunto na construção e a lista padrão pública; KDoc com o que gera S1 e as limitações (ADR-017).
3. [ ] **Testes** (`test:`, papel tester):
   - um caso por loja (parametrizado);
   - lista padrão com exatamente as 11 entradas;
   - nulo;
   - fora da lista (`com.example.store`, `org.fdroid.fdroid`, `com.aurora.store`, `com.sec.android.easyMover`);
   - instaladores do sistema (`com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.miui.packageinstaller`, `com.android.shell`);
   - maiúsculas;
   - só S1 (nunca outro sinal);
   - lista ampliada e lista menor;
   - cópia do conjunto recebido.
4. [ ] **Documentação** (`docs:`):
   - `core-detection/README.md`: coletor, lista, como o SDK amplia, limitações;
   - `core-domain/README.md`: fixtures em `testFixtures`;
   - §5 de `docs/especificacao.md`, `glossary.md` e `backlog_rules.md`: lista de lojas;
   - `SKILLS.md`: classes públicas do `core-detection`, fixtures em `testFixtures`, detekt e Kover no `testFixtures`, lista de lojas;
   - `project-reference.md`, `conventions.md`, `context.md`;
   - **ADR-017** em `decisions.md`.
5. [ ] **Conferência dos critérios**, localmente e desfeita sem commit:
   - tirar uma loja da lista derruba o caso dela e o teste das 11 entradas;
   - tratar nulo como confiável derruba o teste do nulo;
   - uma violação do detekt no `testFixtures` derruba o `./gradlew build`;
   - `./gradlew build` verde sem aviso novo.

### Feito no planejamento (2026-11-18)
- [Comentário na #11](https://github.com/Valossa515/tap-guard/issues/11#issuecomment-6989480523): o arquivo de lojas já existe e a SPEC-015 segue o mesmo formato; o risco dos apps de clonagem.
- Issue pós-MVP #57 "Confiar no instalador só se ele for app do sistema" (labels `spec` e `core-detection`, sem milestone).

## Database changes
Nenhuma. O instalador já vai para `app_snapshot.installer`, e o S1, para `app_snapshot.signals` (bit 0), pela SPEC-018.

## Risks & open questions
- **Instalador falsificado com shell ou root** (adb, Shizuku, AlterInstaller): S1 não aparece. Exige depuração ligada ou root; sem relato de uso em golpe de NFC. Segunda defesa (`initiatingPackageName`) na issue pós-MVP.
- **Nome de pacote emprestado:** dedução sem fonte; análise no ADR-017; issue pós-MVP.
- **Apps de clonagem:** app de banco copiado para celular novo pode ficar vermelho (S1 + S2 + S3 = 65). Resolução na SPEC-015; conferência em aparelho real no teste ponta a ponta.
- **Pacotes com fonte de terceiros** (`com.vivo.appstore`, `com.transsnet.store`, `com.xiaomi.market`, `com.oppo.market`): sem documento oficial do fabricante. Conferir em aparelho real quando houver.
- **Instalador registrado numa instalação à mão a partir da API 30:** não confirmado se é o instalador do sistema ou o app que abriu o APK (navegador, WhatsApp). Nos dois casos fica fora da lista e gera S1.
- **Lista vazia** passada pelo SDK: todo app recebe S1. Comportamento documentado no KDoc, sem validação (a especificação não define).

## Acceptance criteria
- [ ] Cada loja da lista não gera S1, um caso por pacote (AC-001)
- [ ] Instalador nulo, fora da lista ou instalador de pacote do sistema gera S1; comparação exata (AC-002 a AC-005)
- [ ] O coletor só devolve S1 ou nada (AC-006)
- [ ] Lista ampliada, lista menor e cópia do conjunto recebido (AC-007 a AC-009)
- [ ] Dois caminhos de API cobertos pela SPEC-011 (AC-010)
- [ ] Lista padrão com as 11 entradas e fontes (AC-011)
- [ ] Fixtures em `testFixtures`, com detekt e fora do Kover (AC-012 a AC-014)
- [ ] Sem API do Android e sem `PackageManager` no coletor (AC-015)
- [ ] ktlint, detekt, lint e testes passam (AC-016)
- [ ] Documentação atualizada, com o ADR-017 (AC-017)
- [ ] CI da PR verde (AC-018)
