# Plan: SPEC-001 Estrutura do projeto

> Issue #2 · Etapa 0 (Fundação) · Depende de SPEC-000 (#1, fechada) · Requisitos RNF-05, RNF-08
> Spec: `.agent/specs/features/spec-001-estrutura.spec.toon` (FT-001) · Aceite: `.agent/specs/acceptance/spec-001-estrutura.acceptance.toon`
> Branch: `spec-001-estrutura` · Planejado em 2026-11-16

## Summary
Criar o monorepo Gradle (Kotlin DSL + catálogo de versões) com os seis módulos da arquitetura: `core-domain`, `core-detection`, `core-nfc`, `core-data`, `app` e `decoy-app`. Cada módulo nasce com o mínimo para compilar e um teste trivial, sem nenhuma funcionalidade. Também entram nesta spec os plugins de ktlint e detekt, aplicados a todos os módulos. É a base que todas as outras specs usam, e prova desde o início que o `core-domain` fica livre de Android (RNF-08) e que o minSdk é 26 (RNF-05).

## Scope
- **Módulos afetados:** todos, criados do zero. Não havia código no repositório.
- **Novos arquivos (esperados):** Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), `settings.gradle.kts`, `build.gradle.kts` raiz, `gradle.properties`, `gradle/libs.versions.toml`, um `build.gradle.kts` por módulo, os manifestos de `app`, `decoy-app` e das bibliotecas Android, um teste trivial por módulo, um `README.md` por módulo.
- **Arquivos modificados:** `.github/workflows/ci.yml` (JDK 21), `CONTRIBUTING.md` (seção "Ambiente"), `.agent/runbook.md`, `.agent/project-reference.md`, `.agent/context.md`, `.agent/SKILLS.md` (Project Overrides), `.agent/decisions.md`.
- **Fora do escopo:** integração explícita de ktlint e detekt ao CI e publicação do artefato de debug (SPEC-002), cobertura com Kover (SPEC-002), qualquer tela, Activity, Compose, Hilt, Room, permissão ou dependência de biblioteca de produção.

## Decisões já tomadas (usuário, 2026-11-16)

| Item | Decisão |
|------|---------|
| Identificador base | `io.github.valossa515.tapguard` |
| applicationIds | `app` = `io.github.valossa515.tapguard`; `decoy-app` = `io.github.valossa515.tapguard.decoy` |
| Namespaces | `io.github.valossa515.tapguard.core.domain`, `.core.detection`, `.core.nfc`, `.core.data`; `app` = `io.github.valossa515.tapguard`; `decoy-app` = `io.github.valossa515.tapguard.decoy` |
| `core-domain` | só o plugin `kotlin("jvm")` |
| Configuração | sem `build-logic` e sem plugins de convenção: cada módulo declara a própria configuração, usando o catálogo |
| ktlint | plugin `org.jlleitschuh.gradle.ktlint` 14.2.0, motor ktlint 1.8.0 |
| detekt | 1.23.8 (último estável). Se não rodar, sai desta spec para a SPEC-002, sem contorno e sem trocar de versão |
| JUnit | 5.14.4 no `core-domain` (+ `junit-platform-launcher` 1.14.4); 4.13.2 nos módulos Android |
| Combinação de build | Kotlin 2.4.21 + AGP 9.3.3 + Gradle 9.7.0 (ver "Por que AGP 9.3.3") |
| JDK | 21: toolchain de todos os módulos e JDK do CI (`ci.yml`, commit `ci: usa JDK 21`) |
| JDK do daemon | fixado em 21 por `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`). Escrito à mão porque o `./gradlew updateDaemonJvm` do Gradle 9.7.0 exige um repositório de download de JDK (plugin foojay), que não queremos configurar. Verificado em 2026-11-16: com `JAVA_HOME` no JDK 25, o daemon roda no Temurin 21 e `./gradlew help` passa |
| SDK | minSdk 26; targetSdk 36 (exigência atual da Play Store); compileSdk 36 |
| Manifesto | sem `INTERNET` e sem nenhuma outra permissão nesta spec |
| Architect | pulado: não há contrato de API nesta spec; implementação direto deste plano |
| Testes de fumaça | em specs de estrutura, o builder escreve os testes de fumaça (os testes triviais das tarefas 2 a 4), porque eles verificam a configuração do JUnit em cada módulo. Exceção à regra do builder de não escrever testes |
| Documentação | fica na tarefa 6, em commit `docs:` próprio |
| Formato da spec | `status` = código de saída do comando (Project Overrides › Spec format) |

