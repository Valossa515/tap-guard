# Plan: SPEC-003 Tema e navegação

> Issue #4 · Etapa 0 (Fundação) · Depende de SPEC-001 (#2, fechada) · Requisitos RNF-06 (acessibilidade), RNF-07 (linguagem)
> Spec: `.agent/specs/features/spec-003-tema-navegacao.spec.toon` (FT-003, `interface: ui`) · Aceite: `.agent/specs/acceptance/spec-003-tema-navegacao.acceptance.toon`
> Branch: `spec-003-tema-navegacao` · Planejado em 2026-11-16

## Summary
Dar ao `app` uma Activity com Compose, um tema Material 3 claro e escuro, os componentes de nível (verde, amarelo, vermelho, sempre com cor, ícone e frase) e o grafo de navegação com as nove telas da seção 10 (T1 a T9) vazias. É a base visual e de navegação das Etapas 3 a 6. Sem ViewModel, sem Hilt, sem dados e sem nenhuma permissão nova. Para as bibliotecas do Compose 1.12 compilarem, o `compileSdk` sobe para 37; o `targetSdk` continua em 36.

## Scope
- **Módulos afetados:** `app`; o `compileSdk` do catálogo vale para todos os módulos Android.
- **Novos arquivos (esperados):** `.editorconfig` (raiz), `app/detekt.yml`, a Activity de entrada, o tema (cores e `MaterialTheme`), o tema da janela (`res/values/themes.xml` e `res/values-night/themes.xml`), os componentes de nível, as rotas e o `NavHost`, as nove telas vazias, os recursos (`strings.xml`, ícone do app e ícones de nível em vetor), os testes Robolectric de navegação e dos componentes.
- **Arquivos modificados:** `gradle/libs.versions.toml`, `build.gradle.kts` (raiz, plugins com `apply false`), `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/README.md`, `CONTRIBUTING.md`, `.agent/runbook.md`, `.agent/project-reference.md` (§1, §3, §4, §8, §9), `.agent/conventions.md`, `.agent/SKILLS.md` (SDK, composables, permissões), `.agent/decisions.md` (novo ADR-013).
- **Fora do escopo:** conteúdo real das telas (estados carregando, vazio, conteúdo e erro chegam com cada tela), ViewModels, Hilt (SPEC-004), persistência do onboarding, telas do sistema (desinstalar, configurações de NFC), notificações, cores dinâmicas, nome e ícone definitivos.

## Decisões já tomadas (usuário, 2026-11-16)

| Item | Decisão |
|------|---------|
| Versões | Compose BOM 2026.09.00, `activity-compose` 1.13.0, `navigation-compose` 2.10.2, Robolectric 4.18, `androidx.test:core` 1.7.0. Plugins `org.jetbrains.kotlin.plugin.compose` e `org.jetbrains.kotlin.plugin.serialization` na versão do Kotlin (2.4.21). Conferidas nos repositórios oficiais |
| SDK | **compileSdk 37**, targetSdk 36, minSdk 26. Motivo: o Compose 1.12 (BOM 2026.09.00), o `navigation-compose` 2.10.2 e o `lifecycle-*-compose` 2.11.0 exigem compileSdk ≥ 37 (o `checkDebugAarMetadata` recusa 12 artefatos com compileSdk 36). O targetSdk segue a exigência da Play Store, que hoje é 36. compileSdk só libera APIs para compilar; não muda o comportamento em execução |
| Plataforma 37 | `platforms;android-37.0`, canal stable, sem codinome (catálogo oficial `repository2-3.xml`). Instalada nesta máquina pelo próprio AGP no build (revision 2); não havia `sdkmanager` |
| Nomes de composables | ktlint: `.editorconfig` na raiz com `[*.{kt,kts}]` e `ktlint_function_naming_ignore_when_annotated_with = Composable`. detekt: `app/detekt.yml` com `naming > FunctionNaming > ignoreAnnotated: ['Composable']`, aplicado só ao `app` com `buildUponDefaultConfig = true` |
| Navegação provisória | fluxo da seção 10: T1→T2; T2→T3, T5, T7, T8; T3→T4; T5→T6→T2; T8→T9→T4. Começa sempre na T1, sem persistência. T1 sai da pilha ao avançar. T6 volta para a T2 existente (sem criar outra). A rota da T4 recebe `packageName: String`, com valor fixo provisório a partir da T3 e da T9 |
| Tema | paleta fixa, segue claro e escuro do sistema, **sem cores dinâmicas** |
| Níveis | verde com ícone de visto, amarelo com alerta, vermelho com erro, cada um com a frase exata da especificação (seção 10): "Nada suspeito encontrado." / "Encontramos apps que merecem atenção." / "Encontramos um app com perfil de golpe. Veja o que fazer." |
| Provisórios | **nome exibido "Tap Guard"** e **ícone do app: escudo vetorial**. Os dois ficam registrados como provisórios (nome em português é decisão em aberto, seção 13) |
| Permissão e componentes do AndroidX (2026-11-17) | **aceitos** e registrados no `SKILLS.md` › Permissions como itens trazidos pelo AndroidX, cada um com o motivo: `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (`androidx.core`), `ProfileInstallReceiver` (`androidx.profileinstaller`), `InitializationProvider` (`androidx.startup`) |
| Teste de permissões (2026-11-17) | novo teste Robolectric lê as permissões do manifesto mesclado e falha se houver qualquer uma fora da lista explícita: `QUERY_ALL_PACKAGES`, `NFC`, `POST_NOTIFICATIONS` (já aprovadas) e `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Substitui a regra "nada fora da lista de permissões", que hoje ninguém verifica. Viabilidade conferida no projeto descartável: o `PackageManager` do Robolectric devolve exatamente as permissões do manifesto mesclado |
| Ícones (2026-11-17) | vetores em `res/drawable`, sem a dependência `material-icons`; origem (Material Symbols, Apache 2.0) registrada no `app/README.md` |
| `packageName` provisório da T4 (2026-11-17) | `io.github.valossa515.tapguard.decoy` |
| Teste de composable isolado (2026-11-17) | divergência aprovada: `Robolectric.buildActivity` + `createEmptyComposeRule()`, sem `ui-test-manifest` |
| `kotlinx-serialization-json` (2026-11-17, review) | **retirada** do `app` e do catálogo: nenhum código a usava. As rotas tipadas precisam só do plugin `org.jetbrains.kotlin.plugin.serialization` (mantido) e do `kotlinx-serialization-core`, que vem pelo `navigation-compose`. Build e os 11 testes de navegação passam sem ela |

