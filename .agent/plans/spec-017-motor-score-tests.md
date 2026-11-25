# Test Plan: SPEC-017 Motor de score

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-017-motor-score.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-017-motor-score.acceptance.toon` (versão 1.0) · 2026-11-25
> Três alvos no `core-domain`, JUnit 5: os cinco exemplos da §5 (5 testes), a tabela do plano (37 linhas em três `@MethodSource`) e as faixas (um teste com os 6 limites). Execuções novas: 43; o `core-domain` passa de 32 para 75. Cobertura de linhas do módulo: 97,5% (117 de 120; as duas linhas novas sem cobertura são as declarações `val` do `companion object` privado do `WeightedRiskScorer`, cujo getter o código não chama).

## Scope
- **Exemplos da §5:** um teste com nome próprio por exemplo (AC-001 a AC-005).
- **Tabela do plano:** cada peso sozinho (P0–P9, AC-006), regras 3 e 4 com as exceções S7 e S9 e os limites 59/60 (C01–C19, AC-007 a AC-009), regra 2 com as regras 3 e 4 e a comparação exata do pacote (T1–T8, AC-010 e AC-011). Toda linha confere `signals` repassados (AC-012).
- **Faixas:** `RiskScoreConfig.levelFor` em 0, 29, 30, 59, 60 e 100 (os limites 29 e 59 também aparecem nos tetos das regras).
- **Fora (contrato, `excluded`):** regra 1 (SPEC-011); montar o `ScoringContext` e chamar o motor (SPEC-018). Domínio puro e cobertura: build e inspeção (AC-013).

## Padrões seguidos (pattern analysis)
- **`RiskResultTest`, `ScoringContextTest`, `SignalBitMaskTest`:** JUnit 5, `Assertions.assertEquals`, nomes entre crases em inglês, sem mocks.
- **`anInstalledApp`** do `testFixtures` para o app (`packageName = "com.example.app"`).
- **Tabela escrita à mão, nunca a lista de produção** (testes parametrizados do `core-detection`: `InstallSourceCollectorTrustedStoreTest`, `BankNameMatcher`). Aqui com `@ParameterizedTest(name = "{0}")` e `@MethodSource` em `companion object` com `@JvmStatic` (primeiro parametrizado do `core-domain`; `junit-jupiter` já traz `junit-jupiter-params`).
- Sinais escritos como na §5 (`"S1 S2 S3"`), traduzidos por um mapa escrito à mão no teste: o teste não lê o `RiskScoreConfig`, então mudar um peso derruba casos.

## Unit tests

### Exemplos — `core-domain/src/test/.../risk/WeightedRiskScorerExamplesTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | example 1 APK outside store with NFC and HCE is 65 red | S1 S2 S3 → 65, RED | AC-001 |
| 2 | example 2 same app installed yesterday with accessibility is 100 red | S1 S2 S3 S4 S6 → 100, RED | AC-002 |
| 3 | example 3 bank app from store with NFC and HCE is capped at 29 green | S2 S3 → 29, GREEN | AC-003 |
| 4 | example 4 game installed by APK without NFC is 30 yellow | S1 → 30, YELLOW | AC-004 |
| 5 | example 5 app outside store with accessibility and SMS is capped at 59 yellow | S1 S4 S8 → 59, YELLOW | AC-005 |

### Tabela — `core-domain/src/test/.../risk/WeightedRiskScorerTableTest.kt`
Linhas exatamente como no plano (`.agent/plans/spec-017-motor-score.md`, "Tabela de combinações"), com o id da linha no nome do caso.

| # | Scenario | Linhas | AC |
|---|----------|--------|----|
| 1 | single signal weights | P0–P9 | AC-006 |
| 2 | adjustment rules 3 and 4 | C01–C19 | AC-007, AC-008, AC-009 |
| 3 | trusted app rule 2 | T1–T8 | AC-010, AC-011 |

Cada caso confere `score`, `level` e `signals == sinais recebidos` (AC-012).

### Faixas — `core-domain/src/test/.../risk/RiskScoreConfigTest.kt`
| # | Scenario | Expected result |
|---|----------|-----------------|
| 1 | band limits | 0 e 29 → GREEN; 30 e 59 → YELLOW; 60 e 100 → RED |

## Edge cases
- Soma acima de 100 com e sem teto de regra (C09, C17, C19, T5).
- Soma exata de 60 com e sem sinal de NFC (C12, C13, C15).
- Pacote confiável com caixa diferente (T8) e outro pacote no conjunto (T7).

## Test data & fixtures
- `anInstalledApp(packageName = "com.example.app")`; `ScoringContext(emptySet())` ou com o pacote da linha.

## Infrastructure
- Nenhuma. JVM pura, sem Robolectric.

## Conferência local (desfeita sem commit)
As mutações do contrato (`verification`): tirar a exceção de S7 ou de S9 na regra 3, tirar o teto de 100, mexer na regra 2 (parar antes das regras 3 e 4, ou limitar a 29), tirar S7 dos sinais de NFC, trocar o peso de S4. Cada uma precisa derrubar as linhas listadas no contrato.
