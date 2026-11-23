# Test Plan: SPEC-015 Listas de apoio e imitação (S9)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-015-listas-imitacao.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-015-listas-imitacao.acceptance.toon` (versão 1.1) · 2026-11-21
> **BATCH-01** (esta seção): 3 alvos e 9 cenários (2 + 2 + 5), um método de teste por cenário. Execuções novas: 9 no `core-detection`, todas na JVM.
> **BATCH-02** (seção "BATCH-02: comparação de nomes", 2026-11-23): 5 alvos e 19 cenários (1 + 6 + 1 + 1 + 10), 84 execuções novas (os três parametrizados viram uma por caso: 20 + 35 + 13).
> **BATCH-03** (coletor): seção a acrescentar neste arquivo.

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

## BATCH-02: comparação de nomes

### Scope
- **`bankNameSkeleton`:** cada rótulo dá o esqueleto esperado (AC-005).
- **`editDistance`:** troca, inserção, remoção e transposição vizinha valem 1; iguais valem 0 (AC-006).
- **`BankNameMatcher.matches`:** imitadores e falsos positivos aceitos casam (AC-007, AC-009, AC-011); legítimos e imitações que escapam não casam (AC-010, AC-012); limites por tamanho (AC-008); esqueleto vazio (AC-014); nomes de `KnownBanks.apps` não se confundem entre instituições (AC-013).
- **Onde roda:** JUnit 4 na JVM, sem Robolectric, sem `@Config(sdk)` e sem `runTest` (nada é `suspend`).
- **Fora:** o pacote do app e o sinal (coletor, BATCH-03); rótulos reais (SPEC-022); ausência de `android.*` (review, AC-020).

### Padrões seguidos (pattern analysis)
- `InstallSourceCollectorTrustedStoreTest` e `NfcPermissionCollectorRelatedPermissionTest`: `@RunWith(Parameterized::class)`, `@Parameterized.Parameters(name = "{0}")`, lista escrita à mão no `companion object`, com KDoc.
- `KnownBanksTest` e `KnownBankAppTest`: frase com crases, asserções do JUnit 4, constantes num `private companion object`.
- `SKILLS.md` › Tests of a collector with no Android API: os casos parametrizados leem listas escritas à mão, nunca a lista de produção. A única exceção é o cenário 9 do `BankNameMatcherTest` (abaixo).

### Test data
- **`HAND_WRITTEN_BANK_NAMES`** (topo do `BankNameMatcherMatchTest.kt`, `internal`, usada também pelo `BankNameMatcherNoMatchTest`): 13 nomes escritos à mão, um por instituição da tabela do plano (Nubank, Itaú, Bradesco, CAIXA, next, Banco do Brasil, Santander, Mercado Pago, PicPay, Inter, PagBank, C6 Bank, Neon).
- Caracteres que não se distinguem na tela (cirílico, grego, largura cheia, espaço de largura zero, meia-risca, travessão, emoji) vão como escape `\uXXXX` no teste, com o caractere ou a descrição no comentário.
- **`SAME_BANK_PAIRS`** (no `BankNameMatcherTest`): os 6 pares de nomes de `KnownBanks.apps` que casam entre si, todos do mesmo banco e com pacotes na lista (saída do protótipo com as 29 entradas): Itaú Empresas → Itaú, Bradesco Cartões → Bradesco, Bradesco Empresas → Bradesco, Santander Empresas → Santander, Inter Empresas → Inter, BTG Pactual Empresas → BTG Pactual.

### `bankNameSkeleton` — `BankNameSkeletonTest.kt` (JUnit 4, `Parameterized`)
| # | Scenario | Casos (rótulo → esqueleto) | AC |
|---|----------|----------------------------|----|
| 1 | label-yields-expected-skeleton | maiúsculas e espaços (`NU BANK` → `nubank`); acentos (`Itáú` → `itau`); `:`, `\|`, ` - `, ` – `, ` — ` (um caso cada); separador no início não corta (`: Nubank` → `nubank`); largura cheia (`Ｎｕｂａｎｋ`); `0 1 ! 3 4 @ 5 $ 7` num caso e `Bradesc0`; `l` (`Calxa` → `caixa`); cirílico (`Nubаnk` e as 16 letras do mapa); grego (as 7 letras do mapa); `rn` (`rnercado` → `mercado`); `vv` (`vvave` → `wave`; com `l`, o esqueleto troca a letra, e `vvallet` daria `waiiet`); espaço de largura zero (`Ita` + U+200B + `ú` → `itau`); só emoji e pontuação → vazio; rótulo vazio → vazio | AC-005 |

### `editDistance` — `EditDistanceTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | equal-strings-have-distance-zero | `nubank`/`nubank` → 0 | AC-006 |
| 2 | one-substitution-has-distance-one | `nubank`/`nubamk` → 1 | AC-006 |
| 3 | one-insertion-or-deletion-has-distance-one | `bradesco`/`brandesco` → 1; `santander`/`santandr` → 1 | AC-006 |
| 4 | adjacent-transposition-has-distance-one | `nubank`/`nubnak` → 1 (seria 2 sem transposição) | AC-006 |
| 5 | empty-string-distance-is-the-other-length | `""`/`itau` → 4; `itau`/`""` → 4; `""`/`""` → 0 | AC-006 |
| 6 | two-edits-have-distance-two | `bancobrasii`/`bancodobrasii` → 2 | AC-006 |