### Experimento (projeto descartável `%TEMP%\tap-guard-spec003-probe-20261116-1`)
Activity com Material 3, duas rotas tipadas e testes de clique via Robolectric, sobre as versões acima:
1. Com o `.editorconfig`, `:app:ktlintCheck` passa. Sem a config do detekt, `:app:detekt` falha com `FunctionNaming` no composable. Com ela, passa.
2. Com compileSdk 36, o build falha em `:app:checkDebugAarMetadata` (12 artefatos exigem compileSdk 37). Com compileSdk 37 e targetSdk 36, **`./gradlew build` completo passa**: compilação, lint do Android, ktlint, detekt e testes. Os 2 testes Robolectric (claro e `+night`) e o de fumaça passam. Manifesto mesclado: minSdk 26, targetSdk 36.
3. Teste de composable isolado: `createComposeRule()` falha sem o `ui-test-manifest` (`Unable to resolve activity … ComponentActivity`), e também com ele só em `testImplementation`. A skill manda usar `debugImplementation`, mas isso poria uma `ComponentActivity` de teste no APK de debug publicado pelo CI. **`Robolectric.buildActivity(ComponentActivity::class.java)` + `createEmptyComposeRule()` funciona** sem dependência nova e sem nada no APK.

## Approach
- Seguir `skills/android/SKILL.md` (UI — Compose, Navigation, nomes `{Feature}Route`, `{Feature}Screen`, strings `snake_case` com prefixo da tela) e `skills/android/testing.md` (JUnit 4 + Robolectric no `app`). Versões só no catálogo; plugins declarados na raiz com `apply false` e aplicados no `app`.
- **Divergência com a skill nos testes de composable isolado:** em vez de `createComposeRule()` + `ui-test-manifest` em `debugImplementation`, usar `Robolectric.buildActivity` + `createEmptyComposeRule()` (experimento, item 3). Registrar no Project Overrides do `SKILLS.md`.
- **Como testar sem aparelho** (tudo em `src/test`, Robolectric, JUnit 4, rodando no `./gradlew build` e no CI):
  - **Navegação:** `createAndroidComposeRule<MainActivity>()` abre o app real e o teste clica pelos rótulos das telas. Um teste por aresta do fluxo e checagem da tela de destino pelo título. Pilha verificada pelo comportamento observável, sem `TestNavHostController` (evita a dependência `navigation-testing`):
    - depois de T1→T2, o "voltar" fecha a Activity (a T1 não está mais na pilha);
    - depois de T2→T5→T6→T2, o "voltar" também fecha a Activity (a T6 voltou para a mesma T2, sem empilhar outra);
    - T3→T4 e T9→T4 mostram o `packageName` provisório na T4.
  - **Componentes de nível:** composable isolado em `ComponentActivity` criada pelo Robolectric. Para cada nível: a frase exata aparece, o ícone certo aparece (por `contentDescription` ou `testTag`) e o nível usa a cor própria do tema. Em claro e em escuro (`@Config(qualifiers = "+night")`).
  - **Cores:** a escolha de cor por nível é uma função pura do tema, testada em JUnit 4 sem Android. Um teste confere o contraste mínimo AA (4,5:1) de cada par texto/fundo dos níveis, em claro e escuro (RNF-06).
  - **Permissões:** Robolectric lê `requestedPermissions` do `PackageManager` (que reflete o manifesto mesclado) e falha se houver qualquer permissão fora da lista explícita.
