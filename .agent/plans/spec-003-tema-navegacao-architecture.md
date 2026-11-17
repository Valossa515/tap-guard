# Architecture: SPEC-003 Tema e navegação

> Entrada: `.agent/plans/spec-003-tema-navegacao.md`, `.agent/specs/features/spec-003-tema-navegacao.spec.toon` (FT-003), `.agent/specs/acceptance/spec-003-tema-navegacao.acceptance.toon`
> Contrato: `.agent/specs/contracts/spec-003-tema-navegacao.contract.toon` · 2026-11-17

## Summary
Tudo acontece no módulo `app`, no pacote `io.github.valossa515.tapguard`, organizado como a skill Android define: a `MainActivity` na raiz do pacote, `navigation/` (rotas tipadas e `NavHost`), `ui/theme/` (paleta e `MaterialTheme`), `ui/components/` (componente de nível) e um pacote por tela em `ui/<tela>/`. As telas não têm ViewModel nem estado: cada uma é um composable `{Feature}Screen` que recebe lambdas de navegação. O `NavHost` é o único lugar que conhece o `NavController`. Os testes rodam todos em `src/test` com Robolectric e JUnit 4, mais um teste JUnit 4 puro para as cores.

## Pattern Analysis
- **Padrões existentes no repositório** (ainda não há código de produção no `app`):
  1. Cada módulo declara a própria configuração com aliases do catálogo e plugins `apply false` na raiz (`app/build.gradle.kts`, `decoy-app/build.gradle.kts`, `build.gradle.kts`).
  2. Testes em `src/test/kotlin/<pacote>` com JUnit 4 nos módulos Android e nomes com crases (`AppSmokeTest`, `CoreDataSmokeTest`, `DecoyAppSmokeTest`).
  3. Plugins de qualidade aplicados em cada módulo (`alias(libs.plugins.ktlint)`, `alias(libs.plugins.detekt)`), com a versão do motor ktlint vinda do catálogo.
- **Padrão seguido:** os três acima para o build e os testes. Para o código de UI, que ainda não existe no repositório, segue a skill (`skills/android/SKILL.md`, seções Architecture, Naming, UI — Compose e Navigation) e o projeto descartável da SPEC-003, que validou a mesma estrutura com Activity, Material 3, rotas tipadas e Robolectric.

## Layer Decisions

| Camada (pacote) | Decisão | Justificativa |
|------|---------|---------------|
| build | catálogo (versões, bibliotecas, plugins), `apply false` na raiz, `app/build.gradle.kts`, `.editorconfig`, `app/detekt.yml` | BR-011 e decisões aprovadas de nome de composable; mesmo padrão da SPEC-001 e SPEC-002 |
| manifesto e recursos | manifesto com a `MainActivity` como única Activity exportada; tema da janela claro e escuro (`values/themes.xml`, `values-night/themes.xml`); `strings.xml`; quatro vetores em `res/drawable` | BR-010, BR-012, decisão de ícones vetoriais; nome e ícone provisórios |
| entrada (raiz do pacote) | `MainActivity` só chama `setContent { TapGuardTheme { TapGuardNavHost() } }` | Skill: a Activity não tem lógica; o grafo fica em `navigation/` |
| `navigation` | `Routes.kt` com as nove rotas `@Serializable` (`{Feature}Route`; `AppDetailRoute(packageName: String)`); `TapGuardNavHost.kt` com o fluxo, `popUpTo` da T1 inclusiva ao ir para a T2 e retorno da T6 para a T2 existente | BR-001 a BR-005; skill Navigation |
| `ui/theme` | `Color.kt` (paleta fixa clara e escura, incluindo as cores dos níveis) e `Theme.kt` (`TapGuardTheme`: `lightColorScheme`/`darkColorScheme` por `isSystemInDarkTheme()`, sem `dynamicColorScheme`; fornece o modo em uso por `LocalTapGuardDarkTheme`) | BR-009 |
| `ui/components` | `RiskLevelUi.kt` (enum `GREEN`, `YELLOW`, `RED`), `RiskLevelColors.kt` (função pura nível + modo → cores de fundo e conteúdo, lendo a paleta de `ui/theme`) e `RiskLevelIndicator.kt` (composable `(level, modifier)`: ícone com `contentDescription` + frase, nas cores do nível no modo lido de `LocalTapGuardDarkTheme`) | BR-006 a BR-008; RNF-06. A semântica de nível fica em `components`; `theme` continua genérico. Dependência só `components → theme` |
| `ui/<tela>` | um pacote e um arquivo `{Feature}Screen.kt` por tela (T1 a T9): título da seção 10 e botões de saída, todos por `stringResource` | BR-002, BR-010; sem ViewModel nem `*Content` porque não há estado. A spec de cada tela acrescenta o ViewModel e o par `Screen`/`Content` |
| docs | `app/README.md`, `CONTRIBUTING.md`, `runbook.md`, `project-reference.md`, `conventions.md`, `SKILLS.md`, `decisions.md` (ADR-013) | AC-026; decisões do usuário (permissões do AndroidX, origem dos ícones) |

