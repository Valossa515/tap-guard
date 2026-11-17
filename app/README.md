# app

Aplicativo Tap Guard: telas, navegação, vigia agendada, notificações e injeção de dependências.

> **Estado (SPEC-003):** Activity com Compose, tema Material 3 claro e escuro, componente de nível e as nove telas da especificação (T1 a T9) **vazias**, ligadas pela navegação provisória. Sem ViewModel, sem Hilt, sem dados e sem nenhuma permissão própria.

## Dependências

- **Módulos:** `core-domain`, `core-detection`, `core-nfc` e `core-data`.
- **Plugins:** `com.android.application` (Kotlin embutido do AGP 9), `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`, ktlint e detekt (com `detekt.yml` deste módulo).
- **Bibliotecas:** Compose BOM 2026.09.00 (Material 3), `activity-compose` 1.13.0, `navigation-compose` 2.10.2 (traz o `kotlinx-serialization-core` usado pelas rotas tipadas).
- **Testes:** JUnit 4, Robolectric 4.18, `androidx.test:core` 1.7.0 e `ui-test-junit4` (do BOM). Tudo em `src/test`, sem aparelho.
- **applicationId e namespace:** `io.github.valossa515.tapguard` · minSdk 26 · targetSdk 36 · **compileSdk 37** (ADR-013).

## Estrutura

Pacote base `io.github.valossa515.tapguard`:

| Pacote | Conteúdo |
|--------|----------|
| raiz | `MainActivity`: única Activity exportada; só chama `TapGuardTheme { TapGuardNavHost() }` |
| `navigation` | `Routes.kt` (rotas `@Serializable`, uma por tela) e `TapGuardNavHost.kt` (o grafo; único lugar que conhece o `NavController`) |
| `ui/theme` | `Color.kt` (paleta fixa, inclusive as cores dos níveis) e `Theme.kt` (`TapGuardTheme`: segue claro/escuro do sistema, sem cores dinâmicas; fornece o modo em uso por `LocalTapGuardDarkTheme`) |
| `ui/components` | `RiskLevelUi` (`GREEN`, `YELLOW`, `RED`), `riskLevelColors` (função pura) e `RiskLevelIndicator(level, modifier)` (cor + ícone + frase, no modo do `TapGuardTheme` em volta) |
| `ui/<tela>` | um `{Feature}Screen` por tela: `onboarding`, `home`, `scanresult`, `appdetail`, `cardtestintro`, `cardtestreading`, `emergency`, `settings`, `alerthistory` |

### Navegação provisória (especificação, seção 10)

| Tela | Rota | Sai para |
|------|------|----------|
| T1 Boas-vindas | `OnboardingRoute` | T2 (a T1 sai da pilha) |
| T2 Início | `HomeRoute` | T3, T5, T7, T8 |
| T3 Resultado | `ScanResultRoute` | T4 |
| T4 Detalhe do app | `AppDetailRoute(packageName)` | — |
| T5 Teste do cartão: explicação | `CardTestIntroRoute` | T6 |
| T6 Teste do cartão: leitura e resultado | `CardTestReadingRoute` | T2 existente (sem empilhar outra) |
| T7 Emergência | `EmergencyRoute` | — |
| T8 Configurações | `SettingsRoute` | T9 |
| T9 Histórico de alertas | `AlertHistoryRoute` | T4 |

O app começa sempre na T1, sem persistência. A T3 e a T9 abrem a T4 com o pacote provisório `io.github.valossa515.tapguard.decoy`.

### Níveis

| Nível | Ícone | Frase |
|-------|-------|-------|
| `GREEN` | visto | "Nada suspeito encontrado." |
| `YELLOW` | alerta | "Encontramos apps que merecem atenção." |
| `RED` | erro | "Encontramos um app com perfil de golpe. Veja o que fazer." |

Cada par fundo/texto tem contraste AA (4,5:1) ou melhor, em claro e escuro.

## Provisórios

- **Nome exibido "Tap Guard"** e **ícone de escudo**: o nome em português é decisão em aberto (especificação, seção 13).
- `RiskLevelUi`: tipo do app até o `RiskLevel` do domínio (SPEC-010).
- Rótulos de botão que não estão na especificação, marcados como provisórios no `strings.xml`.

## Ícones

Vetores em `src/main/res/drawable`, sem a dependência `material-icons`: `ic_launcher_shield` (`shield`), `ic_level_green` (`check_circle`), `ic_level_yellow` (`warning`) e `ic_level_red` (`error`).

**Origem:** [Material Symbols](https://github.com/google/material-design-icons) (estilo Rounded, preenchido), do Google, sob a licença **Apache 2.0**. Arquivos Android do repositório `google/material-design-icons`, commit `10ecc14`. O `android:tint` de tema foi retirado: a cor vem do componente, e no ícone do app vem da cor primária fixa.

## Regras

- O manifesto nunca declara `android.permission.INTERNET`. O CI falha se ela aparecer.
- Só entram as permissões listadas em `.agent/SKILLS.md` › Permissions. O `MergedManifestTest` lê o manifesto mesclado e falha se houver qualquer permissão fora da lista explícita, ou outra Activity exportada além da `MainActivity`.
- Todo texto visível fica em `strings.xml` (RNF-07). O nível nunca é comunicado só por cor (RNF-06).
- Composables podem ter nome em PascalCase: `.editorconfig` (ktlint) e `detekt.yml` (detekt) ignoram a regra de nome de função só para `@Composable`.

## Comandos

```bash
./gradlew :app:build
./gradlew :app:testDebugUnitTest   # navegação, níveis, tema, cores e manifesto (Robolectric)
./gradlew :app:assembleDebug
./gradlew :app:installDebug        # instala no aparelho ou emulador conectado
```
