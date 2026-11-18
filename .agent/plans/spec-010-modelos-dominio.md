# Plan: SPEC-010 Modelos e contratos do domínio

> Issue #6 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-001 (#2, fechada) · Especificação §5, §7 e §9 · RNF-02, RNF-08 · ADR-004, ADR-008
> Spec: `.agent/specs/features/spec-010-modelos-dominio.spec.toon` (FT-010, `interface: library`) · Aceite: `.agent/specs/acceptance/spec-010-modelos-dominio.acceptance.toon`
> Branch: `spec-010-modelos-dominio` · Planejado em 2026-11-18

## Summary
Criar no `core-domain` os modelos e as cinco interfaces da §7, sem nenhuma implementação. São eles `InstalledApp`, `Signal` (S1 a S9), `RiskLevel`, `RiskResult` e `ScanReport`, mais os tipos que os contratos citam: `ScoringContext`, `ScanTrigger`, `NfcTagHandle` e `CardReadResult`. As interfaces são `AppInventory`, `SignalCollector`, `RiskScorer`, `ScanDevice` e `CardReader`.

Os modelos são imutáveis, inclusive as coleções. O número completo do cartão não cabe no tipo de sucesso da leitura. A conversão entre conjunto de sinais e máscara de bits fica no domínio. O módulo continua em Kotlin puro, sem dependência nova, e os testes desta spec levam a cobertura de linhas a pelo menos 80% (Kover).

## Scope
- **Módulo afetado:** só o `core-domain`, mais a documentação. O `app` não muda (`RiskLevelUi` segue até a SPEC-030).
- **Novos arquivos (esperados):**
  - modelos e interfaces em `core-domain/src/main/kotlin/io/github/valossa515/tapguard/core/domain/`, um arquivo por tipo, mais a conversão da máscara (`SignalBitMask.kt`) e um utilitário interno de cópia defensiva (`UnmodifiableCopies.kt`);
  - `core-domain/detekt.yml`;
  - testes JUnit 5 em `core-domain/src/test/...`, com um arquivo de fixtures do domínio.
