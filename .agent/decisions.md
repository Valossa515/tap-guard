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

## Pending decisions (especificação, seção 13)
- Nome de exibição em português. (Identificador do pacote: decidido no ADR-011.)
- Licença do código.
- Lista inicial de bancos e carteiras na 1.0.
- Play Store já na 1.0 ou depois de validar com usuários.
- API de pagamento padrão no Android 15+ (SPEC-016).
