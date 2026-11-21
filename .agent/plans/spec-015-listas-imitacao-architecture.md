# Architecture: SPEC-015 Listas de apoio e imitação (S9)

> Entrada: `.agent/plans/spec-015-listas-imitacao.md`, `.agent/specs/features/spec-015-listas-imitacao.spec.toon` (FT-015 v1.0, depois v1.1), `.agent/specs/acceptance/spec-015-listas-imitacao.acceptance.toon` (v1.0, depois v1.1)
> Contrato: `.agent/specs/contracts/spec-015-listas-imitacao.contract.toon` (v1.0) · 2026-11-21

## Summary
Três listas públicas e um coletor, Kotlin puro, em `core.detection.signal`, e a comparação de nomes num arquivo `internal`:
- `KnownWallets.packageNames: Set<String>`: 6 pacotes (BR-001), lista do S7 (consumidor na SPEC-016);
- `KnownBankApp(displayName, packageName)`: `data class` pública, com nome e pacote obrigatórios e não em branco;
- `KnownBanks.apps: Set<KnownBankApp>`: 29 entradas e 27 pacotes (BR-002);
- `BankNameMatcher` (`internal`): esqueleto do nome, variantes, distância OSA e limites por tamanho;
- `BankLookalikeCollector(knownBankApps: Set<KnownBankApp>)`: `{BANK_LOOKALIKE}` quando o rótulo casa e o pacote não é nenhum dos da lista.

| Pacote / arquivo | Conteúdo | Visibilidade | Batch |
|---|---|---|---|
| `core.detection.signal` / `KnownWallets.kt` | `object KnownWallets { val packageNames }` | público | 01 |
| `core.detection.signal` / `KnownBankApp.kt` | `data class KnownBankApp(displayName, packageName)` com `require` | público | 01 |
| `core.detection.signal` / `KnownBanks.kt` | `object KnownBanks { val apps }` | público | 01 |
| `core.detection.signal` / `BankNameMatcher.kt` | `class BankNameMatcher`, `fun bankNameSkeleton`, `fun editDistance`; separadores, mapa, prefixos, sufixos, variantes e limites | `internal` (classe e as duas funções); o resto `private` | 02 |
| `core.detection.signal` / `BankLookalikeCollector.kt` | `class BankLookalikeCollector : SignalCollector` | público | 03 |
| `core.domain` testFixtures / `DomainFixtures.kt` | `anInstalledApp` com `label` | público no `testFixtures` | 03 |

**API pública do `core-detection` depois desta spec:** as nove classes de antes, mais `KnownWallets`, `KnownBankApp`, `KnownBanks` e `BankLookalikeCollector` (treze). Nenhum tipo do Android nas novas.

## Revisão do plano (architect)