- **Arquivos modificados:** `core-domain/build.gradle.kts` (bloco `detekt`), `core-domain/README.md`, `.agent/project-reference.md`, `.agent/glossary.md`, `.agent/conventions.md`, `.agent/context.md`, `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-015) e este plano (marcar as tarefas).
- **Arquivo removido:** `core-domain/src/test/.../CoreDomainSmokeTest.kt`, que os testes reais substituem.
- **Fora do escopo:**
  - o `RiskScorer` concreto, pesos, faixas, regras de ajuste e a classificação de "sinal de NFC" (SPEC-017);
  - o `AppInventory` concreto e os coletores (SPEC-011 a SPEC-016), e as listas de apoio (SPEC-015);
  - o `ScanDevice` concreto, a gravação, as contagens por nível e o mapeamento entidade ↔ modelo (SPEC-018);
  - parser TLV e leitor EMV, inclusive a ponte da `Tag` (SPEC-040 e SPEC-041);
  - a verificação de NFC ausente ou desligado (spec do teste do cartão);
  - fixtures compartilhadas entre módulos (`testFixtures`): ficam em `src/test` até a SPEC-011 precisar delas.

## Decisões já tomadas (usuário, 2026-11-18)

| Item | Decisão |
|------|---------|
| `InstalledApp` | `packageName`, `label`, `versionCode: Long`, `installerPackageName: String?`, `firstInstallTime: Instant`, `lastUpdateTime: Instant`, `requestedPermissions: Set<String>`, `services: Set<String>` (nomes de classe dos serviços declarados) |
| Apps do sistema | **Sem campo** no `InstalledApp`: a regra 1 ("apps do sistema são ignorados") é cumprida pelo `AppInventory`, que não os devolve (RF-101). **O teste da regra 1 fica na SPEC-011**, e o critério "100% das regras de ajuste" da SPEC-017 passa a cobrir as **regras 2 a 4** |
| `Signal` | Nove entradas com nomes descritivos, cada uma com o código S1 a S9 no KDoc, e um `bit` explícito: S1 no bit 0 até S9 no bit 8. Nomes: `UNTRUSTED_INSTALL_SOURCE` (S1), `NFC_PERMISSION` (S2), `CARD_EMULATION_SERVICE` (S3), `ACCESSIBILITY_SERVICE_ENABLED` (S4), `OVERLAY_PERMISSION` (S5), `RECENTLY_INSTALLED_OR_UPDATED` (S6), `UNKNOWN_DEFAULT_PAYMENT_APP` (S7), `SMS_PERMISSION` (S8), `BANK_LOOKALIKE` (S9). Sem peso no enum (os pesos ficam no arquivo de configuração da SPEC-017) |
| Máscara de bits | **No domínio**: conjunto ↔ máscara em funções puras do `core-domain`; o `core-data` (SPEC-018) só as chama. Máscara negativa ou com bit fora de 0 a 8 gera erro |
| `RiskLevel` | `GREEN`, `YELLOW`, `RED`, em ordem crescente de gravidade; os nomes são os valores gravados em `app_snapshot.level` e `alert_event.level`. Sem conversão de score para nível (SPEC-017) |
| `RiskResult` | `score: Int`, `level: RiskLevel`, `signals: Set<Signal>`; score fora de 0 a 100 gera erro; o nível **não** é conferido contra o score (pela regra 2, um app confiável fica verde com qualquer score) |
| `ScoringContext` | só `trustedPackages: Set<String>` (apps marcados como confiáveis pelo usuário, regra 2). Pesos são configuração da implementação; a regra 3 sai da ausência de S1; a recência usa relógio no coletor (SPEC-014) |
| `ScanTrigger`, `ScannedApp`, `ScanReport` | `ScanTrigger`: `MANUAL`, `APP_OPEN`, `PERIODIC`. `ScannedApp(app, risk)`. `ScanReport(trigger, startedAt: Instant, finishedAt: Instant, apps: List<ScannedApp>)`, sem id da varredura e sem contagens derivadas. **Emenda (2026-11-18):** `finishedAt` anterior a `startedAt` gera erro (`require` no `init`); igual é aceito |
| `NfcTagHandle` | Interface **sem membros** (marcador opaco). O `core-nfc` embrulha a `Tag` do Android na própria implementação e a desembrulha no `CardReader`; o domínio não conhece a `Tag` nem comandos ao cartão |
| `CardReadResult` | sealed com **só** `Success`, `UnsupportedCard` e `ReadInterrupted`. NFC ausente ou desligado não é resultado de `read(tag)`, que só é chamado com um cartão aproximado |
| `Success` | `applicationLabel: String`, `lastFourDigits: LastFourDigits`, `expiry: YearMonth`, **todos obrigatórios**. Nenhum campo para o número completo |
| `LastFourDigits` | value class sobre `String` que só aceita exatamente quatro dígitos ASCII (0 a 9) |
| `toString()` | mascarado no `Success` e no `LastFourDigits`, **incluindo a validade**; o do `Success` também esconde o nome da aplicação (aprovado em 2026-11-18) |
| Imutabilidade | data classes só com `val`. Modelos com coleção: construtor privado, fábrica `operator fun invoke` com o mesmo nome que copia para coleção não modificável, e `copy()` privado. **A coleção exposta não pode ser modificada nem por cast para `MutableSet` ou `MutableList`; os testes conferem isso.** Nenhum array nos modelos |
| detekt | `core-domain/detekt.yml` com `DataClassShouldBeImmutable` ativo, aplicado com `buildUponDefaultConfig = true` |
| `@Suppress` na fábrica do `InstalledApp` (2026-11-18, emenda 1.2) | A fábrica tem 8 parâmetros e o detekt (`LongParameterList`, limite 6 em função) a aponta; o construtor da data class é isento (`ignoreDataClasses`). `@Suppress("LongParameterList")` só nessa fábrica, com KDoc curto. **Exceção de alcance fechado**, registrada no ADR-015 e no `SKILLS.md`: `@Suppress("LongParameterList")` é permitido apenas em fábricas que espelham exatamente o construtor de uma data class; fora disso, nenhum `@Suppress` |

## Approach
- Seguir `skills/android/SKILL.md` (Domain Layer: modelos imutáveis, portas como interfaces, falhas esperadas como valores) e `skills/android/testing.md` (Unit Tests — Domain, Test Fixtures, nomes com crases).
- As cinco interfaces seguem as assinaturas da §7 sem mudança. `suspend` é recurso da linguagem: nenhuma dependência de corrotinas.
- O módulo continua só com `kotlin("jvm")`: um import `android.*` nem compila. "Compila em Kotlin puro" é garantido pelo próprio build, sem passo novo no CI.
- `java.time` (`Instant`, `YearMonth`) é JVM puro e existe no Android desde a API 26 (minSdk).
- **Cobertura:** as linhas executáveis são as fábricas, os `init` com validação, os enums, a conversão da máscara e os `toString` mascarados. Cada uma ganha teste. Meta de 100% das linhas; mínimo de 80% verificado pelo `koverVerify`.

## Tasks
1. [x] **Sinais e enums** (`feat:`): `Signal` com os bits, a conversão da máscara, `RiskLevel`, `ScanTrigger`, e o utilitário interno de cópia defensiva.
2. [x] **Modelos da varredura** (`feat:`): `InstalledApp`, `RiskResult`, `ScoringContext`, `ScannedApp`, `ScanReport`.
3. [x] **Modelos do cartão** (`feat:`): `NfcTagHandle`, `LastFourDigits`, `CardReadResult`.
4. [x] **Interfaces** (`feat:`): `AppInventory`, `SignalCollector`, `RiskScorer`, `ScanDevice`, `CardReader`.
5. [x] **detekt** (`chore:`): `core-domain/detekt.yml` com `DataClassShouldBeImmutable` e o bloco `detekt` no `core-domain/build.gradle.kts`.
6. [x] **Testes** (`test:`, papel tester): os cenários do contrato; remove o `CoreDomainSmokeTest`.
7. [x] **Documentação** (`docs:`): `core-domain/README.md`; `project-reference.md` (tipos do domínio); `glossary.md` (`ScoringContext`, `ScannedApp`, `LastFourDigits`, nomes dos sinais); `conventions.md` (imutabilidade; falhas esperadas como valores); `context.md`; `SKILLS.md` (regra de imutabilidade dos modelos e as decisões do domínio); **ADR-015** em `decisions.md` (handle opaco, número do cartão barrado pelo tipo, máscara no domínio, técnica de imutabilidade, regra 1 no inventário).
8. [x] **Conferência dos critérios**, localmente e desfeita sem commit:
   - um `var` temporário numa data class faz o detekt falhar;
   - um modelo temporário sem teste derruba o `koverVerify`;
   - `./gradlew build` verde.
   - 2026-11-18, local (Windows), cada alteração desfeita sem commit (árvore limpa depois de cada uma):
     - `val risk` → `var risk` em `ScannedApp` → o detekt falha: `The data class ScannedApp contains a mutable property. The offending property is called risk [DataClassShouldBeImmutable]` (AC-003).
     - Classe temporária `probe/Untested.kt` (data class com 30 funções, sem teste) → o `koverVerify` falha: `lines covered percentage is 71.929800, but expected minimum is 80`. Removida, o `koverVerify` passa (AC-022).
     - `./gradlew build` verde; Kover do `core-domain` em 98,8% de linhas (82 de 83; a linha descoberta é a declaração da value class `LastFourDigits`). Nenhum aviso novo do Kotlin; o aviso de depreciação do Gradle (`ReportingExtension.file`) já existe na `main` (AC-023).
     - Nenhum import `android.*` ou `androidx.*` em `core-domain/src`; plugins só `kotlin.jvm`, `ktlint`, `detekt`, `kover`; nenhuma dependência nova (AC-001).
     - Nenhuma classe implementa as cinco interfaces nem o `NfcTagHandle`; nenhum peso ou faixa; `git diff main -- app` vazio (AC-021).
     - Os quatro modelos com coleção têm `@ConsistentCopyVisibility` e construtor privado; uma chamada a `copy()` de outro pacote não compila (`it is private`), e a fábrica chama o construtor privado (bytecode); nenhum array (AC-006). Único `@Suppress`: `LongParameterList` na fábrica do `InstalledApp` (emenda 1.2).

## Database changes
Nenhuma. A máscara de `app_snapshot.signals` segue a ordem de `conventions.md` (S1 no bit 0 até S9 no bit 8); as funções de conversão nascem aqui e são usadas pelo `core-data` na SPEC-018.

## Risks & open questions
- **Fábrica com o mesmo nome do construtor privado:** o idioma (construtor privado + `operator fun invoke` no `companion`) precisa resolver para a fábrica fora da classe e para o construtor dentro dela. O builder confere na primeira compilação; se não resolver, para e pergunta.
- **`@ConsistentCopyVisibility` no Kotlin 2.4.21:** se o compilador já tornar o `copy()` privado por padrão e avisar que a anotação é redundante, o builder para e pergunta (o projeto não aceita aviso novo).
- **Cópia não modificável:** usar `java.util.Collections.unmodifiableSet/List` sobre uma cópia. `Set.copyOf`/`List.copyOf` só existem no Android a partir da API 30 (minSdk 26), e `toSet()`/`toList()` do Kotlin devolvem coleções que um cast consegue alterar.
- **detekt `MagicNumber`:** limites (100, 4 dígitos) como `const val` nomeados; bits passados como argumento nomeado. Sem `@Suppress`.
- **Nomes de enum persistidos:** renomear `RiskLevel` ou `ScanTrigger` muda valores gravados no banco (ADR-014).
- **`applicationLabel` com dígitos:** o tipo não impede; fica para a revisão e para o teste de privacidade já previsto (busca do número sintético).
- **Tamanho da PR:** estimativa de 450 a 600 linhas entre código e testes. Fica numa PR pela regra "um pull request por spec".

## Acceptance criteria
- [x] Compila em Kotlin puro, sem dependência nova (AC-001)
- [x] Interfaces com as assinaturas da §7 (AC-002)
- [x] Modelos imutáveis: detekt barra `var`; cópia defensiva; coleção exposta recusa alteração por cast; `copy()` privado (AC-003 a AC-006)
- [x] Sinais nos bits 0 a 8; ida e volta nas 512 combinações; máscara inválida rejeitada; conjunto devolvido não modificável (AC-007 a AC-010)
- [x] `RiskLevel` em ordem de gravidade; `RiskResult` com score de 0 a 100 (AC-011, AC-012)
- [x] Formas de `InstalledApp`, `ScoringContext`, `ScanTrigger`, `ScannedApp` e `ScanReport` (AC-013 a AC-015); `ScanReport` recusa fim antes do início (AC-026)
- [x] `NfcTagHandle` sem membros; `CardReadResult` com três subtipos; `LastFourDigits` só com quatro dígitos; `toString` mascarado (AC-016 a AC-020)
- [x] Nenhuma implementação antecipada; `app` sem mudança (AC-021)
- [x] Cobertura de pelo menos 80% (AC-022)
- [x] ktlint, detekt e testes passam (AC-023)
- [x] Documentação atualizada, com o ADR-015 (AC-024)
- [x] CI da PR verde (AC-025). PR #51, run 44321990735 (commit `e9c698a`): "Regras do projeto" e "Build e testes" passaram. No artefato `test-reports` (Linux), o `core-domain` executou os 32 testes das 10 classes, com 0 falhas e 0 ignorados