Versões conferidas em 2026-11-16 no Maven Central, no Google Maven, no Gradle Plugin Portal e em services.gradle.org. Compatibilidade conferida em developer.android.com (AGP 9.3: Gradle ≥ 9.5.0, JDK ≥ 17, API até 37) e em kotlinlang.org (tabela do KGP).

### Por que AGP 9.3.3
- A tabela de compatibilidade do Kotlin 2.4.20–2.4.21 vai até o AGP 9.3.1. A tabela não acompanhou as correções: o KGP 2.4.20 saiu em 07/09/2026, quando o AGP 9.3.2 (24/08/2026) já existia, e mesmo assim lista só até 9.3.1.
- As versões 9.3.2 e 9.3.3 são correções da mesma linha 9.3, sem funcionalidades novas. A 9.3.2 corrige "AGP 9.3 lint crashes on JDK 17 (NoSuchMethodError List.removeLast)". O `./gradlew build` roda o lint dos módulos Android, e na época da escolha o CI usava JDK 17.
- Por isso o usuário escolheu o 9.3.3, aceitando ficar um patch fora da tabela. O teste de compatibilidade da tarefa 1 (Kotlin 2.4.21 + AGP 9.3.3 + Gradle 9.7.0 + JDK 21) passou em compilação, D8, lint, ktlint e detekt.

## Approach
- Seguir `skills/android/SKILL.md` (seções Architecture, Gradle e Naming) e `skills/android/testing.md` (runner por tipo de módulo, nomes de teste com crases).
- **Divergência com a skill:** a skill foi escrita para AGP 8.x e mostra o plugin `org.jetbrains.kotlin.android`. No AGP 9.x o Kotlin já vem embutido e esse plugin não deve ser aplicado nos módulos Android. Registrar isso em `.agent/SKILLS.md` › Project Overrides, que tem precedência sobre a skill.
- Dependências entre módulos conforme `architecture.md`: `core-domain` não depende de nada; `core-detection`, `core-nfc` e `core-data` dependem de `core-domain`; `app` depende de todos os `core-*`; `decoy-app` não depende de nada.
- Todas as versões, inclusive SDKs e JDK, ficam só em `gradle/libs.versions.toml`.
- Cada módulo declara a própria configuração (plugins, `android {}`, SDKs, dependências) referenciando o catálogo. Sem `build-logic`, sem plugins de convenção e sem blocos `subprojects {}`/`allprojects {}` que configurem os módulos pela raiz.

## Tasks
1. [x] **Base do build e verificação de compatibilidade:** wrapper Gradle 9.7.0, `settings.gradle.kts`, catálogo, plugins declarados na raiz. Verificado em projeto descartável (módulo JVM + biblioteca Android + app).
   - Com JDK 25, o detekt 1.23.8 falhou e saiu da spec. O usuário trocou para JDK 21, e com JDK 21 o build do projeto descartável passou inteiro (compilação, D8, lint, ktlint, detekt, test, assemble). O detekt voltou para esta spec.
   - Commits: `chore: cria a base do build Gradle (SPEC-001, tarefa 1)`, `ci: usa JDK 21`, `chore: usa toolchain JDK 21 e restaura o detekt (SPEC-001)`.
