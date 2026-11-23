# Test Plan: SPEC-015 Listas de apoio e imitação (S9)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-015-listas-imitacao.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-015-listas-imitacao.acceptance.toon` (versão 1.1) · 2026-11-21
> **BATCH-01** (esta seção): 3 alvos e 9 cenários (2 + 2 + 5), um método de teste por cenário. Execuções novas: 9 no `core-detection`, todas na JVM.
> **BATCH-02** (comparação de nomes) e **BATCH-03** (coletor): seções a acrescentar neste arquivo em cada batch.

## Scope (BATCH-01)
- **`KnownWallets`:** a lista tem exatamente os 6 pacotes do BR-001 e não pode ser alterada nem por cast (AC-001, AC-003).
- **`KnownBanks`:** a lista tem exatamente as 29 entradas do BR-002 e não pode ser alterada nem por cast (AC-002, AC-003).
- **`KnownBankApp`:** nome e pacote preenchidos criam a entrada; nome ou pacote vazio ou só com espaços lança `IllegalArgumentException` (AC-004).
- **Onde roda:** JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`, dentro do `./gradlew build`.
- **Fora (contrato, `excluded`):**
  - uso de `KnownWallets` pelo S7 (SPEC-016);
  - rótulos reais dos apps de banco (SPEC-022);
  - ausência de `import android.*` e de Hilt nas listas (inspeção no review, AC-020).
  - AC-021 e AC-022 são conferidos pelo build e pelo `git status`; AC-023, pela documentação e pelo CI.

## Padrões seguidos (pattern analysis)
- **`ExpectedTrustedStores.kt` (SPEC-012):** lista `internal` escrita à mão num arquivo próprio, com KDoc dizendo que os testes não leem a lista de produção para montar o esperado.
- **`InstallSourceCollectorTest` (SPEC-012):** `default list has exactly the expected stores` (`assertEquals(EXPECTED.toSet(), TrustedStores.packageNames)`) e `default list rejects mutation through cast` (`assertThrows(UnsupportedOperationException::class.java)`).
- **`LastFourDigitsTest` (SPEC-010, `core-domain`):** valores inválidos lançam `IllegalArgumentException`; um valor válido cria sem erro.
- Nomes em frase com crases, constantes num `private companion object`, asserções do JUnit 4 (`skills/android/testing.md`).

## Unit tests

### `KnownWallets` — `core-detection/src/test/.../signal/KnownWalletsTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | default-list-has-exactly-the-expected-wallets | `KnownWallets.packageNames` igual a `EXPECTED_KNOWN_WALLETS.toSet()` | AC-001 |
| 2 | default-list-rejects-mutation-through-cast | `add` depois de cast para `MutableSet<String>` lança `UnsupportedOperationException` | AC-003 |

### `KnownBanks` — `core-detection/src/test/.../signal/KnownBanksTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | default-list-has-exactly-the-expected-bank-apps | `KnownBanks.apps` igual a `EXPECTED_KNOWN_BANK_APPS.toSet()` | AC-002 |
| 2 | default-list-rejects-mutation-through-cast | `add` depois de cast para `MutableSet<KnownBankApp>` lança `UnsupportedOperationException` | AC-003 |

### `KnownBankApp` — `core-detection/src/test/.../signal/KnownBankAppTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | filled-name-and-package-create-the-entry | `displayName` e `packageName` lidos de volta | AC-004 |
| 2 | empty-display-name-is-rejected | `""` como nome → `IllegalArgumentException` | AC-004 |
| 3 | blank-display-name-is-rejected | `"   "` como nome → `IllegalArgumentException` | AC-004 |
| 4 | empty-package-name-is-rejected | `""` como pacote → `IllegalArgumentException` | AC-004 |
| 5 | blank-package-name-is-rejected | `"   "` como pacote → `IllegalArgumentException` | AC-004 |

## Edge cases
- Apelidos: duas entradas com o mesmo pacote e nomes diferentes (BB e Banco do Brasil; Santander Brasil e Santander; BTG Pactual Banking e BTG Pactual) são entradas distintas no conjunto, e a lista esperada as tem.
- Pacote com maiúscula (`br.com.Inter.CDPro`) escrito exatamente como na Play.
- Espaços em branco (cenários 3 e 5 do `KnownBankApp`): `isNotBlank`, não só `isNotEmpty`.

## Test data & fixtures
- **`ExpectedKnownWallets.kt`:** `internal val EXPECTED_KNOWN_WALLETS: List<String>`, os 6 pacotes escritos à mão.
- **`ExpectedKnownBankApps.kt`:** `internal val EXPECTED_KNOWN_BANK_APPS: List<KnownBankApp>`, as 29 entradas escritas à mão.
- Tirar ou pôr uma entrada na lista de produção derruba o teste de conteúdo dela (conferência da tarefa 3).

## Infrastructure
- Nenhuma biblioteca nova: `junit4` já está no `core-detection`. Os testes não precisam de `runTest` nem da fixture `anInstalledApp`.
