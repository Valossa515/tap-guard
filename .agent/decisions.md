# Architecture Decision Records

> Decisões já tomadas na especificação (`docs/especificacao.md`, 13/11/2026). Ainda não implementadas: status **(planned)**. Alternativas só aparecem quando a especificação as cita.

## ADR-001 — Tudo local, sem backend e sem `INTERNET`
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o produto pede confiança de um público leigo e lida com dados de cartão.
- **Decision:** o app não tem backend, não pede cadastro, não envia dados e não declara a permissão `INTERNET`. Sem login, telemetria ou anúncios.
- **Consequences:** a ausência de `INTERNET` no manifesto prova que nada sai do aparelho; um teste automatizado falha se ela aparecer. Listas de lojas, carteiras e bancos ficam embutidas no app. Uma lista de ameaças atualizável (evolução futura) exigirá rever o RNF-01.

## ADR-002 — Defesa por perfil estático e educação, não por interceptação
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o Android não permite a um app observar ou bloquear o uso de NFC por outro app, nem medir se uma leitura está sendo retransmitida.
- **Decision:** detectar por sinais estáticos do app instalado (S1–S9) e ensinar o usuário (F2, F4).
- **Alternatives citadas e excluídas do MVP:** alerta em tempo real pelo app em primeiro plano (exige acesso a uso de apps ou acessibilidade); análise de código de APK, assinaturas de malware ou serviços de reputação; bloqueio/desinstalação automática.
- **Consequences:** risco de falsos negativos (golpe sem HCE nem acessibilidade fica amarelo) e falsos positivos (apps legítimos por APK), mitigados pelas regras de ajuste e pela opção "confiável".

## ADR-003 — Score por soma de pesos com regras de ajuste
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** é preciso classificar apps de forma explicável e calibrável.
- **Decision:** soma dos pesos dos sinais presentes, limitada a 100, ajustada por quatro regras em ordem (sistema ignorado; confiável sempre verde; loja confiável limitada a 29 salvo S7/S9; vermelho exige S1 + sinal de NFC). Pesos em um único arquivo de configuração.
- **Consequences:** regras documentadas no repositório (RNF-09); cinco exemplos da especificação viram testes unitários. Detalhes em `backlog_rules.md`.

## ADR-004 — Monorepo Gradle de seis módulos com domínio puro
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** a detecção deve virar SDK para bancos sem reescrita, e o score deve ser testável sem Android.
- **Decision:** módulos `core-domain` (Kotlin puro), `core-detection`, `core-nfc`, `core-data`, `app`, `decoy-app`. Nada de lógica de score fora do `core-domain`; nada de API do Android dentro dele; `core-domain` e `core-detection` não dependem do `app`.
- **Consequences:** coletores ficam atrás de interfaces (`AppInventory`, `SignalCollector`, `CardReader`); cobertura de 80% no domínio é viável com JUnit 5 puro.

## ADR-005 — Stack Kotlin + Compose + Hilt
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** Kotlin; Gradle Kotlin DSL com catálogo de versões; Jetpack Compose, Material 3, Navigation Compose; corrotinas e Flow; Hilt; um ViewModel por tela com estado como fluxo imutável.
- **Consequences:** versões do build e da toolchain fixadas no ADR-011; as das bibliotecas de produção (Compose, Hilt etc.) entram nas specs que as usam.

## ADR-006 — Room para histórico, DataStore para preferências
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** Room com as tabelas `scan_run`, `app_snapshot`, `user_decision`, `alert_event`; DataStore com `onboardingDone`, `monitoringEnabled`, `scanIntervalHours`, `lastDefaultPaymentComponent`. Retenção: 10 varreduras, 90 dias de alertas.
- **Consequences:** nenhum dado de cartão entra no banco nem nas preferências.

## ADR-007 — Vigia com WorkManager periódico
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** RNF-04 proíbe serviço permanente em primeiro plano; receptor de instalação no manifesto não é confiável desde o Android 8.
- **Decision:** `PeriodicWorkRequest` (padrão 6 h; opções 1, 6, 12, 24) compara com a última varredura gravada. Receptor em tempo de execução só como complemento com o app aberto; varredura também ao abrir o app.
- **Consequences:** a notificação de um app novo chega no próximo ciclo, não na hora; fabricantes podem atrasar a vigia.

## ADR-008 — Dados de cartão só em memória e mascarados no `core-nfc`
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o teste do cartão não pode virar ferramenta de leitura de cartão nem vazar dados.
- **Decision:** modo leitor só com T6 visível; número mascarado no `core-nfc` (a interface recebe só os 4 últimos dígitos); nada em disco, log ou análise de falhas; tela marcada como segura; nada exportado.
- **Consequences:** teste automatizado procura o número completo sintético em banco, preferências e logs.

## ADR-009 — Testes com app isca próprio, nunca malware real
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** `decoy-app` inofensivo, sem `INTERNET`, com variantes `nfcOnly`, `hce`, `full`, `lookalike`, instalado por `adb install`.
- **Consequences:** a Etapa 2 entrega a isca antes da interface; testes ponta a ponta usam emulador no CI e aparelho físico.

## ADR-010 — Permissões mínimas
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** declarar apenas `QUERY_ALL_PACKAGES`, `NFC` e `POST_NOTIFICATIONS`; recurso NFC com `required=false`.
- **Consequences:** `QUERY_ALL_PACKAGES` exige declaração de app de segurança na Play Store; se recusada, distribuição pelo GitHub (o README deve explicar a contradição de instalar por APK).