- **Nível na interface:** sem `RiskLevel` do domínio ainda (vem na SPEC-010), o componente recebe um tipo do próprio `app` com os três níveis (`GREEN`, `YELLOW`, `RED`, o vocabulário do `scan_run`), sem lógica de score. A ligação com o domínio fica para a spec da T2 (SPEC-030).
- **Ícones de nível e do app** como vetores em `res/drawable` (Material Symbols, Apache 2.0), sem a dependência `material-icons`; origem registrada no `app/README.md`.
- `android.useAndroidX=true` e os `--add-opens` dos testes, presentes no projeto descartável, **só entram se o build exigir**; a implementação confere sem eles primeiro.
- Todos os textos em `strings.xml` (RNF-07); títulos das telas pelos nomes da seção 10; rótulos de botão da T2 pelos textos da especificação ("Verificar agora", "Testar meu cartão", "Acho que caí em um golpe"); demais rótulos provisórios, marcados no arquivo de recursos.

## Tasks
1. [x] **compileSdk 37** (`chore:`): `compileSdk = "37"` no catálogo; targetSdk 36 e minSdk 26 inalterados. `./gradlew build` verde nos seis módulos.
2. [x] **Regras de nome para composables** (`chore:`): `.editorconfig` na raiz e `app/detekt.yml` aplicado ao `app` com `buildUponDefaultConfig = true`.
3. [x] **Activity, tema e identidade provisória** (`feat:`): plugins Compose e serialização, dependências do catálogo, Activity de entrada no manifesto (única exportada pelo projeto), tema Material 3 com paleta fixa claro/escuro sem cores dinâmicas, tema da janela em `res/values/themes.xml` e `res/values-night/themes.xml` (a janela não pisca em branco no modo escuro), nome "Tap Guard" e ícone de escudo vetorial (provisórios).
4. [x] **Componentes de nível** (`feat:`): verde, amarelo e vermelho, cada um com cor do tema, ícone vetorial (visto, alerta, erro) e a frase exata da seção 10.
5. [x] **Grafo de navegação e telas vazias** (`feat:`): rotas tipadas `@Serializable` para T1 a T9 (T4 com `packageName: String`), `NavHost` com o fluxo provisório, T1 removida da pilha ao avançar, T6 voltando para a T2 existente.
6. [x] **Testes** (`test:`, papel tester): navegação (cada aresta, pilha, argumento da T4), componentes de nível em claro e escuro, tema claro/escuro, função de cor e contraste AA, e o teste de permissões do manifesto mesclado com a lista explícita.
7. [x] **Documentação** (`docs:`): `app/README.md`; `CONTRIBUTING.md` e `runbook.md` (instalar e abrir o app com `./gradlew :app:installDebug`, plataforma 37); `project-reference.md` (§1 SDKs, §3 rotas e componentes, §4 telas, §8 versões, §9 comandos); `conventions.md` e `SKILLS.md` (override de SDK com compileSdk 37 e targetSdk 36, regras de nome de composable, divergência de teste de composable aprovada, permissão e componentes do AndroidX em Permissions com o motivo de cada um, e o teste que verifica a lista); origem dos ícones (Material Symbols, Apache 2.0) no `app/README.md`; **ADR-013** em `decisions.md` registrando a separação entre compileSdk e targetSdk, sem reescrever o ADR-011.
8. [x] **Conferência dos critérios:** localmente, desfazendo sem commit: uma função comum (não `@Composable`) com nome em maiúscula ainda falha no ktlint e no detekt; remover o ícone ou trocar a frase de um nível faz o teste falhar; trocar o destino de uma aresta faz o teste de navegação falhar; uma permissão fora da lista (por exemplo `android.permission.CAMERA`) no manifesto faz o teste de permissões falhar. **No primeiro CI da PR:** o log do job "Build e testes" mostra o runner baixando a `Android SDK Platform 37.0` e o build passando.
   - 2026-11-17, local, cada alteração desfeita sem commit (árvore limpa depois de cada uma):
     - função comum `fun BadNamingProbe()` no `app` → `:app:ktlintMainSourceSetCheck` falha ("Function name should start with a lowercase letter") e `:app:detekt` falha (`FunctionNaming`). A exceção vale só para `@Composable`.
     - frase do verde trocada no `strings.xml` → `RiskLevelIndicatorTest` falha (2 de 6: verde claro e escuro).
     - `Icon` removido do `RiskLevelIndicator` → `RiskLevelIndicatorTest` falha (6 de 6).
     - "Verificar agora" apontando para `SettingsRoute` → `TapGuardNavHostTest` falha (2 de 11: `home goes to scan result` e `scan result goes to app detail…`).
     - `android.permission.CAMERA` no manifesto → `./gradlew build` falha no `MergedManifestTest` ("Permissões fora da lista: [android.permission.CAMERA]").
     - Depois de desfazer tudo, `./gradlew build` volta a passar (26 testes, 0 falhas).
   - Pendente: confirmar no primeiro CI da PR que o runner baixa a `Android SDK Platform 37.0`.