| Ponto | Resultado |
|---|---|
| **`KnownWallets`** | Fica. Casa com o glossário ("carteira conhecida") e com o texto do S7 ("lista de carteiras conhecidas"). `packageNames: Set<String>`, igual ao `TrustedStores`: o S7 compara o pacote do serviço padrão, então não precisa de tipo próprio |
| **`KnownBank` → `KnownBankApp`** | Uma entrada é um **app** (ou um apelido de app), não um banco: o Itaú tem três entradas (Itaú, Itaú Empresas, Personnalité) e o BB tem duas para o mesmo pacote. `KnownBank` levaria a crer que há uma entrada por instituição, e um teste ou a SPEC-017 poderia contar bancos errado |
| **`KnownBanks.entries` → `KnownBanks.apps`** | `entries` é o nome da propriedade dos `enum` do Kotlin 1.9+ e sugeriria um enum. `apps` diz o que o conjunto guarda. O `object` continua `KnownBanks`, que é o que a §5 chama de lista ("bancos conhecidos") |
| **`displayName`** | Fica, e não `label`: é o nome da marca escrito à mão, não o rótulo do aparelho (`InstalledApp.label`). O KDoc diz isso, e que os rótulos reais são conferidos na SPEC-022 |
| **Coletor** | `BankLookalikeCollector`, do `Signal.BANK_LOOKALIKE`, como `CardEmulationServiceCollector` ↔ `CARD_EMULATION_SERVICE`. O parâmetro é `knownBankApps: Set<KnownBankApp>`, obrigatório e sem padrão, como o `trustedInstallers` do S1 |
| **O que fica `internal` e `private`** | `internal`: a classe `BankNameMatcher` (o coletor a usa, e os testes do BATCH-02 a usam sem o coletor) e as funções `bankNameSkeleton` e `editDistance` (testadas direto: o esqueleto esperado de cada rótulo e a distância de cada par são critérios, AC-005 e AC-006). `private`: separadores, mapa de letras parecidas, prefixos, sufixos, `variants` e `tolerance`. Prefixos, sufixos e limites são cobertos pelo `BankNameMatcherTest` via `matches`, para não expor mais API. O módulo já usa `internal` (`PackageManagerVersionCompat.kt`); sem `@VisibleForTesting` |
| **Por que não `private` dentro do coletor** | Com a normalização privada no coletor, o BATCH-02 não teria o que entregar sozinho, e os testes de esqueleto e distância passariam pelo pacote e pelo sinal. O `BankNameMatcher` não conhece pacote nem sinal; o coletor junta os dois |
| **Validação de nome e pacote em branco** | **Mantida.** `require(displayName.isNotBlank())` e `require(packageName.isNotBlank())` no `init` do `KnownBankApp`, com mensagens fixas que não repetem o valor, como o `LastFourDigits`. Motivos: quem usa o SDK escreve `KnownBankApp(...)` à mão; um nome em branco daria esqueleto vazio e nunca casaria, sem aviso; um pacote em branco não tiraria app nenhum do S9. O `copy()` da data class passa pelo `init`. **Não** valida o formato do pacote (a comparação é exata; um formato inválido só não casa com nada) nem o esqueleto do nome (exigiria a normalização no BATCH-01; o `BankNameMatcher` descarta esqueletos vazios, BR-009) |
| **Data class sem coleção** | `KnownBankApp` não tem coleção, então não precisa da fábrica com cópia das regras de modelos (SPEC-010). O `DataClassShouldBeImmutable` só está configurado no `core-domain`, mas a classe só tem `val` |
| **Detekt** | O `core-detection` usa a configuração padrão. `MagicNumber`: os limites 5 e 9 viram `const val` privadas (0, 1 e 2 estão na lista de exceções do detekt). Complexidade: a distância OSA tem dois laços e um `if`, abaixo dos limites padrão (`NestedBlockDepth` 4, `CyclomaticComplexMethod` 15); o builder confere no build |
| **Minúsculas e acentos** | `lowercase()` do Kotlin já é invariante (equivale a `Locale.ROOT`). Acentos saem por `CharCategory.NON_SPACING_MARK` depois do NFD, sem regex, para não depender da diferença entre o regex da JVM e o do Android (ICU) |
| **Corte no separador** | Só corta quando há algo antes do separador; o primeiro separador é o de menor posição entre todos. Gera falso positivo aceito: "Next - Launcher" e "Caixa – Organizador" (de outro desenvolvedor) viram "Next" e "Caixa" e recebem S9. Os dois viram casos do `BankNameMatcherMatchTest`, separados por comentário, e entram no ADR-020 (pedido do usuário em 2026-11-21). O mesmo mecanismo vale para "Neon \| Wallpapers" e "Inter: Notícias do clube" (conferido no protótipo) |
| **Kover** | O `core-detection` fica fora do Kover (só o `core-domain` tem). Custo já aceito no ADR-017 para os coletores; aqui a cobertura vem dos casos parametrizados |
| **Documentação por batch** | A DoD pede a documentação do módulo em cada PR. O BATCH-01 cria o ADR-020 com todas as decisões (normalização e coletor marcados para os batches seguintes) e muda a §5. O BATCH-02 documenta o algoritmo (README e glossário). O BATCH-03 documenta o coletor, muda a §8 e conclui o ADR-020 |
| **Branches** | Seguem o que foi decidido na SPEC-014: `spec-015-listas-imitacao` (BATCH-01), `spec-015-listas-imitacao-batch-02` e `spec-015-listas-imitacao-batch-03`, cada uma criada a partir da `main` depois do merge do batch anterior. Só o PR do BATCH-03 leva `Closes #11` |
| **Estimativa** | 968 linhas pelos arquivos do contrato (tabela abaixo), contra 790 no plano. Com dois batches, o BATCH-02 do plano teria 649 linhas, acima de 500. **Emenda 1.1:** `estimate-lines: 970` e três batches, separando a normalização e os testes dela (BATCH-02) do coletor (BATCH-03), sem `single-pr-reason`, como pedido |

### Estimativa pelos arquivos do contrato (código e testes)

