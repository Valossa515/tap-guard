# Architecture: SPEC-017 Motor de score

> Entrada: `.agent/plans/spec-017-motor-score.md`, `.agent/specs/features/spec-017-motor-score.spec.toon` (FT-017 v1.0), `.agent/specs/acceptance/spec-017-motor-score.acceptance.toon` (v1.0)
> Contrato: `.agent/specs/contracts/spec-017-motor-score.contract.toon` (v1.0) · 2026-11-25

## Summary
Dois arquivos de produção no pacote `core.domain.risk`, Kotlin puro:

| Arquivo | Conteúdo | Visibilidade |
|---|---|---|
| `RiskScoreConfig.kt` | `internal object RiskScoreConfig`: os nove pesos como `const val`, o teto da soma (100), os tetos das regras 3 (29) e 4 (59), o início das faixas amarela (30) e vermelha (60), `weightOf(signal)` com `when` exaustivo e `levelFor(score)` | `internal` |
| `WeightedRiskScorer.kt` | `class WeightedRiskScorer : RiskScorer`, sem parâmetros e sem estado: soma, regras 2 a 4, nível | público |

`RiskScorer`, `ScoringContext`, `RiskResult` e `Signal` não mudam. Nenhuma dependência nova, nenhum `@Suppress`.

## Pattern Analysis
- **Padrões existentes:**
  - **Lista ou configuração num `object` de arquivo próprio:** `TrustedStores`, `KnownWallets`, `KnownBanks` (`core-detection`). O arquivo de pesos segue a forma: um `object`, um comentário por entrada apontando a fonte (§5).
  - **Implementação nomeada pelo mecanismo:** `PackageManagerAppInventory : AppInventory`. Daí `WeightedRiskScorer : RiskScorer` (soma de pesos, ADR-003).
  - **Limites como `const val` nomeados** (detekt `MagicNumber`): `MIN_SCORE`/`MAX_SCORE` no `RiskResult`, limites 5 e 9 do `BankNameMatcher`.
  - **`internal` só para teste e uso no módulo:** `BANK_NAME_PREFIXES`/`BANK_NAME_SUFFIXES` (`internal` para um teste), `unmodifiableCopy()`.
  - **Tabela escrita à mão no teste, nunca a lista de produção:** testes parametrizados do `core-detection` (`InstallSourceCollectorTrustedStoreTest`, casos do `BankNameMatcher`).
- **Padrão seguido:** `object` de configuração `internal` + implementação pública sem parâmetros; testes com tabela escrita à mão.

## Revisão do plano (architect)

| Ponto | Plano | Decisão do architect | Muda comportamento? |
|---|---|---|---|
| Nome do arquivo de configuração | "arquivo de configuração dos pesos e limites" | `RiskScoreConfig.kt` com `internal object RiskScoreConfig`. "Config" porque guarda pesos **e** limites, e é o "único arquivo de configuração" da §5 | Não |
| Nome da implementação | "implementação do `RiskScorer`" | `WeightedRiskScorer` | Não |
| Onde ficam os pesos | No arquivo de configuração, não no `Signal` | Nove `const val` (`UNTRUSTED_INSTALL_SOURCE_WEIGHT = 30`, …) e `weightOf(signal)` com `when` exaustivo: um sinal novo sem peso não compila. `Map` foi descartado: aceitaria um sinal faltando e só falharia na execução | Não |
| Faixas | No arquivo de configuração | `YELLOW_FROM = 30`, `RED_FROM = 60` e `levelFor(score)` no mesmo `object`. Testada direto com 0, 29, 30, 59, 60 e 100 (gap 8 do plano) | Não |
| Regras (quais sinais são de NFC, exceções da regra 3) | — | Ficam no `WeightedRiskScorer`, como conjuntos privados: são regras da §5, não pesos para calibrar. O arquivo de configuração guarda só números | Não |
| O que fica público | Implementação pública; pesos fixos | `WeightedRiskScorer` público (o `ScanDevice` do `core-detection` e o SDK o constroem); `RiskScoreConfig` `internal` (nada fora do módulo precisa dos números; o nível vem no `RiskResult`). Pesos não injetáveis: a §5 calibra com testes no repositório | Não |
| Teto 100 duplicado | — | `RiskScoreConfig.MAX_SCORE` é a regra (soma limitada a 100); o `MAX_SCORE` privado do `RiskResult` continua sendo a invariante do modelo. Não se fundem: um é configuração, o outro validação | Não |
| Estimativa | 380 (90 código, 230 testes, 60 margem) | Pelos arquivos do contrato: 110 de código e 215 de testes = **325**. A spec mantém 380 (margem de 55, abaixo de 500): um PR | Não |