Nomes das telas, em inglês como manda a convenção de código: T1 `onboarding/OnboardingScreen`, T2 `home/HomeScreen`, T3 `scanresult/ScanResultScreen`, T4 `appdetail/AppDetailScreen`, T5 `cardtestintro/CardTestIntroScreen`, T6 `cardtestreading/CardTestReadingScreen`, T7 `emergency/EmergencyScreen`, T8 `settings/SettingsScreen`, T9 `alerthistory/AlertHistoryScreen`. As rotas seguem o mesmo nome com sufixo `Route`.

## Dependencies
- **Reusing:** a configuração existente do `app` (namespace, applicationId, toolchain, dependências de projeto dos quatro `core-*`, ktlint, detekt); o catálogo; o `AppSmokeTest`, que continua como está.
- **New** (versões aprovadas no plano, todas no catálogo):
  - produção: Compose BOM 2026.09.00 (`material3`), `activity-compose` 1.13.0, `navigation-compose` 2.10.2 (o `kotlinx-serialization-core` das rotas vem por ele; a `kotlinx-serialization-json`, aprovada no plano, foi retirada na emenda 1.3 por não ter uso);
  - plugins: `org.jetbrains.kotlin.plugin.compose` e `org.jetbrains.kotlin.plugin.serialization` 2.4.21;
  - teste: `ui-test-junit4` (versão do BOM), Robolectric 4.18, `androidx.test:core` 1.7.0.
- **Não entram:** `material-icons`, `ui-test-manifest`, `navigation-testing`, `ui-tooling`.

## Database Changes
- Nenhuma.

## Test Strategy Rationale
- **Navegação pela Activity real** (`createAndroidComposeRule<MainActivity>()`): cobre de uma vez a `MainActivity`, o tema, o `NavHost`, as rotas e as nove telas, do jeito que o usuário usa. Cada aresta do fluxo vira um cenário (clique pelo rótulo e checagem pelo título da tela). A pilha é verificada pelo comportamento observável: depois de sair da T1, e depois de T6→T2, o "voltar" fecha a Activity. Assim não precisa de `TestNavHostController` nem da dependência `navigation-testing`.
- **Componente de nível isolado** (`Robolectric.buildActivity(ComponentActivity::class.java)` + `createEmptyComposeRule()`, divergência aprovada), sempre dentro do `TapGuardTheme`: frase exata e ícone (por `contentDescription`) de cada nível, em claro e em `+night`. A cor de cada nível é garantida pelo teste puro das cores e pela revisão de que o componente usa `riskLevelColors`. Desde a emenda 1.1, um cenário com tema forçado diferente do sistema confere a cor do texto (via `GetTextLayoutResult`, sem comparar pixels).
- **Cores em JUnit 4 puro:** `riskLevelColors` não depende de Android. O teste confere que os três níveis têm cores distintas e que cada par fundo/conteúdo tem contraste de pelo menos 4,5:1 (AA), em claro e escuro. O contraste é calculado no próprio teste com `Color.luminance()`.
- **Tema:** composable isolado dentro de `TapGuardTheme`, que lê `MaterialTheme.colorScheme`. Ele confere o esquema claro no modo claro e o escuro em `+night`.
- **Permissões e Activity exportada:** Robolectric lê `requestedPermissions` e as Activities do `PackageManager`, que refletem o manifesto mesclado (conferido no projeto descartável). Falha se houver permissão fora da lista explícita, ou Activity exportada além da `MainActivity`.
- **SDK das classes Robolectric:** `@Config(sdk = [36])` em cada classe (o targetSdk). Não entra um `robolectric.properties`, que seria arquivo fora do plano.
- **Excluídos:** rotas (dados `@Serializable` sem lógica), telas (cobertas pela navegação), `MainActivity` (coberta pela navegação), constantes de cor (cobertas pelo teste de cores), recursos (revisão: AC-018 e AC-024).