## Database changes
- Nenhuma.

## Risks & open questions
- **Permissão e componentes que o AndroidX traz (resolvido: aceitos).** No manifesto mesclado do projeto descartável apareceram, por dependência indireta:
  - permissão `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (declarada e usada), de nível signature, do `androidx.core` 1.18.0;
  - `androidx.profileinstaller.ProfileInstallReceiver` exportado, protegido por `android.permission.DUMP` (só o shell e o sistema têm), do `profileinstaller` 1.4.0;
  - `androidx.startup.InitializationProvider`, não exportado.

  O job "Regras do projeto" só procura `INTERNET`. Os três ficam registrados no `SKILLS.md` com o motivo: remover a permissão quebraria o `ContextCompat.registerReceiver` com `RECEIVER_NOT_EXPORTED` abaixo do Android 13; o receptor só atende o shell e serve aos perfis de inicialização (RNF-03, abertura em até 2 s); o provider não é exportado. O teste de permissões passa a barrar qualquer permissão nova fora da lista.
- **Uma dependência nova que traga permissão** faz o teste de permissões falhar. É o comportamento desejado: a permissão só entra com decisão registrada e a lista do teste atualizada na mesma spec.
- **Tipo de nível no `app`** até a SPEC-010 criar o `RiskLevel` do domínio. Não é lógica de score, só apresentação; a SPEC-030 liga os dois.
- **CI e plataforma 37:** o runner precisa baixar a `android-37.0` (o AGP baixa se as licenças estiverem aceitas, como baixou a 36 na SPEC-001). Confirmar no primeiro CI da PR.
- **Robolectric baixa o `android-all-instrumented`** do Maven Central no primeiro teste. Precisa de rede no CI (já tem).
- **compileSdk 37 vale para os seis módulos** (catálogo único). Sem efeito em execução; o targetSdk 36 continua.
- **Estados das telas:** as telas desta spec não têm dados, então não têm os quatro estados obrigatórios. Eles chegam com o conteúdo de cada tela.

## Acceptance criteria
- [ ] O app abre na T1, e a navegação entre todas as telas segue o fluxo provisório (AC-001 a AC-009)
- [ ] A T1 sai da pilha, a T6 volta para a T2 existente e a T4 recebe `packageName` (AC-010 a AC-012)
- [ ] Componentes de nível com cor, ícone e frase exata, em claro e escuro (AC-013 a AC-015)
- [ ] Tema com paleta fixa claro/escuro, sem cores dinâmicas, com contraste AA nos níveis (AC-016, AC-017)
- [ ] Nome "Tap Guard" e ícone de escudo, provisórios (AC-018)
- [ ] compileSdk 37, targetSdk 36, minSdk 26; versões só no catálogo (AC-019, AC-020)
- [ ] Composables aceitos pelo ktlint e pelo detekt, funções comuns ainda verificadas (AC-021, AC-022)
- [ ] Sem `INTERNET`; permissão e componentes do AndroidX registrados no `SKILLS.md` (AC-023)
- [ ] Teste de permissões do manifesto mesclado com a lista explícita, passando hoje e falhando com permissão fora da lista (AC-028, AC-029)
- [ ] Textos em `strings.xml` (AC-024)
- [ ] ktlint, detekt e testes passam, sem aparelho (AC-025)
- [ ] Documentação atualizada, com o ADR-013 (AC-026)
- [ ] CI da PR verde, com a plataforma 37 baixada pelo runner (AC-027)