| Arquivo | Ação | Batch | Linhas |
|---|---|---|---|
| `core-detection/.../signal/KnownWallets.kt` | create | 01 | 41 |
| `core-detection/.../signal/KnownBankApp.kt` | create | 01 | 26 |
| `core-detection/.../signal/KnownBanks.kt` | create | 01 | 91 |
| `core-detection/src/test/.../signal/ExpectedKnownWallets.kt` | create | 01 | 16 |
| `core-detection/src/test/.../signal/ExpectedKnownBankApps.kt` | create | 01 | 40 |
| `core-detection/src/test/.../signal/KnownWalletsTest.kt` | create | 01 | 28 |
| `core-detection/src/test/.../signal/KnownBanksTest.kt` | create | 01 | 32 |
| `core-detection/src/test/.../signal/KnownBankAppTest.kt` | create | 01 | 45 |
| **Subtotal BATCH-01** | | | **319** |
| `core-detection/.../signal/BankNameMatcher.kt` | create | 02 | 140 |
| `core-detection/src/test/.../signal/BankNameSkeletonTest.kt` | create | 02 | 55 |
| `core-detection/src/test/.../signal/EditDistanceTest.kt` | create | 02 | 50 |
| `core-detection/src/test/.../signal/BankNameMatcherMatchTest.kt` | create | 02 | 70 |
| `core-detection/src/test/.../signal/BankNameMatcherNoMatchTest.kt` | create | 02 | 48 |
| `core-detection/src/test/.../signal/BankNameMatcherTest.kt` | create | 02 | 100 |
| **Subtotal BATCH-02** | | | **463** |
| `core-detection/.../signal/BankLookalikeCollector.kt` | create | 03 | 48 |
| `core-domain/src/testFixtures/.../DomainFixtures.kt` | modify | 03 | 3 |
| `core-detection/src/test/.../signal/BankLookalikeCollectorTest.kt` | create | 03 | 135 |
| **Subtotal BATCH-03** | | | **186** |
| **Total** | | | **968** |

- **Base da conta:** arquivos da SPEC-012. `TrustedStores` tem 49 linhas para 11 entradas (KDoc de 20 e duas linhas por entrada); `ExpectedTrustedStores`, 20; `InstallSourceCollector`, 35; `InstallSourceCollectorTest`, 90 (8 cenários); o teste parametrizado das lojas, 30.
- **Por que passou do plano (+178):** o plano contou a normalização junto do coletor (~145 de código) e ~320 de testes. Pelo contrato, o `BankNameMatcher` sozinho tem ~140 (KDoc do algoritmo, mapa de letras parecidas, variantes, OSA e limites) e os testes da comparação viraram cinco arquivos (esqueleto, distância, casa, não casa, limites), porque cada grupo parametrizado é uma classe (regra do `SKILLS.md`).
- **Testes:** cerca de 72% das linhas (699).
- **BATCH-02 com 463:** é o mais perto do limite. Se passar de 500 na implementação, a saída é mover o `EditDistanceTest` e a `editDistance` para um batch próprio antes do BATCH-02, não juntar batches.
- A documentação não entra na conta (regra do SDD 1.8.0).

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Lista pública sem alteração, com fonte por entrada:** `TrustedStores` (SPEC-012, ADR-017), `Collections.unmodifiableSet(linkedSetOf(...))`.
  2. **Coletor com conjunto obrigatório e copiado:** `InstallSourceCollector(trustedInstallers)` (SPEC-012).
  3. **Lista esperada escrita à mão e teste parametrizado sobre ela:** `ExpectedTrustedStores` e `InstallSourceCollectorTrustedStoreTest`; `NfcPermissionCollectorRelatedPermissionTest` (SPEC-013); `SmsPermissionCollectorUnrelatedPermissionTest` (SPEC-014).
  4. **Validação com `require` e mensagem que não repete o valor:** `LastFourDigits` (SPEC-010).
  5. **Funções `internal` testadas direto no módulo:** `PackageManagerVersionCompat.kt` (SPEC-011).
- **Padrão seguido:** 1 nas três listas; 2 no coletor; 3 nos testes de listas e da comparação; 4 no `KnownBankApp`; 5 no `BankNameMatcher`.

## Layer Decisions

