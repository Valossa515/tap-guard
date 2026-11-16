# Plan: SPEC-002 Integração contínua

> Issue #3 · Etapa 0 (Fundação) · Depende de SPEC-001 (#2, fechada) · Requisito RNF-08 (cobertura de 80% no domínio)
> Spec: `.agent/specs/features/spec-002-integracao-continua.spec.toon` (FT-002) · Aceite: `.agent/specs/acceptance/spec-002-integracao-continua.acceptance.toon`
> Branch: `spec-002-integracao-continua` · Planejado em 2026-11-16

## Summary
Completar o CI que a SPEC-001 deixou rodando. Antes, limpar do job "Build e testes" os passos que a SPEC-001 tornou desnecessários. Depois, três itens: publicar o APK de debug do `app` como artefato do workflow; medir a cobertura do `core-domain` com Kover e exigir no mínimo 80% (RNF-08); validar o `gradle-wrapper.jar` antes de executá-lo. O que já existe não muda: o `ci.yml` roda `./gradlew build` no JDK 21, que inclui ktlint, detekt, lint e testes, e a ruleset da `main` já bloqueia PR com os checks "Regras do projeto" e "Build e testes" falhando.

## Scope
- **Módulos afetados:** `core-domain` (Kover) e o workflow de CI. Nenhum código de produção.
- **Arquivos modificados:** `.github/workflows/ci.yml`, `gradle/libs.versions.toml`, `build.gradle.kts` (raiz, `apply false`), `core-domain/build.gradle.kts`, `core-domain/README.md`, `CONTRIBUTING.md` (Ambiente), `.agent/runbook.md`, `.agent/project-reference.md` (§8, §9), `.agent/conventions.md` (Coverage), `.agent/context.md`, `.agent/SKILLS.md` (Project Overrides), `.agent/decisions.md`.
- **Novos arquivos:** nenhum.
- **Fora do escopo:** Kover nos módulos Android; APK do `decoy-app` e relatório de cobertura como artefatos; atualizar `actions/checkout` (v4 → v7) e `actions/setup-java` (v4 → v6); assinatura de release; mexer na ruleset da `main`.

## Decisões já tomadas (usuário, 2026-11-16)

| Item | Decisão |
|------|---------|
| Limpeza do job "Build e testes" | remover o passo "Verifica se o projeto Gradle existe", as condições `if` que dependem dele e o `chmod +x gradlew` (o bit executável está no índice desde o commit `dfce9ce`). Commit próprio |
| Referência das actions novas | pelo SHA do commit da versão, com a versão em comentário na mesma linha. SHAs conferidos na página de releases, na API do GitHub (tag anotada resolvida até o commit) e no `git ls-remote`: `gradle/actions` v6.4.1 = `8da46b539cd8fe6ab27ca75128919a18d757604c`; `actions/upload-artifact` v7.0.2 = `cf430e030ddbb5b0abf93d22962f4752f3646cd9`. `checkout` e `setup-java` continuam como estão (tag `@v4`) |
| Validação do wrapper | `gradle/actions/wrapper-validation` v6.4.1 (última, 28/10/2026). A v6 tornou proprietário o componente de cache (`gradle-actions-caching`, Termos de Uso da Gradle), mas a `wrapper-validation` não o usa. Registrar o aviso no ADR |
| Artefato | só o APK de debug do `app` (`app/build/outputs/apk/debug/app-debug.apk`). O do `decoy-app` entra quando os testes ponta a ponta precisarem dele |
| Upload | `actions/upload-artifact` v7.0.2 (última, 07/10/2026) |
| Kover | 0.9.11 (último, 29/09/2026, Maven Central e Plugin Portal), aplicado **só no `core-domain`** |
| Regra de cobertura | ligada já nesta spec: mínimo de 80% de linhas no `core-domain` (ver "Cobertura com zero classes") |

Versões conferidas em 2026-11-16 no Maven Central, no Gradle Plugin Portal e nas releases do GitHub (`Kotlin/kotlinx-kover`, `gradle/actions`, `actions/upload-artifact`).

### Cobertura com zero classes: experimento e decisão
Experimento em projeto descartável fora do repositório (Gradle 9.7.0, Kotlin 2.4.21, JDK 21, Kover 0.9.11, JUnit 5, regra `minBound(80)`):

| Cenário | `koverVerify` | Observação |
|---------|---------------|-----------|
| A: nenhum código de produção, só o teste de fumaça | **passa** (saída 0) | relatório com 0/0 em todos os contadores; `koverLog` imprime "No sources"; sem erro |
| B: uma classe 100% coberta | passa | 100% de linhas |
| C: uma classe com 20% de cobertura | **falha** (saída 1) | `Rule violated: lines covered percentage is 20.000000, but expected minimum is 80` |
| E1: só uma interface | passa | "No sources": interface não tem código executável |
| E2: interface + `data class` sem teste | **falha** (saída 1) | 0% |