**Conclusão:** nenhuma decisão de comportamento muda. O fluxo segue direto para a implementação, como o usuário autorizou.

## Algoritmo (contrato do builder)
```
sum   = min(Σ weightOf(s) para s em signals, MAX_SCORE)
rule3 = S1 ∉ signals && S7 ∉ signals && S9 ∉ signals         → min(score, TRUSTED_STORE_CAP)
rule4 = !(S1 ∈ signals && signals ∩ {S2, S3, S7} ≠ ∅)        → min(score, NO_NFC_SIGNAL_CAP)
level = app.packageName ∈ ctx.trustedPackages ? GREEN : levelFor(score)
RiskResult(score, level, signals)
```
Regra 2 não mexe no score (decisão do usuário). As regras 3 e 4 são mínimos e comutam; o código as aplica na ordem da §5.

## Dependencies
- **Reusando:** `RiskScorer`, `ScoringContext`, `RiskResult`, `RiskLevel`, `Signal`, `InstalledApp` (SPEC-010); `anInstalledApp` do `testFixtures` (SPEC-012).
- **Novas:** nenhuma. `junit-jupiter` (BOM 5.14.4) já traz `junit-jupiter-params` para `@ParameterizedTest`/`@MethodSource`.

## Database Changes
Nenhuma. Score e nível vão para `app_snapshot` pela SPEC-018.

## Test Strategy Rationale
- **JUnit 5 no `core-domain`, sem mocks** (§11, RNF-08). O motor é função pura: entra app, sinais e contexto; sai `RiskResult`.
- **`WeightedRiskScorerExamplesTest`:** os cinco exemplos da §5, um teste com nome próprio cada (§11 e critério da #13).
- **`WeightedRiskScorerTableTest`:** a tabela do plano (P0–P9, C01–C19, T1–T8) em três `@MethodSource` (pesos, regras 3 e 4, regra 2). Cada linha confere score, nível e `signals` repassados. Os sinais são escritos como "S1 S2 S3" e traduzidos por um mapa escrito à mão no teste (S1 → `UNTRUSTED_INSTALL_SOURCE`, …): o teste nunca lê o `RiskScoreConfig`, então mudar um peso derruba casos.
- **`RiskScoreConfigTest`:** `levelFor` nos limites 0, 29, 30, 59, 60 e 100. É o único teste que chama o `object` `internal`; os pesos são conferidos só pelo motor.
- **Conferência local desfeita sem commit** (mutações que precisam derrubar testes): no contrato, `verification`.

## Trade-offs & Alternatives Considered
- **Pesos no `Signal`** — rejeitado (ADR-015, primeiro comentário da #13).
- **Pesos num `Map<Signal, Int>`** — rejeitado: um sinal sem peso passaria pela compilação.
- **Pesos injetáveis no construtor** (como os conjuntos dos coletores) — rejeitado: nada na especificação pede calibrar pelo SDK; acrescentar depois não quebra quem chama `WeightedRiskScorer()` se vier com padrão, mas isso é decisão de uma spec futura.
- **`RiskScoreConfig` público** — rejeitado: nenhum consumidor fora do módulo; mantém a API do SDK mínima.
- **Regras 3 e 4 como dados no arquivo de configuração** — rejeitado: viraria um mini-interpretador de regras; a §5 manda calibrar pesos, não regras.
- **Um teste por linha da tabela** — rejeitado: 37 funções quase iguais; `@ParameterizedTest` com nome por linha dá o mesmo diagnóstico.

## Risks
- **Primeiro `@ParameterizedTest` do `core-domain`:** `@MethodSource` precisa de função estática (`companion object` com `@JvmStatic`). Se o detekt acusar algo, ajustar na tarefa e registrar como emenda.
- **Kover:** o `object` `internal` e a classe entram na cobertura; `when` exaustivo sem `else` não deixa linha sem teste.
