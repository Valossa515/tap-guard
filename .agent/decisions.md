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

## Pending decisions (especificação, seção 13)
- Nome de exibição em português. (Identificador do pacote: decidido no ADR-011.)
- Licença do código.
- Lista inicial de bancos e carteiras na 1.0.
- Play Store já na 1.0 ou depois de validar com usuários.
- API de pagamento padrão no Android 15+ (SPEC-016).