Depois, numa cópia descartável do repositório (`git archive`, fora do repo), com o Kover só no `core-domain`: `./gradlew build` inteiro passa (AGP 9.3.3, ktlint, detekt, lint e testes juntos, sem aviso de depreciação do Kover). Uma classe sem teste no `core-domain` faz o `./gradlew build` falhar no `:core-domain:koverVerify`. O `check` já depende do `koverVerify`, então o CI não precisa de passo extra.

**Proposta: ligar a regra agora (primeira saída).** Motivos:
- Com zero classes a regra passa de forma correta. Não existe linha descoberta, e o Kover trata 0/0 como satisfeito, sem erro e sem falso negativo. Não é contorno: nada foi criado para ter o que medir.
- A regra começa a valer sozinha na primeira classe com código executável (cenários C e E2). A SPEC-010 já nasce sob a regra, sem depender de alguém lembrar de ligá-la.
- Configurar agora e ligar só na SPEC-010 (segunda saída) seria trabalho em dois PRs, e o risco é a SPEC-010 entrar com modelos sem teste antes da regra.
- O ponto fraco é que hoje a regra passa sem medir nada, e uma configuração quebrada também mostraria "No sources". Para cobrir isso, a tarefa 6 roda a verificação negativa no repositório real: uma classe temporária sem teste precisa derrubar o build, e a alteração é desfeita sem commit, como na SPEC-001.

## Approach
- Seguir `skills/android/SKILL.md` (Gradle) e `skills/android/testing.md` (cobertura do domínio). Versões só no catálogo; plugin declarado na raiz com `apply false` e aplicado no próprio `core-domain` (sem `subprojects {}`, sem plugin de convenção, como na SPEC-001).
- Kover só no `core-domain`: é o único módulo com meta de cobertura (RNF-08), e o Kover tem um bug aberto em módulos `com.android.application` no AGP 9 com o Kotlin embutido (`Kotlin/kotlinx-kover#815`, ligado ao configuration cache, que o projeto não usa).
- Validação do wrapper como passo do job "Build e testes", **antes** de qualquer execução do `gradlew`. Num job à parte, ela rodaria em paralelo com o build, que já teria executado o JAR. E um job novo não seria check obrigatório sem alterar a ruleset.
- Upload do APK como passo do job "Build e testes", depois do build e só se ele passar. O passo falha se o APK não existir (sem artefato vazio). Retenção: o padrão do repositório.
- O checksum do `gradle-wrapper.jar` atual já foi conferido: igual ao publicado para o Gradle 9.7.0 (`7a9ce74c…262c5d`).