## Emenda 1.1 (2026-11-17, review)
- **Problema** (🟡 do review): o `RiskLevelIndicator` tinha um parâmetro `darkTheme` com padrão `isSystemInDarkTheme()`. Isso divergia do contrato `(level, modifier)`, deixava o `modifier` fora da última posição opcional (skill) e ignorava um `TapGuardTheme(darkTheme = …)` forçado diferente do sistema.
- **Decisão:** o `TapGuardTheme` fornece o modo em uso por `LocalTapGuardDarkTheme` (`staticCompositionLocalOf`, que falha com mensagem clara se for lido sem o tema em volta). O `RiskLevelIndicator` volta a `(level, modifier)` e chama `riskLevelColors(level, LocalTapGuardDarkTheme.current)`.
- **Alternativa:** prover as próprias cores de nível por `CompositionLocal`. Rejeitada: duplicaria a função pura `riskLevelColors`, que já é testada em JUnit puro.
- **Teste:** novo cenário `forced-dark-theme-on-light-system-uses-dark-level-colors`. A cor é lida do estilo final do texto via `SemanticsActions.GetTextLayoutResult`, sem comparar pixels. Verificação negativa: com o componente fixo no modo claro, só esse cenário falha.

## Trade-offs & Alternatives Considered
- **`ui-test-manifest` em `debugImplementation` (como a skill mostra):** rejeitado. Põe uma `ComponentActivity` exportada no APK de debug, que o CI publica.
- **`TestNavHostController` (`navigation-testing`):** rejeitado. Exigiria uma dependência nova, e o comportamento observável basta.
- **Comparar pixels para checar a cor** (`captureToImage` com `@GraphicsMode(NATIVE)`): rejeitado. É frágil e lento; o teste puro das cores cobre a regra.
- **Ícone adaptativo** (`mipmap-anydpi-v26` com fundo e primeiro plano): adiado até o ícone definitivo. O ícone provisório é um único vetor usado em `android:icon`.
- **Tema de plataforma direto no manifesto** (como no projeto descartável): rejeitado (decisão do usuário, 2026-11-17). No modo escuro, a janela piscaria em branco antes do Compose. Entram `res/values/themes.xml` (`Theme.TapGuard` sobre `android:Theme.Material.Light.NoActionBar`) e `res/values-night/themes.xml` (sobre `android:Theme.Material.NoActionBar`).
- **`RiskLevelColors` em `ui/theme`:** rejeitado. Faria `theme` depender do tipo de nível, que é de `components`.

## Risks
- **`android.useAndroidX=true` e `--add-opens`** (presentes no projeto descartável) não estão no contrato. Se o build ou os testes exigirem, o builder para e pergunta, em vez de acrescentar.
- **`Color.luminance()` fora do Robolectric:** a classe `Color` é Kotlin puro, mas, se o teste de cores esbarrar em stub do Android, o tester troca o runner da classe para Robolectric. É mudança de execução, não de escopo.
- **Tamanho da PR:** estimativa de ~550 linhas de produção e ~350 de testes, acima das 500 que o planner usa para dividir em batches. O projeto define "um pull request por spec" (`CONTRIBUTING.md`), então a spec fica inteira. Registrado para o reviewer.
- **CI e plataforma 37:** confirmar no primeiro run que o runner baixa a `android-37.0`.