| Camada | Decisão | Justificativa |
|---|---|---|
| `core-detection` (listas) | Três arquivos: `KnownWallets`, `KnownBankApp`, `KnownBanks` | Formato do `TrustedStores` (BR-003); um arquivo por tipo (critério da #11) |
| `core-detection` (comparação) | `BankNameMatcher.kt`, `internal` | Lógica isolada e testável sem o coletor; permite o BATCH-02 sozinho |
| `core-detection` (coletor) | `BankLookalikeCollector`, uma classe por sinal | ADR-017: coletores no `core-detection`, mesmo sem API do Android |
| `core-domain` (testFixtures) | Parâmetro `label` na `anInstalledApp` | Os testes do coletor trocam o rótulo; hoje ele é fixo em `"Example"` |
| `core-domain` (modelo) | Sem mudança | `InstalledApp` já tem `label` e `packageName`; `ScoringContext` não muda (SPEC-017) |

## Dependencies
- **Reusing:** `SignalCollector`, `Signal.BANK_LOOKALIKE`, `InstalledApp` (SPEC-010); `TrustedStores` e `InstallSourceCollector` como modelo (SPEC-012); `anInstalledApp` (SPEC-012, SPEC-014).
- **JDK e Kotlin:** `java.text.Normalizer` (NFKC, NFD; API 1 no Android), `java.util.Collections`, `CharCategory`.
- **New:** nenhuma biblioteca.

## Database Changes
Nenhuma. O S9 vai no bit 8 de `app_snapshot.signals` pela SPEC-018. O `1.json` não muda (ADR-014).

## Test Strategy Rationale
- **Tudo em JUnit 4 sem Robolectric:** nada toca o Android (`SKILLS.md` › Tests of a collector with no Android API).
- **Listas:** conteúdo exato contra listas escritas à mão; cast para `MutableSet` lança `UnsupportedOperationException`.
- **Comparação, cinco arquivos:**
  - esqueleto parametrizado (rótulo → esqueleto esperado);
  - distância com casos fixos;
  - "casa" e "não casa" parametrizados sobre uma lista de nomes de banco escrita à mão no teste (não a de produção). Os falsos positivos aceitos e as imitações que escapam ficam em seções comentadas, documentando o comportamento;
  - limites, esqueleto vazio e lista vazia num teste comum.
- **Exceção de propósito:** `known-bank-names-only-match-hand-written-same-bank-pairs` lê `KnownBanks.apps` de produção. Ele protege a lista: uma entrada nova que colida com o nome de outra instituição derruba o teste. Os pares do mesmo banco que podem casar (Itaú Empresas → Itaú etc.) ficam escritos à mão no teste.
- **Coletor:** lista pequena no teste (Nubank, Itaú, Itaú Empresas) e um banco fictício para a extensão pelo SDK, que é o mesmo mecanismo que a variante `lookalike` do app isca vai usar.
- **Fora dos testes automáticos:** rótulos reais (SPEC-022); uso de `KnownWallets` pelo S7 (SPEC-016); imports (review); desempenho (SPEC-022).

## Trade-offs & Alternatives Considered
- **`KnownBank` com conjunto de nomes e de pacotes por instituição:** rejeitado. Exigiria fábrica com cópia de coleções (regras da SPEC-010) e um teste de lista mais difícil de ler. O par (nome, pacote) com apelidos cobre os mesmos casos.
- **`KnownBanks.packageNames` além de `apps`:** rejeitado por ora. A SPEC-017 deriva o conjunto com `map`, se precisar (comentário na #13).
- **Normalização no `core-domain`:** rejeitada. Só o coletor a usa, e o ADR-017 põe os coletores e o que eles usam no `core-detection`.
- **Regex para tirar acentos e símbolos:** rejeitado; `CharCategory` e um filtro de caracteres não dependem do motor de regex.
- **Levenshtein sem transposição:** rejeitado; "Nubnak" daria 2 e escaparia do limite de 1.
- **Validar o formato do pacote no `KnownBankApp`:** rejeitado; a comparação é exata e um formato inválido só não casa.

## Risks
- **BATCH-02 perto do limite (463):** se passar de 500, separar a distância num batch próprio (acima).
- **Rótulos não conferidos:** um imitador com o rótulo real de um app oficial que difira do nome da marca escapa (por exemplo, "Inter&Co"). SPEC-022.
- **Falsos positivos aceitos:** nome curto igual e corte no separador. No máximo amarelo (regras 3 e 4); a pessoa pode marcar como confiável.
- **Pacote emprestado:** ADR-017 e issue #65.