## ADR-011 — Identificador do pacote e combinação de versões do build
- **Date:** 2026-11-16
- **Status:** Aceita (implementada na SPEC-001)
- **Context:** a SPEC-001 cria o monorepo Gradle e precisa do identificador do pacote (decisão em aberto na seção 13 da especificação) e de uma combinação de Kotlin, AGP, Gradle e JDK que compile, rode o lint do Android, o ktlint e o detekt.
- **Decision:**
  - Identificador base `io.github.valossa515.tapguard`. applicationId e namespace do `app` = `io.github.valossa515.tapguard`; do `decoy-app` = `io.github.valossa515.tapguard.decoy`. Namespaces das bibliotecas: `io.github.valossa515.tapguard.core.domain`, `.core.detection`, `.core.nfc`, `.core.data`.
  - Kotlin 2.4.21 + AGP 9.3.3 + Gradle 9.7.0 + JDK 21. minSdk 26, targetSdk 36, compileSdk 36. ktlint-gradle 14.2.0 com motor 1.8.0; detekt 1.23.8; JUnit 5.14.4 no `core-domain`, JUnit 4.13.2 nos módulos Android.
  - AGP 9.3.3 fica um patch fora da tabela de compatibilidade do Kotlin 2.4.20–2.4.21 (que vai até 9.3.1). 9.3.2 e 9.3.3 são correções da mesma linha; a 9.3.2 corrige um crash do lint no JDK 17. O teste de compatibilidade da combinação passou em compilação, D8, lint, ktlint e detekt.
  - Nos módulos Android, Kotlin embutido do AGP 9, sem o plugin `org.jetbrains.kotlin.android`.
  - JDK 21 em vez do 25 (fora do plano original): o detekt 1.23.8 roda no processo do Gradle e o compilador Kotlin embutido nele não reconhece o JVM 25 (`IllegalArgumentException: 25`). O JDK 21 é o toolchain de todos os módulos, o JDK do CI e o JDK do daemon do Gradle.
  - `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) escrito à mão (fora do plano original): o `./gradlew updateDaemonJvm` do Gradle 9.7.0 exige um repositório de download de JDK (plugin foojay), que o projeto não quer configurar. Verificado em 2026-11-16 com `JAVA_HOME` no JDK 25: o daemon roda no Temurin 21 e `./gradlew help` passa.
  - Cada módulo declara a própria configuração com o catálogo; sem `build-logic` nem plugins de convenção.
- **Alternatives:** JDK 25 (detekt falha); tirar o detekt da SPEC-001 e adiá-lo para a SPEC-002 (desnecessário com o JDK 21); gerar o arquivo do daemon com `updateDaemonJvm` + foojay (rejeitado: baixaria JDKs da internet durante o build).
- **Consequences:** quem contribui precisa de um JDK 21 instalado, mesmo usando outro JDK no dia a dia (documentado no `CONTRIBUTING.md`). Trocar o JDK ou subir o detekt exige rever este ADR. O identificador sai da lista de decisões pendentes.

## ADR-012 — CI: cobertura do domínio, validação do wrapper e APK de debug
- **Date:** 2026-11-16
- **Status:** Aceita (implementada na SPEC-002)
- **Context:** a Etapa 0 pede um CI que compile, teste e publique o APK de debug. A SPEC-001 já deixou o `./gradlew build` (ktlint, detekt, lint e testes) no job "Build e testes", e a ruleset da `main` exige os checks "Regras do projeto" e "Build e testes". Faltavam a cobertura de 80% no domínio (RNF-08), a validação do `gradle-wrapper.jar` e o artefato. O `core-domain` ainda não tem código de produção.
- **Decision:**
  - Kover 0.9.11 só no `core-domain`, com regra de no mínimo 80% de cobertura de linhas, **ligada desde já**. Num projeto descartável (Gradle 9.7.0, Kotlin 2.4.21, JDK 21), o `koverVerify` passou sem erro com zero classes (0/0, "No sources") e com só uma interface; passou com uma classe 100% coberta; falhou com 20% e com uma `data class` sem teste. Numa cópia do repositório, `./gradlew build` passou com o Kover e falhou com uma classe sem teste. A regra passa corretamente sem código e começa a valer sozinha na primeira classe com código executável.
  - `gradle/actions/wrapper-validation` v6.4.1 no job "Build e testes", antes de qualquer execução do `gradlew`. A v6 tornou proprietário o componente de cache (`gradle-actions-caching`, Termos de Uso da Gradle); a `wrapper-validation` não o usa.
  - `actions/upload-artifact` v7.0.2 publica só o APK de debug do `app` (artefato `app-debug`), depois de um build verde, falhando se o arquivo não existir.
  - Actions novas referenciadas pelo SHA do commit da versão, com a versão em comentário: `gradle/actions` `8da46b539cd8fe6ab27ca75128919a18d757604c` (v6.4.1), `actions/upload-artifact` `cf430e030ddbb5b0abf93d22962f4752f3646cd9` (v7.0.2). SHAs conferidos na página de releases, na API do GitHub e no `git ls-remote`.
  - Passos novos dentro dos jobs existentes, que já são checks obrigatórios; sem job novo e sem mudança na ruleset.
- **Alternatives:** configurar o Kover agora e ligar os 80% só na SPEC-010 (rejeitada: dois PRs e risco de modelos entrarem sem teste); criar código de produção só para ter o que medir (rejeitada); Kover em todos os módulos (rejeitada: sem meta de cobertura neles e bug aberto `Kotlin/kotlinx-kover#815` com AGP 9); `gradle/actions` v5.0.2, só MIT (rejeitada: sem manutenção); validação do wrapper num job separado (rejeitada: rodaria em paralelo com o build, que já executaria o JAR, e não seria check obrigatório); publicar também o APK do `decoy-app` e o relatório de cobertura (adiado até haver uso).
- **Consequences:** enquanto o `core-domain` não tiver código, a regra passa sem medir; uma configuração quebrada também mostraria "No sources", por isso a verificação negativa (classe temporária sem teste derruba o build) faz parte da conferência da spec. A SPEC-010 já nasce sob a regra: os modelos (`data class`) precisam de testes. O Kover 0.9.11 tem problema conhecido com o configuration cache do Gradle, que o projeto não liga; ligar exige rever. `actions/checkout` e `actions/setup-java` continuam em `@v4` por tag.

## ADR-013 — compileSdk separado do targetSdk
- **Date:** 2026-11-17
- **Status:** Aceita (implementada na SPEC-003)
- **Context:** o ADR-011 fixou targetSdk e compileSdk em 36. Na SPEC-003, o Compose 1.12 (BOM 2026.09.00), o `navigation-compose` 2.10.2 e o `lifecycle-*-compose` 2.11.0 passaram a exigir compileSdk ≥ 37: com 36, o `:app:checkDebugAarMetadata` recusa 12 artefatos. O targetSdk segue a exigência da Play Store, que hoje é 36.
- **Decision:** **compileSdk 37**, **targetSdk 36**, minSdk 26, para todos os módulos Android (catálogo único). Plataforma `platforms;android-37.0`, conferida como estável no catálogo oficial do SDK (`repository2-3.xml`: canal stable, número sem codinome); as prévias `37.2-beta*` foram descartadas. Nesta máquina, o AGP a instalou no primeiro build (sem `sdkmanager`); no CI, o runner a baixa da mesma forma.
- **Alternatives:** manter compileSdk 36 e voltar Compose BOM, Navigation e Lifecycle para versões mais antigas (rejeitada: versões já conferidas e testadas; compileSdk não muda o comportamento em execução); subir também o targetSdk para 37 (rejeitada: o targetSdk segue a Play Store e muda o comportamento em execução).
- **Consequences:** compileSdk e targetSdk deixam de andar juntos. O compileSdk sobe quando uma biblioteca exigir; o targetSdk sobe quando a Play Store exigir, com teste das mudanças de comportamento. Quem contribui precisa da plataforma 37 no SDK. O ADR-011 continua válido em todo o resto.

## ADR-014 — Base de persistência: Room 2 por KSP, colunas da especificação, backup desligado e regra de schema
- **Date:** 2026-11-17
- **Status:** Aceita (implementada na SPEC-004)
- **Context:** a SPEC-004 cria o banco (seção 9), as preferências e o Hilt. É a primeira spec com KSP, Room e DataStore. O app guarda histórico de apps e decisões e não pode deixar nada sair do aparelho (RNF-01).
- **Decision:**
  - **Room 2.8.5** (`androidx.room`), não o Room 3 (`androidx.room3`). Room e Hilt **só por KSP** 2.3.14; o kapt é incompatível com o Kotlin embutido do AGP 9. Hilt 2.60.1, DataStore 1.3.0. Combinação verificada num projeto descartável com Kotlin 2.4.21, AGP 9.3.3, Gradle 9.7.0 e JDK 21.
  - **Colunas com os nomes da especificação** (camelCase), `id` `Long` autoincremento, datas em milissegundos, enums como texto, `level` `GREEN`/`YELLOW`/`RED`. Diverge da skill de banco (`snake_case`, UUID, `created_at`/`updated_at`). `app_snapshot` com chave `scanId` + `packageName` e FK para `scan_run` com exclusão em cascata.
  - **Backup desligado:** `allowBackup="false"`. A documentação de Auto Backup diz que, com targetSdk 31+, em aparelhos de alguns fabricantes isso não desliga a transferência direta entre aparelhos. Por isso entram também `dataExtractionRules` e `fullBackupContent`, excluindo os nove domínios. O `MergedManifestTest` confere a flag e os dois arquivos; um passo do CI confere a ligação no manifesto.
  - **Regra de schema:** até a primeira versão pública, o schema da versão 1 pode mudar, com o `1.json` commitado junto, sem `Migration`, porque não há banco instalado em aparelho de usuário. A partir da primeira versão pública, o schema de uma versão publicada fica congelado: mudança exige subir a versão, uma `Migration` e um teste de migração, e o CI deve barrar alteração em `N.json` já existente na `main` (passo da SPEC-073). O build regera o schema a cada compilação, então o teste de migração sozinho não detecta entidade alterada; desde já, o passo "Schema do Room commitado" do CI falha se o build regerar `core-data/schemas` com diferença do commitado.
  - Banco `tapguard.db`; DataStore `settings`.
- **Alternatives:** Room 3 (rejeitada pelo usuário; skill e convenções seguem o Room 2); kapt (incompatível com o AGP 9); colunas em `snake_case` (rejeitada: os nomes da especificação são o vocabulário do projeto); só `allowBackup="false"` (insuficiente no Android 12+); `MigrationTestHelper` clássico (falha no Robolectric); teste instrumentado da migração (exigiria emulador no CI).
- **Consequences:** a SPEC-018 pode ajustar a versão 1 (índices, colunas) enquanto não houver versão pública, sempre com o schema commitado. A SPEC-073 acrescenta a trava dos schemas publicados. Testes que gravam com DataStore real mais de uma vez são ignorados no Windows e verificados pelo CI.

