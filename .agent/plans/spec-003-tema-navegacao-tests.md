# Test Plan: SPEC-003 Tema e navegação

> Fonte: `test-strategy` do contrato `.agent/specs/contracts/spec-003-tema-navegacao.contract.toon` e critérios de `.agent/specs/acceptance/spec-003-tema-navegacao.acceptance.toon` · 2026-11-17
> Sem desvio da estratégia aprovada: 5 classes, 26 cenários (25 + 1 da emenda 1.1 do contrato).

## Scope
Navegação entre T1 e T9 pela Activity real, componente de nível isolado, tema claro e escuro, cores dos níveis (função pura) e manifesto mesclado (permissões e Activity exportada). Tudo em `app/src/test`, rodando no `./gradlew build` sem aparelho.

## Padrões seguidos (pattern analysis)
- Existentes: `AppSmokeTest`, `CoreDataSmokeTest`, `DecoyAppSmokeTest`. JUnit 4 nos módulos Android, `org.junit.Assert`, nomes de teste em frase com crases, sem base classes nem fixtures compartilhadas.
- Robolectric e Compose UI Test conforme `skills/android/testing.md`, com a divergência aprovada para composable isolado (`Robolectric.buildActivity` + `createEmptyComposeRule()`).
- `@Config(sdk = [36])` em cada classe Robolectric (targetSdk).
- Frases e descrições dos níveis escritas **literalmente** nos testes (texto da especificação). Assim, trocar o texto no `strings.xml` faz o teste falhar. Rótulos de navegação vêm de `getString(R.string.…)`.

## Unit tests

### `RiskLevelColors` — `ui/components/RiskLevelColorsTest.kt` (JUnit 4, sem Robolectric)
| # | Scenario | Função | Expected result |
|---|----------|--------|-----------------|
| 1 | Níveis distintos no modo claro | `riskLevelColors(level, darkTheme = false)` | Os 3 `container` são diferentes entre si, e os 3 `content` também |
| 2 | Níveis distintos no modo escuro | `riskLevelColors(level, darkTheme = true)` | Idem |
| 3 | Contraste AA no modo claro | idem | `content` sobre `container` ≥ 4,5:1 para cada nível |
| 4 | Contraste AA no modo escuro | idem | Idem |

Contraste calculado no teste com `Color.luminance()`: `(L1 + 0,05) / (L2 + 0,05)`.

## Integration tests (Robolectric)

### Navegação — `navigation/TapGuardNavHostTest.kt` (`createAndroidComposeRule<MainActivity>()`)
| # | Scenario | Ação | Expected result | AC |
|---|----------|------|-----------------|----|
| 1 | app-starts-on-onboarding | abrir | título "Boas-vindas" exibido | AC-001 |
| 2 | onboarding-goes-to-home | "Começar" | título "Início" | AC-002 |
| 3 | home-goes-to-scan-result | "Verificar agora" | título "Resultado" | AC-003 |
| 4 | home-goes-to-card-test-intro | "Testar meu cartão" | título da T5 | AC-004 |
| 5 | home-goes-to-emergency | "Acho que caí em um golpe" | título "Emergência" | AC-005 |
| 6 | home-goes-to-settings | "Configurações" | título "Configurações" (o botão some, o título fica) | AC-006 |
| 7 | scan-result-goes-to-app-detail-with-provisional-package-name | "App de teste" | título da T4 e "Pacote: io.github.valossa515.tapguard.decoy" | AC-007, AC-012 |
| 8 | card-test-intro-goes-to-card-test-reading | "Continuar" | título da T6 | AC-008 |
| 9 | settings-goes-to-alert-history-then-app-detail-with-provisional-package-name | "Histórico de alertas" e depois "Alerta de teste" | título da T9, depois título da T4 com o pacote provisório | AC-009, AC-012 |
| 10 | back-on-home-after-onboarding-closes-the-app | T1 → T2, depois voltar | `activity.isFinishing` = true | AC-010 |
| 11 | card-test-reading-returns-to-existing-home-and-back-closes-the-app | T2 → T5 → T6 → "Voltar ao início", depois voltar | T2 exibida, depois `isFinishing` = true | AC-011 |