### `BankNameMatcher` (casa) — `BankNameMatcherMatchTest.kt` (JUnit 4, `Parameterized`)
| # | Scenario | Casos | AC |
|---|----------|-------|----|
| 1 | label-matches-a-bank-name | imitadores da tabela do plano (Nubank e variações, Itaú e variações, Bradesco, Calxa, Santandr, PicPay, Mercad0 Pag0, Banco Brasil, títulos da Play copiados); seção comentada com os falsos positivos aceitos: `Inter`, `NEXT`, `Next - Launcher`, `Caixa – Organizador` | AC-007, AC-009, AC-011 |

### `BankNameMatcher` (não casa) — `BankNameMatcherNoMatchTest.kt` (JUnit 4, `Parameterized`)
| # | Scenario | Casos | AC |
|---|----------|-------|----|
| 1 | label-matches-no-bank-name | legítimos: Baixa, Caixinha, Caixa de Ferramentas, Next Launcher, Neon Light, Itaú Shop, Mercado Livre, Bradesco Seguros, C6; seção comentada com as imitações que escapam: Atualização Nubank, Nubank Wallpapers, Inter&Co, Neon Pay | AC-010, AC-012 |

### `BankNameMatcher` — `BankNameMatcherTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | prefix-and-suffixes-are-removed-including-glued-and-repeated | contra "Itaú": `Banco Itaú`, `ItauToken` (colado), `Itaú Cartões Empresas` (repetido) e `Itaú PJ Mobile Oficial Digital` (os sufixos com `l`, emenda 1.3) casam; `Banco Meu Itaú` (dois prefixos) não casa | AC-007 |
| 2 | five-letter-name-needs-equal-skeleton | contra "Caixa": `CAIXA` casa; `Baixa` e `Caixas` (distância 1) não | AC-008 |
| 3 | six-letter-name-tolerates-one-edit-but-not-two | contra "Nubank": `Nubamk` e `Nubnak` casam; `Nibamk` não | AC-008 |
| 4 | nine-letter-name-tolerates-one-edit-but-not-two | contra "Santander": `Santandr` casa; `Santadr` não | AC-008 |
| 5 | ten-letter-name-tolerates-two-edits-but-not-three | contra o nome fictício "Brasilcard" (10 letras): `Brazilcard` e `Brazilcart` casam; `Brazilkart` não | AC-008 |
| 6 | label-with-empty-skeleton-never-matches | só emoji e pontuação, e rótulo vazio, não casam | AC-014 |
| 7 | bank-name-with-empty-skeleton-is-ignored | lista com um nome só de emoji e "Nubank": `Nubank` casa; rótulos vazios não | AC-014 |
| 8 | empty-name-list-never-matches | lista vazia: `Nubank` não casa | AC-014 |
| 9 | known-bank-names-only-match-hand-written-same-bank-pairs | **lê `KnownBanks.apps` de produção** (exceção de propósito, arquitetura › Test Strategy): para cada par de entradas com pacotes diferentes, o nome de uma casa com o da outra só nos 6 pares de `SAME_BANK_PAIRS` | AC-013 |
| 10 | every-loaded-prefix-and-suffix-is-removed | percorre `BANK_NAME_PREFIXES` (3) e `BANK_NAME_SUFFIXES` (11), as listas já no formato do esqueleto (`internal`, emenda 1.3): contra "Itaú" (5 letras, só igual), `<prefixo> Itaú` e `Banco Itaú <sufixo>` casam, ou seja, cada entrada sai inteira. Pedido do usuário em 2026-11-23, depois que `mobile`, `oficial` e `digital` não saíam | AC-007 |

### Edge cases
- Limites 5/6 e 9/10 dos dois lados (cenários 2 a 5).
- Separador no início não corta; emoji (par substituto) e caractere invisível somem do esqueleto.
- Esqueleto vazio do rótulo e do nome da lista (cenários 6 e 7). Os dois se sobrepõem: como os limites 0, 1 e 2 são sempre menores que o tamanho do esqueleto do nome da lista, um rótulo vazio nunca casaria com um nome não vazio. As duas guardas são defesa e documentação, e tirar só uma não derruba teste.

### Conferência (tarefa 7, local e desfeita sem commit)
- Tirar o mapa de letras parecidas → falham os casos de cirílico e de `0→o`.
- Tolerância 1 para nomes de até 5 letras → falha `Baixa`.
- "Contém" no lugar da igualdade → falha `Caixa de Ferramentas`.
- Sem o corte no separador → falha `Next - Launcher`.
- (Além do plano) Prefixos e sufixos sem passar pelo esqueleto → falham os cenários 1 e 10 (o defeito corrigido no `fix:` da tarefa 5).