## ADR-015 — Modelos do domínio: imutabilidade, cartão barrado pelo tipo, máscara no domínio
- **Date:** 2026-11-18
- **Status:** Aceita (implementada na SPEC-010)
- **Context:** a SPEC-010 cria os modelos e as interfaces da seção 7 no `core-domain`, que vai virar SDK para bancos. A issue pede modelos imutáveis; a seção 7 cita `NfcTagHandle` e `CardReadResult` sem definir a forma; o número completo do cartão nunca pode sair do `core-nfc` (RNF-02, ADR-008); a máscara de `app_snapshot.signals` tem ordem fixa (S1 no bit 0 até S9 no bit 8); a forma do `ScoringContext` estava em aberto desde o bootstrap.
- **Decision:**
  - **Imutabilidade, inclusive das coleções:** data classes só com `val`, verificado pelo detekt (`DataClassShouldBeImmutable` em `core-domain/detekt.yml`). Modelo com coleção tem construtor privado, `copy()` privado (`@ConsistentCopyVisibility`) e uma fábrica `companion operator fun invoke` com o mesmo nome, que copia para `Collections.unmodifiableSet/List`. A coleção exposta recusa alteração mesmo por cast. Verificado no Kotlin 2.4.21: de fora, a chamada resolve para a fábrica; dentro, para o construtor (bytecode); `copy()` de fora não compila; sem aviso do compilador.
  - **Exceção de alcance fechado ao `@Suppress`:** a fábrica do `InstalledApp` tem 8 parâmetros, e o detekt (`LongParameterList`, limite 6 em função) a recusa, embora isente o construtor da data class (`ignoreDataClasses`). `@Suppress("LongParameterList")` é permitido **apenas** em fábricas que espelham exatamente o construtor de uma data class. Fora disso, a regra de não usar `@Suppress` continua valendo.
  - **`NfcTagHandle` opaco:** interface sem membros. O `core-nfc` embrulha a `Tag` do Android na própria implementação e a desembrulha no `CardReader`. O domínio não conhece a `Tag` nem comandos ao cartão.
  - **Número do cartão barrado pelo tipo:** `CardReadResult.Success(applicationLabel, lastFourDigits: LastFourDigits, expiry: YearMonth)`, sem outro campo numérico. `LastFourDigits` é uma value class que só aceita quatro dígitos ASCII, com mensagem de erro que não repete o valor. O `toString()` do `Success` e do `LastFourDigits` não mostra nenhum valor (nem o nome da aplicação). Resultados: `Success`, `UnsupportedCard`, `ReadInterrupted`.
  - **Máscara no domínio:** `Signal.bit` explícito e as funções `toBitMask()`/`signalsFromBitMask()` no `core-domain`; máscara inválida lança `IllegalArgumentException`. O `core-data` só as chama (SPEC-018).
  - **`ScoringContext` = `trustedPackages`:** pesos são configuração da implementação do motor; a regra 3 sai da ausência de S1.
  - **Regra 1 no inventário:** o `AppInventory` não devolve apps do sistema, e o `InstalledApp` não tem campo para isso. O teste da regra 1 fica na SPEC-011, e o critério "100% das regras de ajuste" da SPEC-017 cobre as regras 2 a 4.
  - **`ScanReport`** recusa `finishedAt` anterior a `startedAt`.
- **Alternatives:** canal de comandos ao cartão (`transceive`) no domínio (rejeitado: anteciparia a SPEC-041 e passaria a resposta com o número completo por um contrato do domínio); `kotlinx-collections-immutable` (rejeitado: dependência anterior à 1.0 num módulo que vai virar SDK); só tipos de leitura por convenção (não impede alteração pela origem nem por cast); máscara num conversor do `core-data`; `ordinal` como bit (reordenar mudaria valores gravados); `isSystemApp` no `InstalledApp`; `NfcUnavailable`/`NfcDisabled` no `CardReadResult` (não acontecem em `read(tag)`); subir o `functionThreshold` da `LongParameterList` no módulo (afrouxaria a regra para todo o domínio); agrupar campos do `InstalledApp` em subtipos (inventaria tipos fora da especificação).
- **Consequences:** a SPEC-011 testa a regra 1 e a SPEC-017 cobre as regras 2 a 4. A SPEC-018 converte `Instant` e a máscara para as colunas. A SPEC-041 decide o resultado para um `NfcTagHandle` de outra origem e para cartão sem validade ou sem nome. Renomear `RiskLevel` ou `ScanTrigger` muda valores gravados (ADR-014). Fixtures de teste não espelham construtores longos: expõem só os parâmetros usados.