2. [x] **`core-domain`:** só `kotlin("jvm")`, toolchain JDK 21, JUnit 5 com launcher, um teste de fumaça.
3. [x] **`core-detection`, `core-nfc`, `core-data`:** bibliotecas Android com Kotlin embutido do AGP 9, minSdk 26, dependência de `core-domain`, JUnit 4, um teste de fumaça cada.
4. [x] **`app` e `decoy-app`:** aplicativos com applicationId e namespace próprios, minSdk 26, targetSdk 36, manifesto sem permissões, um teste de fumaça cada. O `app` depende dos quatro `core-*`.
5. [x] **ktlint e detekt em todos os módulos:** `./gradlew build` verde, incluindo `ktlintCheck`, `detekt` e `test`.
6. [x] **Documentação (commit `docs:` próprio):** `README.md` de cada módulo (responsabilidade e dependências); comandos em `runbook.md` e na seção "Ambiente" do `CONTRIBUTING.md`, incluindo que é preciso ter um JDK 21 instalado e que `gradle/gradle-daemon-jvm.properties` faz o Gradle rodar nele mesmo com outro `JAVA_HOME`; versões e comandos em `project-reference.md` (§1, §8, §9) e `context.md`; application id, versões, JDK 21 e o override de Kotlin embutido em `SKILLS.md`; um ADR em `decisions.md` com o identificador e a combinação de versões.
7. [x] **Conferência dos critérios:** rodar localmente as verificações negativas (import `android.*` no domínio, violação de ktlint, violação de detekt, teste falhando) e desfazê-las sem commit.
   - 2026-11-16, todas desfeitas sem commit: import `android.content.Context` no `core-domain` → `:core-domain:build` falha (`Unresolved reference 'android'`); espaços extras num teste → `ktlintCheck` falha; `if` vazio → `detekt` falha (`EmptyIfBlock`); `assertTrue(false)` → `build` falha em `:core-domain:test`. Depois de desfazer, `./gradlew build` volta a passar.

## Database changes
- Nenhuma. O Room entra em specs futuras.

## Risks & open questions
- **O Gradle precisa rodar no JDK 21, não só compilar com ele.** O detekt 1.23.8 roda dentro do processo do Gradle e o compilador Kotlin 2.0.21 embutido nele não reconhece o JVM 25 (`IllegalArgumentException: 25` em `JavaVersion.current()`). No CI o `setup-java` já põe o Gradle no JDK 21. Localmente, `gradle/gradle-daemon-jvm.properties` faz o daemon rodar num JDK 21 instalado, qualquer que seja o `JAVA_HOME`. Sem JDK 21 instalado, o Gradle não acha um JVM compatível e para.
- **CI já roda `./gradlew build`.** O job "Build e testes" do `ci.yml` compila assim que existe `gradlew`. Como `build` inclui `check`, ktlint e detekt passam a rodar no CI já nesta PR. A integração explícita continua sendo da SPEC-002.
- **Android SDK no CI:** o runner precisa da plataforma 36 e do Build Tools 36. O AGP baixa o que falta se as licenças estiverem aceitas no runner. Verificar no primeiro CI da PR.
- **Formato dos scripts Gradle:** o ktlint 1.8.0 também verifica os `build.gradle.kts` (regra `chain-method-continuation`). Expressões como `libs.versions.minSdk.get().toInt()` precisam quebrar a linha antes de cada `.`.
- **Aviso de depreciação:** um dos plugins (ktlint-gradle ou detekt) usa `ReportingExtension.file(String)`, que o Gradle 10 vai remover. Hoje é só aviso.
- **Gradle 9** exige `junit-platform-launcher` explícito no runtime de teste dos módulos JUnit 5.
- O `.gitattributes` já força LF em `gradlew` e CRLF em `gradlew.bat`.

## Acceptance criteria
- [x] `./gradlew build` passa (AC-001)
- [x] `core-domain` não tem nenhuma dependência Android (AC-002, AC-003)
- [x] minSdk 26 configurado (AC-004) e targetSdk 36 nos aplicativos (AC-005)
- [x] Versões só no catálogo (AC-006) e dependências entre módulos conforme a arquitetura (AC-007)
- [x] Nenhum manifesto declara `INTERNET` (AC-008)
- [x] ktlint, detekt e testes passam e falham quando devem (AC-009 a AC-014)
- [x] Nenhuma funcionalidade, permissão ou dependência de produção (AC-015)
- [x] applicationIds e namespaces definidos (AC-016)
- [x] Configuração declarada em cada módulo, sem `build-logic` nem plugins de convenção (AC-019)
- [x] Documentação atualizada (AC-017)
- [x] CI da PR verde (AC-018) — CI do PR #42 passou em 2026-11-16: "Regras do projeto" e "Build e testes" verdes, e o job "Build e testes" rodou o `./gradlew build` de verdade no Temurin 21 (`BUILD SUCCESSFUL in 2m 10s` no log), sem pular
