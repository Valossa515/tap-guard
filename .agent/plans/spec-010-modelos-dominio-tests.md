# Test Plan: SPEC-010 Modelos e contratos do domínio

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-010-modelos-dominio.contract.toon` (versão 1.2) e critérios de `.agent/specs/acceptance/spec-010-modelos-dominio.acceptance.toon` · 2026-11-18
> Sem desvio da estratégia aprovada: 10 alvos, 32 cenários (3 + 2 + 5 + 2 + 5 + 3 + 1 + 5 + 3 + 3), um método de teste por cenário.

## Scope
Modelos, enums e conversão da máscara do `core-domain`: construção, invariantes (`require`), cópia defensiva, coleções não modificáveis por cast, bits dos sinais, ordem dos níveis, `toString()` mascarado e os três resultados de leitura do cartão. Tudo em JUnit 5 na JVM, em `core-domain/src/test`, no `./gradlew build`. As interfaces e o `NfcTagHandle` ficam de fora (sem código executável, conferidos no review).

## Padrões seguidos (pattern analysis)
- Existentes: `CoreDomainSmokeTest` (JUnit 5, `org.junit.jupiter.api.Test` e `Assertions`), `RiskLevelColorsTest` e `SettingsDataSourceTest` (uma classe por unidade, nomes em frase com crases, constantes em `private companion object`, sem classe-base).
- Domínio conforme `skills/android/testing.md` › Unit Tests — Domain: objetos reais, sem mocks e sem Robolectric. Fixtures como funções de topo com argumentos padrão (› Test Fixtures), num único arquivo previsto no contrato.
- Asserções do JUnit 5 (`assertEquals`, `assertThrows`, `assertTrue`, `assertFalse`): sem biblioteca nova.

## Unit tests (JUnit 5, JVM)

### `InstalledApp` — `inventory/InstalledAppTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | fields-are-read-back-and-installer-accepts-null | os oito campos voltam como informados; `installerPackageName` nulo aceito | AC-013 |
| 2 | mutating-source-permissions-and-services-does-not-change-the-model | alterar os `MutableSet` de origem depois da construção não muda o modelo | AC-004 |
| 3 | exposed-permissions-and-services-reject-add-remove-and-clear-through-mutable-set-cast | `add`, `remove` e `clear` após cast para `MutableSet` lançam `UnsupportedOperationException`; o conteúdo não muda | AC-005 |

### `Signal` — `signal/SignalTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | nine-entries-with-bits-0-to-8-in-s1-to-s9-order | as nove entradas, na ordem S1 a S9, com bits 0 a 8 fixados um a um | AC-007 |
| 2 | bits-are-distinct | nove bits diferentes | AC-007 |

### `SignalBitMask` — `signal/SignalBitMaskTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | all-512-combinations-round-trip | para cada máscara de 0 a 511, o conjunto decodificado volta à mesma máscara | AC-008 |
| 2 | empty-set-is-0-and-all-signals-is-511 | vazio ↔ 0; todos ↔ 511 | AC-008 |
| 3 | single-signal-mask-is-1-shifted-by-its-bit | cada sinal sozinho vira `1 shl bit` e volta a ele | AC-008 |
| 4 | negative-and-above-bit-8-masks-are-rejected | -1, 512, `Int.MIN_VALUE` e `Int.MAX_VALUE` lançam `IllegalArgumentException` | AC-009 |
| 5 | decoded-set-rejects-add-through-mutable-set-cast | `add` após cast lança `UnsupportedOperationException` | AC-010 |

### `RiskLevel` — `risk/RiskLevelTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | severity-order-is-green-yellow-red | `GREEN < YELLOW < RED` | AC-011 |
| 2 | names-match-persisted-values | nomes `GREEN`, `YELLOW`, `RED` | AC-011 |

### `RiskResult` — `risk/RiskResultTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | score-0-and-100-are-accepted | os dois limites constroem | AC-012 |
| 2 | score-minus-1-and-101-are-rejected | `IllegalArgumentException` nos dois | AC-012 |
| 3 | level-is-not-checked-against-score | score 65 com `GREEN` aceito | AC-012 |
| 4 | mutating-source-signals-does-not-change-the-model | alterar a origem não muda `signals` | AC-004 |
| 5 | exposed-signals-reject-add-remove-and-clear-through-mutable-set-cast | `UnsupportedOperationException` nas três operações | AC-005 |