## ADR-016 — Inventário de apps: permissão fora da biblioteca, regra 1 e supressões
- **Date:** 2026-11-18
- **Status:** Aceita (implementada na SPEC-011)
- **Context:** a SPEC-011 implementa o `AppInventory` no `core-detection`, que vai virar SDK para bancos. Para ver todos os pacotes a partir da API 30, o app precisa de `QUERY_ALL_PACKAGES`, que é restrita na Play Store. A regra 1 da seção 5 (apps do sistema ignorados) ficou com o inventário (ADR-015). As APIs do `PackageManager` mudam entre a API 26 e a 36: o código de versão longo vem na 28, a origem da instalação na 30 e o `PackageInfoFlags` na 33. O compileSdk 37 marca como depreciados o `versionCode` e o `getInstallerPackageName`, e o lint recusa `QUERY_ALL_PACKAGES` com erro.
- **Decision:**
  - **`QUERY_ALL_PACKAGES` só no manifesto do `app`**, com `tools:ignore="QueryAllPackagesPermission"` e comentário. A biblioteca não declara permissão nenhuma: uma permissão no manifesto dela seria herdada por todo app que usasse o SDK. Quem usa a biblioteca declara a permissão (README do módulo). O `MergedManifestTest` exige a permissão no `app`, e o `LibraryManifestTest` falha se o manifesto mesclado da biblioteca declarar qualquer permissão além de `REORDER_TASKS`, que vem do `androidx.test:core` e só existe em teste.
  - **Regra 1:** fica fora o app com `FLAG_SYSTEM` **ou** `FLAG_UPDATED_SYSTEM_APP`. Um app pré-instalado atualizado mantém `FLAG_SYSTEM`, e a atualização precisa da mesma assinatura do original. Há um teste para cada caso.
  - **Próprio app fora:** o pacote de quem consulta (`context.packageName`) não é devolvido, também no SDK (o app do banco não avalia a si mesmo). RF-101 atualizado em `docs/especificacao.md`.
  - **Busca em duas etapas:** uma lista leve com flags 0, onde rodam os filtros. Depois, para cada app de usuário, `GET_PERMISSIONS | GET_SERVICES | MATCH_DISABLED_COMPONENTS` e o instalador. Cada chamada carrega um só app, e os dados dos apps do sistema nunca são pedidos. Serviços desligados entram em `services`, e app inteiro desligado aparece. `ensureActive()` a cada app. App desinstalado no meio (`NameNotFoundException`) é pulado; outros erros do sistema são propagados.
  - **Instalador no inventário:** `getInstallSourceInfo().installingPackageName` a partir da API 30 e `getInstallerPackageName` abaixo. A SPEC-012 só compara com a lista de lojas (comentário na issue #8).
  - **Exceção de alcance fechado ao `@Suppress`, ao lado da `LongParameterList` (ADR-015):** `@Suppress("DEPRECATION")` só no ramo antigo de um teste de `SDK_INT` cujo outro ramo usa a API nova. No código de produção, só no `PackageManagerVersionCompat.kt` (funções `internal`, único arquivo com ramos por versão); em teste, só na fixture que grava o `versionCode` para a API 26.
  - **Supressões de lint no próprio elemento, com comentário, sem `lint.xml`:** `tools:ignore="QueryAllPackagesPermission"` no `app` e `@SuppressLint("QueryPermissionsNeeded")` em `installedPackagesCompat`.
  - **`api` para tipos da API pública:** o `core-detection` expõe `api(project(":core-domain"))` e `api(kotlinx-coroutines-core)`, porque `AppInventory`, `InstalledApp` e `CoroutineDispatcher` aparecem na API pública dele. Regra geral para módulos de biblioteca: dependência cujo tipo aparece na API pública é `api`; o resto é `implementation`.
  - **API pública mínima:** só o `PackageManagerAppInventory(context, ioDispatcher)`, que guarda o `PackageManager` e o nome do pacote, não o `Context`. Hilt na SPEC-018.
- **Alternatives:**
  - permissão no manifesto da biblioteca (rejeitada: herdada pelos bancos);
  - uma chamada com permissões e serviços de todos os pacotes (rejeitada: dados do sistema descartados e resposta maior);
  - só `FLAG_SYSTEM` (rejeitada: a intenção fica implícita);
  - `androidx.core` (`PackageInfoCompat`) (rejeitada: mais uma dependência no SDK, que resolveria só o `versionCode`);
  - aceitar os avisos de depreciação (rejeitada: regra de "sem aviso novo");
  - `lint.xml` por módulo (rejeitada: supressão longe do motivo);
  - ponto de troca `internal` só para teste (rejeitada: os ganchos do Robolectric falham em vez de passar se o mecanismo mudar).
- **Consequences:**
  - O CI baixa as imagens do Robolectric das APIs 26, 28, 30, 33 e 36.
  - Os testes de cancelamento e de desinstalação no meio dependem de o Robolectric copiar o `PackageInfo` (Parcel); se isso mudar, eles falham.
  - A visibilidade de pacotes, o limite de transferência e o desempenho em aparelho ficam para o teste ponta a ponta e para a SPEC-018.
- **Open:** `explicitApi()` no `core-detection` quando o SDK for publicado, para que todo símbolo público seja declarado de propósito.

## ADR-017 — Sinal S1: coletor no `core-detection`, lojas confiáveis e limitações
- **Date:** 2026-11-18
- **Status:** Aceita (implementada na SPEC-012)
- **Context:** a SPEC-012 entrega o coletor do S1 (instalado fora de uma loja confiável). O `AppInventory` já lê o instalador nos dois caminhos de API (ADR-016, comentário na issue #8), então o coletor só compara `InstalledApp.installerPackageName` com uma lista e não usa API do Android. A §5 citava cinco lojas sem pacote. Desde 30/09/2026, o Google exige verificação de desenvolvedores no Brasil para instalações de sete lojas participantes ([Android Developers Blog, jun/2026](https://android-developers.googleblog.com/2026/06/android-developer-verification.html)). O `core-detection` vai virar SDK para bancos.
- **Decision:**
  - **Coletor no `core-detection`** (`InstallSourceCollector`, pacote `core.detection.signal`), mesmo sem API do Android. A §7 põe os coletores no `core-detection` e só as interfaces no `core-domain`. A regra "nada de lógica de score fora do `core-domain`" vale para o score: o coletor só detecta. Para o SDK não muda nada, porque o `core-detection` expõe o `core-domain` como `api`. Todos os coletores ficam juntos com o `ScanDevice`. Custo aceito: o coletor fica fora do Kover. Os testes são JUnit 4 sem Robolectric.
  - **S1 quando o instalador é nulo ou está fora da lista**, com comparação exata (o nome de pacote diferencia maiúsculas e minúsculas). Nulo aparece em `adb install` e quando o instalador foi desinstalado ([InstallSourceInfo](https://developer.android.com/reference/android/content/pm/InstallSourceInfo)).
  - **Lista `TrustedStores.packageNames`**, com 11 pacotes e a fonte de cada um num comentário:
    - as sete lojas da verificação do Google: Google Play `com.android.vending`, Galaxy Store `com.sec.android.app.samsungapps`, Xiaomi GetApps `com.xiaomi.mipicks`, OPPO App Market `com.heytap.market`, vivo V-Appstore `com.vivo.appstore`, HONOR App Market `com.hihonor.appmarket` e Palm Store `com.transsnet.store`;
    - as variantes `com.xiaomi.market` e `com.oppo.market`: sem elas, celulares Xiaomi e realme teriam falso positivo;
    - Huawei AppGallery `com.huawei.appmarket`, que está na §5;
    - Amazon Appstore `com.amazon.venezia`: está na §5 e fechou em celulares Android comuns em 20/08/2025, mas segue nos aparelhos Fire. Os apps instalados antes ainda mostram esse instalador, e tirar a loja daria S1 a eles.
    - Os pacotes vêm de fontes de terceiros: [Talsec](https://docs.talsec.app/appsec-articles/glossary/sideloading/installation-sources-cookbook), APKMirror, [bayton](https://sysapps.bayton.org) e Stack Overflow. Não há documento oficial dos fabricantes.
  - **Ficam fora:**
    - os instaladores de pacote do sistema (`com.google.android.packageinstaller`, `com.android.packageinstaller` e os de fabricante, como `com.miui.packageinstaller`) e `com.android.shell`, porque são o que aparece num APK instalado à mão ou por adb;
    - F-Droid (`org.fdroid.fdroid`): o cliente aceita repositórios de terceiros, que não seguem as regras do repositório oficial;
    - Aurora Store (`com.aurora.store`): cliente não oficial da Play, que aparece como instaladora de tudo o que baixa;
    - apps de clonagem (Smart Switch, Mi Mover, Clone Phone), que copiam também um APK de golpe;
    - `com.motorola.installer`: tem `INSTALL_PACKAGES`, mas a função não está descrita. Não foi achada loja própria da Motorola no Brasil.
  - **Risco de app ruim em loja confiável:** vem do limite da regra 3 (score até 29 sem S7 ou S9), que não é desta spec. Exemplo: o app pirata MovieBox passou de 300 milhões de downloads na Palm Store.
  - **Extensão pelo SDK:** o coletor recebe o conjunto inteiro na construção. O parâmetro é obrigatório, sem valor padrão, como o dispatcher do inventário (ADR-016), e o conjunto é copiado. A lista padrão é pública e não pode ser alterada nem por cast. O banco passa `TrustedStores.packageNames + "com.banco.loja"` ou uma lista menor. Um conjunto vazio faz todo app receber S1.
  - **Lista num arquivo próprio** (`object` público no `core.detection.signal`). As listas da SPEC-015 (carteiras e bancos) seguem a mesma forma ([comentário na #11](https://github.com/Valossa515/tap-guard/issues/11#issuecomment-6989480523)).
  - **Fixtures do domínio em `core-domain/src/testFixtures`** (`java-test-fixtures`), usadas pelo `core-detection` com `testImplementation(testFixtures(project(":core-domain")))`. Duas correções no build, verificadas num projeto descartável:
    - a tarefa `detekt` não analisava `src/testFixtures` e passa a analisar (`source.from`);
    - o Kover contava as fixtures na cobertura e passa a excluir o source set (`excludedSourceSets`).
- **Limitações conhecidas do S1:**
  - **Instalador falsificado com shell ou root:**
    - um app comum não consegue se passar por uma loja: `setInstallerPackageName` lança `SecurityException` se o novo instalador não tiver o certificado de quem chama, ou se o atual for de outro certificado (documentação do SDK);
    - com shell, isso é possível: `pm install -i com.android.vending` via adb, ou apps que usam o Shizuku (depuração sem fio), como o Universal Installer, com a opção "Set Install Source";
    - com root, um módulo como o AlterInstaller edita o registro de pacotes;
    - exige que a vítima ligue a depuração ou tenha root. Não foi achado relato de golpe de NFC que use isso.
  - **Nome de pacote emprestado** (dedução, sem fonte): o nome de pacote é único por aparelho, não no mundo. Num aparelho sem uma das lojas (por exemplo, `com.huawei.appmarket` num Samsung), um app comum pode usar o pacote dela e instalar outros apps, que aparecem como instalados por loja confiável. Análise:
    - o app que instala o golpe foi instalado à mão e recebe S1;
    - se ele se desinstalar, o instalador do golpe vira nulo e também gera S1;
    - a brecha é o tempo em que o app instalador continua no aparelho.
    - Defesa planejada na issue #57 (pós-MVP): confiar no instalador só se ele for app do sistema, com exceção a decidir para a Amazon Appstore, e `getInitiatingPackageName` (API 30+), que não muda com `setInstallerPackageName`, como segunda defesa.
  - **Apps de clonagem:** um app de banco copiado para um celular novo pode ter o app de clonagem como instalador e ficar vermelho (S1 + S2 + S3 = 65). Quem resolve isso é a lista de bancos conhecidos da SPEC-015, não a lista de lojas. Para isso, a SPEC-017 precisa definir como essa lista entra no score, e conferir só o nome do pacote cai na limitação acima. A conferência em aparelho real fica para o teste ponta a ponta.
  - **Instalação à mão a partir da API 30:** não foi confirmado se o instalador registrado é o do sistema ou o app que abriu o APK (navegador, WhatsApp). Nos dois casos fica fora da lista e gera S1.
- **Alternatives:**
  - coletor no `core-domain` (rejeitada: os coletores ficariam em dois módulos, contra a §7);
  - valor padrão no construtor (rejeitada: um esquecimento passaria calado);
  - lista num recurso JSON (rejeitada: ler recurso Android exige `Context`, e o arquivo Kotlin compila);
  - tornar `unmodifiableCopy()` público (rejeitada: mudaria a API do `core-domain` por uma linha);
  - F-Droid, Aurora Store e apps de clonagem na lista (rejeitadas pelos motivos acima);
  - `detektTestFixtures` no `check` (rejeitada: experimental e com outra configuração).
- **Consequences:**
  - a §5 da especificação, o glossário e o `backlog_rules.md` passam a listar os pacotes;
  - os testes do coletor parametrizam sobre uma lista escrita à mão, para que uma mudança na lista de produção derrube um teste;
  - fora de `src/test`, a regra `MagicNumber` do detekt vale no `testFixtures`.

## ADR-018 — Sinais S2 e S3: permissão de NFC, descoberta do HCE e critério mais largo
- **Date:** 2026-11-19
- **Status:** Aceita (implementada na SPEC-013)
- **Context:**
  - A SPEC-013 entrega os sinais S2 (declara a permissão de NFC) e S3 (declara serviço de emulação de cartão, HCE).
  - `InstalledApp.services` só guardava nomes de classe, e pelo nome não dá para saber se um serviço é de HCE.
  - O sistema NFC acha os serviços em [`RegisteredServicesCache.getInstalledServices`](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/heads/main/NfcNci/src/com/android/nfc/cardemulation/RegisteredServicesCache.java) (AOSP, main):
    - duas consultas `queryIntentServicesAsUser`, com `HostApduService.SERVICE_INTERFACE` e `OffHostApduService.SERVICE_INTERFACE`, com `GET_META_DATA` e sem `MATCH_DISABLED_COMPONENTS`;
    - pula o serviço não exportado, o de pacote sem a permissão `NFC`, o que não exige `BIND_NFC_SERVICE` e o de meta-data ausente ou inválido.
  - `BIND_NFC_SERVICE` também é exigida por `OffHostApduService` e `HostNfcFService` ([HostNfcFService](https://developer.android.com/reference/android/nfc/cardemulation/HostNfcFService)).
- **Decision:**
  - **S2 só com `android.permission.NFC`** em `requestedPermissions`, com comparação exata. É permissão normal, concedida na instalação, e é a que o modo leitor do golpe exige ([Manifest.permission](https://developer.android.com/reference/android/Manifest.permission)). Não contam:
    - `NFC_TRANSACTION_EVENT` (API 28) e `NFC_PREFERRED_PAYMENT_INFO` (API 30), que não leem nem emulam cartão sem a `NFC`;
    - `BIND_NFC_SERVICE`, de nível signature: o app a usa para proteger o serviço, não a pede.
  - **S3 pela mesma descoberta do sistema:** o inventário faz uma consulta `queryIntentServices(Intent(HostApduService.SERVICE_INTERFACE), MATCH_DISABLED_COMPONENTS)` por varredura, depois da lista leve. Fica com os serviços cuja `ServiceInfo.permission` é `BIND_NFC_SERVICE` e os agrupa por pacote em `InstalledApp.cardEmulationServices`. Pacotes fora da lista leve (sistema, o próprio app) são ignorados.
  - **Off-host e NFC-F não contam:** no off-host os comandos vão para o chip seguro e não passam pelo app ([OffHostApduService](https://developer.android.com/reference/android/nfc/cardemulation/OffHostApduService)). NFC-F (FeliCa) não é o padrão dos cartões EMV no Brasil.
  - **Critério mais largo que o do sistema, de propósito:** não exige serviço exportado, meta-data nem a permissão `NFC` no pacote. Um falso positivo raro (serviço declarado com o filtro e a permissão, mas que o sistema ignoraria) é melhor que deixar de avisar. Por isso o S3 não depende do S2.
  - **Desligados contam:** componente desligado no manifesto, desligado em tempo de execução e app inteiro desligado. O app pode ligar o serviço depois com `setComponentEnabledSetting`. Segue o inventário da SPEC-011, que já inclui serviços e apps desligados.
  - **Dois coletores, um por sinal:** `NfcPermissionCollector` e `CardEmulationServiceCollector`, públicos, `class` sem parâmetros, no `core.detection.signal`, sem API do Android ([comentário na #9](https://github.com/Valossa515/tap-guard/issues/9#issuecomment-7027958269)). Cada um devolve o seu sinal ou nada, como o do S1. Os dois leem partes diferentes do `InstalledApp`, e o SDK pode usar só um. As constantes do Android que eles comparam ficam como literais, sem `import android.*`.
  - **Campo `InstalledApp.cardEmulationServices: Set<String>`**, obrigatório, depois de `services`, copiado com `unmodifiableCopy()`. O nome é o do `Signal.CARD_EMULATION_SERVICE`; `hostApduServices` levaria uma classe do Android para o `core-domain`. Não é validado como parte de `services`: os dois vêm de chamadas diferentes. A SPEC-016 (S7) pode reaproveitá-lo para montar o `ComponentName` de cada serviço.
  - **Segunda supressão de lint:** `@SuppressLint("QueryPermissionsNeeded")` em `queryIntentServicesCompat`, com comentário, ao lado da de `installedPackagesCompat` (ADR-016). O lint acusa os dois ramos (`ResolveInfoFlags` na API 33+).
  - **Banco:** sem mudança. `app_snapshot` não guarda serviços; S2 e S3 vão nos bits 1 e 2 de `signals` pela SPEC-018. O `1.json` não muda.
- **Alternatives:**
  - `ServiceInfo.permission` do `getPackageInfo` que já existia (rejeitada: conta off-host e NFC-F e não confere o filtro de intent, que é como o sistema acha o serviço);
  - permissão mais a chave do meta-data com `GET_META_DATA` (rejeitada: não confere o filtro e carrega o meta-data de todos os serviços de cada app);
  - a mesma consulta por app com `setPackage` (rejeitada: N chamadas em vez de uma);
  - `services` como pares nome e permissão (rejeitada: o filtro de intent não vem no `ServiceInfo`, e o coletor repetiria a regra do inventário);
  - exigir exportação, meta-data e `NFC`, como o sistema (rejeitada pelo usuário: deixaria de avisar sobre serviços mal declarados);
  - um coletor para os dois sinais (rejeitada pelo usuário: motivos acima);
  - lista pública das permissões que não contam (rejeitada: o SDK não tem o que ampliar ali).
- **Consequences:**
  - O inventário faz uma chamada a mais por varredura, não por app. Para o SDK, a visibilidade segue a regra da lista (ADR-016); sem `QUERY_ALL_PACKAGES`, o README pede o `<queries>` com a ação de HCE.
  - **Testes no Robolectric 4.18:** um serviço que precisa aparecer no `queryIntentServices` entra com `addOrUpdateService` e `addIntentFilterForService`. Posto no `PackageInfo` do `installPackage`, ele não entra na resolução de intents: no bytecode do `ShadowPackageManager`, `addComponent` só cria a entrada de filtros para componente novo. Cada teste de HCE espera pelo menos um serviço encontrado, então uma mudança na biblioteca derruba os testes em vez de passar por engano.
- **Limitações e riscos:**
  - `MATCH_DISABLED_COMPONENTS` na consulta: o Robolectric o respeita, mas a documentação só o descreve para `PackageInfo` e `ComponentInfo`. Conferir na SPEC-022 com um serviço de HCE desligado no app isca.
  - Um app instalado entre a lista leve e a consulta fica sem S3 nessa varredura; a seguinte corrige.
  - Um serviço com o filtro e a permissão, mas não exportado ou sem meta-data, gera S3 sem funcionar como HCE (falso positivo aceito).

## ADR-019 — Sinais S4, S5, S6 e S8: fonte do S4, janela do S6 e exceção da fixture
- **Date:** 2026-11-20
- **Status:** Aceita (implementada na SPEC-014: S5, S6 e S8 no BATCH-01; S4 no BATCH-02)
- **Context:**
  - A SPEC-014 entrega os sinais S4 (serviço de acessibilidade ativo), S5 (pede sobreposição de tela), S6 (instalado ou atualizado nos últimos 7 dias) e S8 (pede leitura ou recebimento de SMS). É a primeira spec com batches: BATCH-01 com S5, S6 e S8, que usam dados que o `InstalledApp` já tinha; BATCH-02 com S4, que precisa de campo novo e de mudança no inventário.
  - A §8 citava `AccessibilityManager.getEnabledAccessibilityServiceList` como fonte do S4. Ele só devolve os serviços conectados ao sistema, e no Robolectric exige reflexão (`setComponentName`, método oculto) para dizer de que app é o serviço.
- **Decision:**
  - **S5 pela permissão declarada:** `OverlayPermissionCollector` devolve `OVERLAY_PERMISSION` quando `requestedPermissions` contém `android.permission.SYSTEM_ALERT_WINDOW`, com comparação exata. A §5 diz "pede"; a concessão (`AppOpsManager.unsafeCheckOpNoThrow`) não é lida: exigiria o `uid` no `InstalledApp`, uma chamada por app e, com `MODE_DEFAULT`, conferir a permissão em si.
  - **S8 só com `READ_SMS` e `RECEIVE_SMS`:** `SmsPermissionCollector`, comparação exata; as duas juntas geram um só S8. Não contam `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`, `READ_CELL_BROADCASTS` e `BROADCAST_SMS` ([Manifest.permission](https://developer.android.com/reference/android/Manifest.permission)).
  - **S6 com janela de 7 dias inclusiva e `Clock` injetado:** `RecentInstallOrUpdateCollector(clock: Clock)` devolve `RECENTLY_INSTALLED_OR_UPDATED` quando `maxOf(firstInstallTime, lastUpdateTime)` não é anterior a `clock.instant()` menos `Duration.ofDays(7)` (168 horas corridas, sem fuso). O `Clock` é obrigatório e sem padrão; o coletor não chama `Instant.now()`. A janela fica privada no coletor: a §5 fixa 7 dias, e calibrar é da SPEC-017.
  - **Horário no futuro conta como S6** (decisão do usuário). Com o relógio do aparelho atrasado, todos os apps ganham S6. Sozinho, o S6 vale 10 e não tira nenhum app do verde. **Somado a outros sinais, ele muda o nível de alguns apps** (pesos e regras da §5):
    - app de loja confiável com S9 (20, verde) vai a 30, amarelo, porque o S9 tira o teto de 29 (regra 3);
    - app de fora da loja com S1 e S3 (50, amarelo) vai a 60, vermelho, porque tem S1 e um sinal de NFC (regra 4);
    - app de fora da loja com S1, S2 e S5 ou S8 (55, amarelo) vai a 65, vermelho.
  - **S4 por `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`** (decisão do usuário, no lugar do `AccessibilityManager`; a §8 muda no BATCH-02). Motivos:
    1. o sinal é o usuário ter ligado o serviço: um serviço que caiu reconecta e continua perigoso, e a chave o mantém, enquanto o `AccessibilityManager` só lista os conectados;
    2. **correção:** o motivo dado era que o `AccessibilityManager` devolve lista vazia quando a chamada ao sistema falha, o que esconderia o S4 sem erro. O `Settings.Secure` tem o mesmo problema: `Settings.NameValueCache.getStringForUser` pega a `RemoteException` do provedor, escreve no log e devolve `null` ("Return null, but don't cache it"), em [android-8.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-8.0.0_r1/core/java/android/provider/Settings.java), android-16.0.0_r1 e `main`. Esse `null` não se distingue de "nenhum serviço ligado". Nenhuma das duas fontes deixa propagar a falha. **A troca de fonte se mantém pelos motivos 1 e 3**;
    3. no Robolectric basta `Settings.Secure.putString`, sem reflexão (conferido nas APIs 26, 28, 30, 33 e 36).
  - **Legível sem permissão da API 26 à 36:** até a API 30, o `SettingsProvider.enforceSettingReadable` só barra instant apps (tags 8.0.0_r1, 9.0.0_r1, 10.0.0_r1 e [11.0.0_r1](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-11.0.0_r1/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java)). Da API 31 em diante, o `checkReadableAnnotation` exige `@Readable`, e a chave tem `@Readable` sem `maxTargetSdk` nas tags 12.0.0_r1 a 16.0.0_r1 e na [`main`](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/provider/Settings.java). Vale para targetSdk 36 e acima. O texto é global, sem filtro de pacotes: o SDK não precisa de `<queries>` para o S4.
  - **S4 no inventário, coletor puro (opção A):** o inventário lê a chave uma vez por varredura, separa por `:`, pula entradas vazias, passa cada uma por `ComponentName.unflattenFromString` (formato completo e curto `pkg/.Cls`, o que o sistema grava com `flattenToShortString`), pula as nulas e agrupa os nomes de classe por pacote no campo novo `InstalledApp.enabledAccessibilityServices`. É a mesma separação do `AccessibilityManagerService.readColonDelimitedStringToSet`. Pacotes fora da lista de apps de usuário são ignorados; chave nula ou vazia quer dizer nenhum serviço ligado. A `SecurityException` (se a chave deixar de ser legível) propaga, sem `try`.
  - **S4 não cruza com `services`** (confirmado pelo usuário): um serviço ligado conta mesmo que a classe não esteja entre os declarados. É o critério mais largo do ADR-018.
  - **Quatro coletores, um por sinal**, públicos, `class`, no `core.detection.signal`, sem API do Android: `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector` e `EnabledAccessibilityServiceCollector`.
  - **Exceção do ADR-015 estendida à fixture `anInstalledApp`:** com `firstInstallTime` e `lastUpdateTime`, ela passa a 7 parâmetros (8 com `enabledAccessibilityServices`, no BATCH-02), e o detekt (`LongParameterList`) já dispara com 6. Ela recebe `@Suppress("LongParameterList")` com KDoc: repassa à fábrica do `InstalledApp`, uma data class, com valores padrão, e só existe no `testFixtures`. A exceção vale **só** para fixtures que repassam à fábrica de uma data class, com valores padrão, não para qualquer função de teste (emenda 1.2 do contrato, aprovada pelo usuário).
  - **Banco:** sem mudança. S4, S5, S6 e S8 vão nos bits 3, 4, 5 e 7 de `signals` pela SPEC-018; o `1.json` não muda.
- **Alternatives:**
  - S5 pela concessão no `AppOpsManager` (rejeitada: a §5 pede a permissão declarada; custo de `uid` e de uma chamada por app);
  - `AccessibilityManager.getEnabledAccessibilityServiceList` para o S4 (rejeitada pelo usuário: só os conectados; reflexão no Robolectric);
  - S4 num contexto de varredura passado a `collect` (rejeitada: muda a interface e os coletores existentes) ou num coletor que lê o Android (rejeitada: N leituras ou cache, e `android.*` no coletor);
  - cruzar o S4 com `services` (rejeitada pelo usuário);
  - ignorar horários no futuro no S6 (rejeitada pelo usuário: um falso positivo raro é melhor que deixar de avisar);
  - janela do S6 como parâmetro do construtor (rejeitada por ora: a §5 fixa 7 dias);
  - para a fixture: `ignoreDefaultParameters` no detekt do módulo (afrouxaria a regra para a produção) ou fixture com 5 parâmetros e ajudantes locais (repetiria os padrões e não resolveria o BATCH-02).
- **Consequences:**
  - O S6 dispara em toda atualização automática da loja; um app de loja confiável fica limitado a 29 pela regra 3, a não ser que tenha S7 ou S9.
  - O `Clock` do grafo (`Clock.systemUTC()`) entra com a ligação no Hilt, na SPEC-018.
  - O inventário passa a quatro etapas (lista leve, HCE, chave de acessibilidade, detalhe) e guarda o `ContentResolver`; o construtor não muda.
  - **Testes no Robolectric 4.18:** a chave é gravada com `Settings.Secure.putString`, que basta nas cinco APIs e volta a `null` em cada teste. O teste escreve a chave como literal, então trocar a chave na produção derruba os testes. O Robolectric não aplica o `@Readable`.
- **Limitações e riscos:**
  - Falha do provedor de configurações: o S4 some nessa varredura sem erro, igual a "nenhum serviço ligado".
  - Relógio do aparelho atrasado: S6 em todos os apps e mudança de nível nos exemplos acima.
  - O Robolectric não aplica o `@Readable`: a legibilidade vem das fontes do AOSP; conferência em aparelho na SPEC-022.
  - Serviço ligado que o app não declara mais gera S4 (falso positivo raro, aceito).

## ADR-020 — Listas de apoio (carteiras e bancos) e sinal S9: critério, formato e comparação de nomes
- **Date:** 2026-11-21
- **Status:** Aceita (SPEC-015: listas implementadas no BATCH-01; comparação de nomes implementada no BATCH-02, em `BankNameMatcher.kt`, `internal`, com a emenda 1.3 da spec; coletor de S9 no BATCH-03, planejado)
- **Context:**
  - A SPEC-015 (#11) entrega as listas de carteiras conhecidas (S7) e de bancos conhecidos (S9), e o coletor de S9 ("nome parecido com o de um banco conhecido, mas pacote diferente do oficial"). A §5 citava as listas sem pacotes, e a §13 deixava em aberto "quantos entram na versão 1.0".
  - O Pix por aproximação faz apps de banco virarem o app de pagamento por aproximação padrão. Sem eles na lista de carteiras, o S7 acusaria o app legítimo do banco (#47 e comentário na #11).
  - A lista de lojas (`TrustedStores`, ADR-017) fixou o formato que as listas desta spec seguem.
  - Três batches: listas (BATCH-01), comparação de nomes (BATCH-02) e coletor (BATCH-03). Com dois, o segundo passaria de 500 linhas (emenda 1.1 da spec).
- **Decision:**
  - **Formato do `TrustedStores`:** um `object` público por tipo, num arquivo próprio, em `core.detection.signal`; conjunto que não pode ser alterado nem por cast (`Collections.unmodifiableSet(linkedSetOf(...))`); um comentário por entrada com o nome e a fonte (em `KnownBanks`, um por instituição, com o título de cada app). Quem usa o SDK monta o próprio conjunto com `+` ou `-`.
  - **`KnownWallets.packageNames: Set<String>`, 6 pacotes** (decisão do usuário: só apps com fonte de que fazem pagamento por aproximação). O S7 compara o pacote do serviço padrão, então a lista não precisa de tipo próprio. O consumidor é o coletor de S7 (SPEC-016).
    - Carteira do Google `com.google.android.apps.walletnfcrel` e Samsung Wallet `com.samsung.android.spay` (Google Play; §5);
    - Nubank `com.nu.production`: "é possível definir o Nubank como carteira padrão do dispositivo" ([TechTudo, 07/04/2026](https://www.techtudo.com.br/noticias/2026/04/como-funciona-o-pix-por-aproximacao-no-nubank-veja-a-novidade-edapps.ghtml));
    - Itaú `com.itau`: Pix por aproximação "dentro do seu aplicativo", só Android ([Mobile Time, 27/01/2026](https://www.mobiletime.com.br/noticias/27/01/2026/itau-pix-aplicativo/));
    - Banco do Brasil `br.com.bb.android`: o cliente escolhe o "Serviço Pix por Aproximação" em "NFC e pagamentos sem contato" ([TechTudo, 03/08/2026](https://www.techtudo.com.br/listas/2026/08/9-bancos-que-ja-permitem-pagar-por-aproximacao-usando-pix-veja-quais-edapps.ghtml); [BPMoney](https://bpmoney.com.br/mercado/pix-por-aproximacao-9-bancos-como-usar/));
    - Inter `br.com.intermedium`: pelo Super App 26.1.1 ou mais recente (mesmas fontes).
    - **Ficam fora:** Mercado Pago (o próprio banco diz que o Pix por aproximação "só pode ser realizado pela Carteira do Google"), PicPay, Bradesco, Caixa, Santander e C6, que fazem pela Carteira do Google (TechTudo e BPMoney, 03/08/2026). O PagBank "Tap On" é maquininha no celular (lê cartões), não carteira.
    - Itaú e Inter: as fontes dizem que pagam pelo app, mas não que viram o padrão. A entrada não custa nada se nunca forem o padrão.
  - **`KnownBankApp(displayName, packageName)`:** `data class` pública do `core-detection`, sem coleção. Uma entrada é um app ou um apelido de app, não uma instituição: o Itaú tem três entradas, e o BB duas para o mesmo pacote. Nome ou pacote em branco lança `IllegalArgumentException`, com mensagem fixa que não repete o valor: quem usa o SDK escreve as entradas à mão, e um nome em branco nunca casaria, sem aviso. Não valida o formato do pacote (a comparação é exata). O `core-domain` não conhece o tipo.
  - **`KnownBanks.apps: Set<KnownBankApp>`, 29 entradas e 26 pacotes.**
    - **Critério de corte** (decisão do usuário): as 14 instituições com mais clientes no ranking do Banco Central (dez/2025), todas acima de 25 milhões: Caixa, Nubank, Bradesco, Itaú, Banco do Brasil, Santander, Mercado Pago, PicPay, Inter, PagBank, C6 Bank, BTG Pactual, 99Pay e Neon ([Melhores Cartões, 24/01/2026](https://www.melhorescartoes.com.br/bancos-com-mais-clientes-brasil.html), com os dados do [BCB](https://www3.bcb.gov.br/ranking/historico.do); [Forbes Brasil, 22/01/2026](https://forbes.com.br/forbes-money/2026/01/nubank-ultrapassa-112-milhoes-de-clientes-e-se-torna-a-segunda-maior-instituicao-financeira-do-brasil/)). Entram também os apps oficiais dessas instituições com o nome da marca no título, achados na busca da Google Play com o desenvolvedor do banco (Empresas, Cartões, Personnalité, CAIXA Tem, next, BTG Pactual Investimentos).
    - **Fonte de cada pacote:** a página da Play (`play.google.com/store/apps/details?id=<pacote>`), consultada em 2026-11-21. Cinco palpites voltaram 404 e não entraram: `com.itau.iti`, `com.itau.personnalite`, `com.itaucard`, `com.bradesco.cartoes`, `com.santander.empresas`.
    - **Nomes:** tirados do título da Play (a parte antes de `:`) ou do nome do desenvolvedor, sem os prefixos e sufixos que a comparação já tira (decisão do usuário). Apelidos com o mesmo pacote: "BB" e "Banco do Brasil"; "Santander Brasil" (título da Play) e "Santander"; "BTG Pactual Banking" e "BTG Pactual". Sem o apelido, o imitador "Santander" escaparia de "Santander Brasil" (distância 6). O rótulo real no aparelho (`loadLabel`) fica para a SPEC-022.
    - **Ficaram para depois, com o pacote já conferido na Play:** Banco Pan `br.com.bancopan.cartoes`, Sicoob `br.com.sicoobnet`, Sicredi `br.com.sicredimobi.smart` e "Sicredi X" `br.com.sicredi.app`, Banrisul `br.com.banrisul`, Safra `br.livetouch.safra.net`, BV `com.votorantim.bvpd`. As posições 15 a 18 do ranking não estão nas fontes. O Will Bank (19º) teve a liquidação decretada. O Banco Original não foi achado na busca.
  - **Comparação de nomes do S9** (decisão do usuário: nome inteiro, sem "contém"; conferida num protótipo em Java com a lista de 29 entradas e 80 rótulos). Implementação no BATCH-02 (`BankNameMatcher`, `internal`) e no BATCH-03 (`BankLookalikeCollector`).
    1. **Pacote na lista não gera S9:** se o pacote do app é o de qualquer entrada da lista, o coletor devolve vazio, com comparação exata. Usar todos os pacotes, e não só o do banco que casou, evita marcar um app oficial cujo nome lembra outro app do mesmo banco ("Itaú Empresas" casa com "Itaú" depois de tirar o sufixo).
    2. **Esqueleto do nome**, o mesmo para o rótulo e para os nomes da lista:
       - corta no primeiro separador (`:`, `|`, ` - `, ` – `, ` — `), se houver algo antes dele;
       - NFKC, minúsculas invariantes (`lowercase()`), NFD e remoção das marcas combinantes (`CharCategory.NON_SPACING_MARK`, sem regex);
       - letras parecidas trocadas: `0→o`, `1`, `l` e `!` por `i`, `3→e`, `4` e `@` por `a`, `5` e `$` por `s`, `7→t`, cirílico e grego que imitam letras latinas, `rn→m`, `vv→w`. A barra vertical `|` **não** vira `i` (emenda 1.3): o protótipo a trocava, mas a especificação não pede, e a barra já é separador. A troca só mudaria um rótulo que começa com `|`, e nenhum caso da tabela muda;
       - fica só com `a-z` e `0-9` (espaços, pontuação, emojis e caracteres invisíveis saem).
    3. **Variantes:** o esqueleto; sem um prefixo (`banco`, `meu`, `app`); sem sufixos, repetidamente (`token`, `empresas`, `cartoes`, pedidos pelo usuário; `empresa`, `cartao`, `pj`, `app`, `mobile`, `oficial`, `digital`, `seguranca`). Funciona sobre o esqueleto colado ("ItauToken"). Um prefixo ou sufixo só sai se sobrar algo.
       - **Emenda 1.3 (BATCH-02, 2026-11-23):** os prefixos e sufixos também passam pelo esqueleto quando o arquivo carrega, e as entradas vazias ou repetidas saem. Sem isso, `mobile`, `oficial` e `digital` nunca sairiam, porque o esqueleto já trocou `l` por `i` (`itauoficiai` não termina em `oficial`). O protótipo tinha o mesmo defeito, e nenhum caso da tabela usava esses sufixos. A ordem das listas não importa, porque as variantes tentam todos. As listas carregadas (`BANK_NAME_PREFIXES`, `BANK_NAME_SUFFIXES`) são `internal`, para um teste que confere que cada entrada sai.
    4. **Casa** quando alguma variante fica a uma distância de Damerau-Levenshtein (versão OSA: inserção, remoção, troca e transposição de letras vizinhas) dentro do limite do nome da lista: até 5 letras, 0 (só igual); de 6 a 9 letras, até 1; 10 letras ou mais, até 2. Nomes curtos não têm tolerância porque uma letra a mais ou a menos já forma outra palavra ("Baixa" não casa com "Caixa").
    5. **Esqueleto vazio nunca casa**; um nome da lista com esqueleto vazio é ignorado.
    6. No protótipo, nomes da lista só casam entre si quando são do mesmo banco (Itaú Empresas e Itaú, Bradesco Cartões e Bradesco, entre outros), e os pacotes deles estão na lista. Um teste do BATCH-02 protege isso (`known bank names only match hand-written same-bank pairs`, que lê `KnownBanks.apps` de propósito); com as 29 entradas finais, são 6 pares.
  - **Coletor de S9 (BATCH-03):** `BankLookalikeCollector(knownBankApps: Set<KnownBankApp>)`, parâmetro obrigatório e sem padrão, conjunto copiado, como o S1. Lista vazia nunca gera S9. Devolve `{BANK_LOOKALIKE}` ou vazio. Sem `android.*` (`java.text.Normalizer` é do JDK). A variante `lookalike` do app isca pode passar `KnownBanks.apps + KnownBankApp("Banco Fictício", …)`; como o teste ponta a ponta injeta o banco fictício fica para a SPEC-021 ou a SPEC-022.
- **Comportamentos aceitos** (falsos positivos e imitações que escapam, com testes que os documentam):
  - **Nome curto igual de outro desenvolvedor gera S9:** "Inter" (um app de clube) e "NEXT" (uma loja de roupas).
  - **O corte no separador também gera S9** (pedido do usuário): "Next - Launcher" e "Caixa – Organizador", de outro desenvolvedor, viram "Next" e "Caixa". O mesmo vale para "Neon | Wallpapers" e "Inter: Notícias do clube". O corte existe para pegar o imitador que copia o título da Play ("Nubank: conta, cartão e mais").
  - **Palavras fora da lista fechada escapam:** "Atualização Nubank", "Nubank Wallpapers", "Inter&Co" e "Neon Pay" não geram S9.
  - **Custo de um falso positivo:** um app de loja confiável com S9 perde o teto de 29 (regra 3) e pode ficar amarelo, mas não vermelho, porque o vermelho exige S1 (regra 4). A pessoa pode marcá-lo como confiável (regra 2).
- **Limitações:**
  - **Nome de pacote emprestado** (a mesma do S1, ADR-017): num aparelho sem o Nubank, um app de golpe pode usar `com.nu.production` e o nome "Nubank" e não recebe S9. Instalado à mão, ele recebe S1 e, com NFC e HCE, fica vermelho mesmo sem o S9. O S9 deixa de ajudar só quando o app vem de uma loja confiável. Defesa planejada na issue #65 (certificado de assinatura por pacote), junto com a #57.
  - **Rótulos não conferidos:** um imitador com o rótulo real de um app oficial que difira do nome da marca escapa. Conferência na SPEC-022.
  - **Listas embutidas** (RNF-01, sem internet): um banco que lançar o Pix por aproximação no próprio app recebe S7 até entrar em `KnownWallets`; um app oficial novo de um banco da lista pode receber S9 até entrar em `KnownBanks`.
- **Apps de clonagem (SPEC-017):** o banco copiado por Smart Switch, Mi Mover ou Clone Phone recebe S1 e, com S2 e S3, fica vermelho (ADR-017). Esta spec entrega `KnownBanks.apps`, de onde sai o conjunto de pacotes de banco (`Set<String>`, para o `KnownBankApp` não entrar no `core-domain`). A regra fica para a SPEC-017. Recomendação registrada na #13: a forma estreita (teto só com pacote de banco **e** instalador de clonagem conhecido), que não reabre a brecha do S1 e exige uma lista pública de apps de clonagem no formato destas listas.
- **Alternatives:**
  - todos os pacotes de banco na lista de carteiras (rejeitada pelo usuário: poria na lista apps sem fonte de que pagam por NFC);
  - incluir Pan, Sicoob, Sicredi, BV, Banrisul e Safra já na 1.0 (rejeitada pelo usuário: sem fonte da posição no ranking);
  - comparação por "contém o nome do banco" (rejeitada pelo usuário: acusaria "Caixa de Ferramentas" e outros);
  - tipo com conjuntos de nomes e de pacotes por instituição (rejeitada: fábrica com cópia de coleções e teste de lista mais difícil de ler);
  - nomes `KnownBank` e `KnownBanks.entries` (rejeitados na revisão do architect: uma entrada é um app, e `entries` é a propriedade dos `enum` do Kotlin);
  - regex para tirar acentos (rejeitada: depende do motor de regex da JVM e do Android) e Levenshtein sem transposição (rejeitada: "Nubnak" daria 2 e escaparia).
- **Consequences:**
  - A §5 da especificação, o glossário e o `backlog_rules.md` passam a listar os pacotes. A pendência "lista inicial de bancos e carteiras" da §13 fica resolvida.
  - Os testes das listas comparam com listas escritas à mão (`ExpectedKnownWallets`, `ExpectedKnownBankApps`): tirar ou pôr uma entrada na produção derruba um teste.
  - A API pública do `core-detection` ganha `KnownWallets`, `KnownBankApp` e `KnownBanks` (BATCH-01) e `BankLookalikeCollector` (BATCH-03).
  - Banco: sem mudança. O S9 vai no bit 8 de `signals` pela SPEC-018; o `1.json` não muda.

## Pending decisions (especificação, seção 13)
- Nome de exibição em português. (Identificador do pacote: decidido no ADR-011.)
- Licença do código.
- Lista inicial de bancos e carteiras na 1.0: decidida no ADR-020 (SPEC-015), com 6 carteiras e 29 apps de 14 instituições.
- Play Store já na 1.0 ou depois de validar com usuários.
- API de pagamento padrão no Android 15+ (SPEC-016).