## Tasks
1. [x] **Limpeza do job "Build e testes"** (`ci: remove passos que a SPEC-001 tornou desnecessários`): sai o passo "Verifica se o projeto Gradle existe", saem as condições `if` que dependem dele e sai o `chmod +x gradlew`.
2. [x] **Validação do wrapper** (`ci:`): passo `gradle/actions/wrapper-validation` v6.4.1, referenciado pelo SHA com a versão em comentário, no job "Build e testes", antes do primeiro uso do `gradlew`.
3. [x] **APK de debug como artefato** (`ci:`): passo `actions/upload-artifact` v7.0.2, referenciado pelo SHA com a versão em comentário, depois de `./gradlew build`, com `app/build/outputs/apk/debug/app-debug.apk`, falhando se o arquivo não existir.
4. [x] **Kover no `core-domain`** (`chore:`): Kover 0.9.11 no catálogo, `apply false` na raiz, aplicado no `core-domain` com a regra de 80% de linhas. `./gradlew build` verde.
5. [x] **Documentação** (`docs:`): comandos `koverVerify`, `koverHtmlReport` e `koverXmlReport` e onde fica o relatório (`core-domain/build/reports/kover/`) em `CONTRIBUTING.md` (Ambiente), `runbook.md`, `project-reference.md` §8 e §9 e `core-domain/README.md`; o artefato e a validação do wrapper no `runbook.md`; Kover confirmado em `conventions.md` (Coverage), `context.md` e `SKILLS.md` (Project Overrides); ADR-012 em `decisions.md` (Kover só no domínio com a regra ligada desde já, com o experimento; `wrapper-validation` v6 e o aviso de licença; só o APK do `app`).
6. [x] **Conferência dos critérios:** localmente, uma classe temporária sem teste no `core-domain` faz o `./gradlew build` falhar no `koverVerify`, e a alteração é desfeita sem commit. No CI da PR: o passo de validação aparece no log e encontra um wrapper válido, e o artefato com o APK de debug aparece no run e baixa.
   - 2026-11-16, local, desfeito sem commit: classe `CoverageProbe` sem teste no `core-domain` → `./gradlew build` falha em `:core-domain:koverVerify` (`Rule violated: lines covered percentage is 0.000000, but expected minimum is 80`). Depois de desfazer, `./gradlew build` volta a passar com o `koverVerify` executando.
   - 2026-11-16, [PR #44](https://github.com/Valossa515/tap-guard/pull/44), [CI #10](https://github.com/Valossa515/tap-guard/actions/runs/43929332317), commit `0c7d191`: os jobs "Regras do projeto" e "Build e testes" passaram. O log registra `Found known Gradle Wrapper JAR files` e o checksum `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d` antes do build; `:core-domain:koverVerify` executou e o build terminou com `BUILD SUCCESSFUL`. O [artefato app-debug](https://github.com/Valossa515/tap-guard/actions/runs/43929332317/artifacts/14257623233) foi publicado (928738 bytes) e baixado com sucesso pelo conector GitHub como `spec-002-app-debug.zip`.
   - Conferência complementar: `CONTRIBUTING.md` preservou os comandos e as seções "Cobertura" e "CI" após o merge; relatórios HTML e XML presentes em `core-domain/build/reports/kover/`; `git diff --check origin/main...HEAD` sem erros. A API de regras aplicáveis à `main` confirmou "Regras do projeto" e "Build e testes" como checks obrigatórios (ruleset `27384263`).

## Database changes
- Nenhuma.

## Risks & open questions
- **Regra passa sem medir enquanto não houver código.** Esperado e verificado (cenário A). Coberto pela verificação negativa da tarefa 6.
- **A SPEC-010 já nasce sob a regra.** As `data class` do domínio têm código executável (cenário E2): os modelos da SPEC-010 vão precisar de testes para chegar a 80%. É o que o RNF-08 pede; vale registrar no plano da SPEC-010.
- **Kover em módulos Android no AGP 9:** bug aberto (#815). Não afeta esta spec, porque o Kover fica só no `core-domain`. Se um dia for preciso estender, rever.
- **Kover 0.9.11 reverteu a correção da 0.9.10** (#827/#831, cobertura 0% com configuration cache). O projeto não liga o configuration cache. Se ligar, rever.
- **Licença da `gradle/actions` v6:** não afeta a `wrapper-validation`. Usar outra sub-action (`setup-gradle` com cache) exigiria aceitar os Termos de Uso. Fora desta spec: o cache do Gradle continua com o `cache: gradle` do `setup-java`.
- **Caso negativo da validação do wrapper** (JAR adulterado) não será testado por nós: precisaria de um PR descartável contra a `main`. Fica a cargo da própria action. Testar só se o usuário pedir.
- `actions/checkout@v4` e `actions/setup-java@v4` estão atrás das versões atuais e referenciados por tag, não por SHA. Fora do escopo, mas é candidato a PR próprio.
- **Sem o passo "Verifica se o projeto Gradle existe"**, o job falha se o `gradlew` sumir, em vez de pular o build. É o comportamento desejado agora que o projeto Gradle existe.

## Acceptance criteria
- [x] PR com falha de lint ou teste fica bloqueado (AC-001, já atendido: `./gradlew build` + ruleset; checks obrigatórios reconferidos pela API)
- [x] APK de debug do `app` disponível como artefato do workflow (AC-002, AC-003; download do CI #10 e configuração `if-no-files-found: error` conferidos)
- [x] Wrapper validado antes da execução no CI (AC-004; log do CI #10)
- [x] Actions novas referenciadas por SHA com a versão em comentário (AC-014; inspeção de `ci.yml`)
- [x] Job "Build e testes" sem o passo de verificação do `gradlew`, sem as condições `if` e sem `chmod` (AC-015; inspeção de `ci.yml`)
- [x] Kover no `core-domain` com 80% exigido, passando hoje e falhando com código sem teste (AC-005 a AC-008; experimento local, relatórios e CI #10)
- [x] Versões só no catálogo e Kover só no `core-domain` (AC-009, AC-010; inspeção dos scripts Gradle e catálogo)
- [x] ktlint, detekt e testes passam (AC-011; build local e CI #10)
- [x] Documentação atualizada (AC-012; diff revisado, inclusive após o merge)
- [x] CI da PR verde (AC-013; ambos os jobs do CI #10 passaram, com validação, build e upload executados)