### `ScoringContext` — `risk/ScoringContextTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | holds-the-trusted-packages | `trustedPackages` igual ao informado | AC-014 |
| 2 | mutating-source-trusted-packages-does-not-change-the-model | alterar a origem não muda o modelo | AC-004 |
| 3 | exposed-trusted-packages-reject-add-remove-and-clear-through-mutable-set-cast | `UnsupportedOperationException` nas três operações | AC-005 |

### `ScanTrigger` — `scan/ScanTriggerTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | entries-are-manual-app-open-and-periodic | entradas `MANUAL`, `APP_OPEN`, `PERIODIC` | AC-015 |

### `ScanReport` e `ScannedApp` — `scan/ScanReportTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | fields-and-apps-in-the-given-order-are-read-back | `trigger`, `startedAt`, `finishedAt` e `apps` na ordem informada | AC-015 |
| 2 | finished-before-started-is-rejected-and-equal-is-accepted | 1 ms antes lança `IllegalArgumentException`; igual constrói | AC-026 |
| 3 | scanned-app-holds-app-and-risk | `app` e `risk` voltam como informados | AC-015 |
| 4 | mutating-source-apps-does-not-change-the-report | alterar a `MutableList` de origem não muda `apps` | AC-004 |
| 5 | exposed-apps-reject-add-remove-and-clear-through-mutable-list-cast | `UnsupportedOperationException` nas três operações | AC-005 |

### `LastFourDigits` — `card/LastFourDigitsTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | four-ascii-digits-are-accepted | `1234` aceito e `value` igual | AC-018 |
| 2 | three-five-sixteen-digits-letters-spaces-and-non-ascii-digits-are-rejected | `123`, `12345`, `4111111111111111`, `12a4`, ` 123` e `١٢٣٤` lançam `IllegalArgumentException`, e a mensagem não contém o valor recebido | AC-018 |
| 3 | to-string-hides-the-digits | `toString()` não contém `1234` | AC-020 |

### `CardReadResult` — `card/CardReadResultTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | exhaustive-when-covers-success-unsupported-card-and-read-interrupted | `when` sem `else` com os três subtipos compila e distingue cada um | AC-017 |
| 2 | success-holds-label-last-four-digits-and-expiry | os três campos voltam como informados | AC-019 |
| 3 | success-to-string-hides-digits-expiry-and-label | `toString()` não contém `1234`, `2030` nem o rótulo | AC-020 |

## Integration tests
Nenhum: módulo Kotlin puro, sem adaptador nem I/O nesta spec.

## Edge cases
- Limites do score (0, 100, -1, 101) e do tempo da varredura (igual e 1 ms antes).
- Máscaras nos extremos de `Int` e o primeiro bit inválido (512).
- Dígitos de outro alfabeto (`isDigit()` os aceitaria) e o número de teste sintético completo.

## Test data & fixtures
- `core-domain/src/test/kotlin/io/github/valossa515/tapguard/core/domain/DomainFixtures.kt`: `anInstalledApp(...)` e `aRiskResult(...)` com argumentos padrão; pacotes `com.example...` e datas fixas. Usado pelo `ScanReportTest` e pelo `RiskResultTest`.
- Dados de cartão só sintéticos: `1234`, validade `2030-12`, rótulo `CARTAO TESTE`, número de teste `4111111111111111`.

## Infrastructure
Nenhuma além do JUnit 5 já declarado no `core-domain`. O `CoreDomainSmokeTest` é removido (contrato).

## Execução
- [x] `./gradlew build` verde (2026-11-18, Windows): 32 testes no `core-domain`, 0 falhas, 0 ignorados.
- [x] Kover do `core-domain`: **98,8% de linhas** (82 de 83). A única linha não coberta é a declaração da value class `LastFourDigits` (`LastFourDigits.kt:10`), código gerado pelo compilador para value classes, sem lógica; a estratégia não prevê teste para ela.
- Ajuste durante a execução: a fixture `anInstalledApp` expõe só os parâmetros que os testes sobrescrevem (`packageName`, `requestedPermissions`, `services`), com os demais campos fixos. Com os oito parâmetros, o detekt (`LongParameterList`) a recusava. A exceção do `@Suppress` (emenda 1.2) vale só para fábricas que espelham exatamente o construtor, e a fixture não precisa espelhar.