O "voltar" é acionado por `onBackPressedDispatcher.onBackPressed()` na Activity. Quando o `NavHost` não tem mais para onde voltar, a `ComponentActivity` encerra.

### Componente de nível — `ui/components/RiskLevelIndicatorTest.kt` (`Robolectric.buildActivity(ComponentActivity)` + `createEmptyComposeRule()`)
| # | Scenario | Modo | Expected result | AC |
|---|----------|------|-----------------|----|
| 1 | green light | claro | "Nada suspeito encontrado." e ícone "Nível verde" | AC-013 |
| 2 | yellow light | claro | "Encontramos apps que merecem atenção." e ícone "Nível amarelo" | AC-014 |
| 3 | red light | claro | "Encontramos um app com perfil de golpe. Veja o que fazer." e ícone "Nível vermelho" | AC-015 |
| 4 a 6 | os três | escuro (`@Config(qualifiers = "+night")`) | idem | AC-013 a AC-015 |
| 7 | forced-dark-theme-on-light-system-uses-dark-level-colors (emenda 1.1) | sistema claro, `TapGuardTheme(darkTheme = true)` | cor do texto do verde = `riskLevelColors(GREEN, darkTheme = true).content`, lida por `SemanticsActions.GetTextLayoutResult` | AC-013, BR-009 |

Todos os cenários envolvem o componente no `TapGuardTheme`. A cor de cada nível fica coberta pelo `RiskLevelColorsTest` e pela revisão de que o `RiskLevelIndicator` usa `riskLevelColors` (AC-013 a AC-015). O cenário 7 confere que o componente segue o modo do tema, não o do sistema.

### Tema — `ui/theme/TapGuardThemeTest.kt` (isolado)
| # | Scenario | Modo | Expected result | AC |
|---|----------|------|-----------------|----|
| 1 | light-color-scheme-in-light-mode | claro | `MaterialTheme.colorScheme.background` = `LightBackground` e `primary` = `LightPrimary` | AC-016 |
| 2 | dark-color-scheme-in-night-mode | `+night` | `background` = `DarkBackground` e `primary` = `DarkPrimary` | AC-016 |

### Manifesto mesclado — `MergedManifestTest.kt` (Robolectric, `PackageManager`)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | requested-permissions-are-within-the-explicit-allowlist | toda permissão de `requestedPermissions` está em: `android.permission.QUERY_ALL_PACKAGES`, `android.permission.NFC`, `android.permission.POST_NOTIFICATIONS`, `<packageName>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AC-028, AC-029 |
| 2 | main-activity-is-the-only-exported-activity | Activities exportadas = só `MainActivity` | AC-023 |

## Edge cases
- Nenhum além da estratégia (regra "strategy is law").

## Test data & fixtures
- Constantes locais em cada classe (frases dos níveis, pacote provisório, lista de permissões). Sem fixtures compartilhadas.

## Infrastructure
- Robolectric 4.18, `androidx.test:core` 1.7.0 e `ui-test-junit4` (BOM), já no `app/build.gradle.kts`. Nenhuma dependência nova.

## Execução
- [x] `./gradlew build` verde, com os 25 cenários + `AppSmokeTest` (26 testes, 0 falhas), em 2026-11-17.
- [x] Emenda 1.1: com o cenário 7, 27 testes, 0 falhas. Com o componente fixo no modo claro, só o cenário 7 falha.
- Regras de teste do Compose em `androidx.compose.ui.test.junit4.v2` (`createAndroidComposeRule`, `createEmptyComposeRule`): as versões sem `v2` estão depreciadas no Compose 1.12. Mesmo artefato (`ui-test-junit4`), sem dependência nova.
