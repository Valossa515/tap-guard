# Architecture: SPEC-010 Modelos e contratos do domínio

> Entrada: `.agent/plans/spec-010-modelos-dominio.md`, `.agent/specs/features/spec-010-modelos-dominio.spec.toon` (FT-010), `.agent/specs/acceptance/spec-010-modelos-dominio.acceptance.toon`
> Contrato: `.agent/specs/contracts/spec-010-modelos-dominio.contract.toon` · 2026-11-18

## Summary
Tudo fica no `core-domain`, no pacote `io.github.valossa515.tapguard.core.domain`, com um subpacote por assunto. Cada tipo tem um arquivo próprio.

| Pacote | Conteúdo |
|--------|----------|
| `core.domain` (raiz) | `UnmodifiableCopies.kt`: utilitário `internal` de cópia defensiva |
| `core.domain.inventory` | `InstalledApp`, `AppInventory` |
| `core.domain.signal` | `Signal`, `SignalBitMask.kt` (conversão da máscara), `SignalCollector` |
| `core.domain.risk` | `RiskLevel`, `RiskResult`, `ScoringContext`, `RiskScorer` |
| `core.domain.scan` | `ScanTrigger`, `ScannedApp`, `ScanReport`, `ScanDevice` |
| `core.domain.card` | `NfcTagHandle`, `LastFourDigits`, `CardReadResult`, `CardReader` |

Nenhuma dependência nova. O módulo continua só com `kotlin("jvm")`, ktlint, detekt e Kover. O detekt passa a ler um `core-domain/detekt.yml` com `DataClassShouldBeImmutable`. Os testes são JUnit 5 na JVM, em `src/test`, nos mesmos pacotes, sem mocks.

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Configuração do detekt por módulo:** `app/detekt.yml` com só as regras alteradas, ligado no `app/build.gradle.kts` por `detekt { buildUponDefaultConfig = true; config.setFrom(files("detekt.yml")) }`. O `core-domain` repete exatamente esse bloco.
  2. **Pacotes por assunto dentro do módulo:** o `core-data` separa `database`, `database.entity`, `database.dao`, `settings` e `di` (SPEC-004), e o `app` separa `navigation`, `ui.components` e `ui.theme`. O `core-domain` segue a mesma ideia: `inventory`, `signal`, `risk`, `scan` e `card`.
  3. **Enum com KDoc em português e nomes persistidos em UPPER_SNAKE:** `RiskLevelUi` (`GREEN`, `YELLOW`, `RED`) no `app` e os valores de enum gravados como texto no `core-data` (`trigger`, `level`). `RiskLevel` e `ScanTrigger` usam os mesmos nomes.
  4. **Testes:** uma classe por unidade, nomes com crases, sem classe-base (`CoreDomainSmokeTest` com `org.junit.jupiter.api`, `RiskLevelColorsTest`, `SettingsDataSourceTest`). Constantes nomeadas no lugar de números soltos (`DATABASE_NAME`, `DATASTORE_NAME`), como o `MagicNumber` do detekt exige.
- **Padrão seguido:** os quatro acima. Para os modelos, que ainda não existem no repositório, vale `skills/android/SKILL.md` › Domain Layer (modelos imutáveis, portas como interfaces, falhas esperadas como sealed), com as decisões do plano por cima dos exemplos da skill: `RiskScorer` é interface, como na §7; `ScoringContext` não tem `rules`; o subtipo de leitura interrompida chama `ReadInterrupted`.

## Layer Decisions

| Camada (pacote) | Decisão | Justificativa |
|------|---------|---------------|
| build | `core-domain/build.gradle.kts` ganha o bloco `detekt` do padrão 1; `core-domain/detekt.yml` ativa `style > DataClassShouldBeImmutable` | BR-003; padrão 1 |
| `core.domain` | `UnmodifiableCopies.kt`: `internal fun <T> Set<T>.unmodifiableCopy()` = `Collections.unmodifiableSet(LinkedHashSet(this))` e `internal fun <T> List<T>.unmodifiableCopy()` = `Collections.unmodifiableList(ArrayList(this))` | BR-003. Cópia mais embrulho: alterar a origem não muda o modelo, e um cast para `MutableSet` ou `MutableList` lança `UnsupportedOperationException`. `Set.copyOf` e `List.copyOf` só existem no Android a partir da API 30; `toSet()` e `toList()` devolvem coleções que um cast consegue alterar |
| `core.domain.inventory` | `InstalledApp`: data class com construtor privado, `@ConsistentCopyVisibility` e `companion operator fun invoke` com os mesmos parâmetros, que copia `requestedPermissions` e `services`. `AppInventory`: interface da §7 | BR-003, BR-008, BR-009. Sem campo de app do sistema |
| `core.domain.signal` | `Signal`: `enum class Signal(val bit: Int)`, nove entradas na ordem S1 a S9, com o bit por argumento nomeado (`bit = 0` … `bit = 8`) e o código no KDoc. `SignalBitMask.kt`: `fun Set<Signal>.toBitMask(): Int` e `fun signalsFromBitMask(mask: Int): Set<Signal>`; máscara com bit fora dos nove sinais lança `IllegalArgumentException` por `require` (mensagem com a máscara recebida); o conjunto devolvido passa por `unmodifiableCopy()`. `SignalCollector`: interface da §7 | BR-004, BR-005. Bit explícito em vez de `ordinal`: reordenar as entradas não muda a máscara gravada. A máscara válida é calculada a partir de `Signal.entries`, sem número solto |
| `core.domain.risk` | `RiskLevel`: `GREEN`, `YELLOW`, `RED` nessa ordem. `RiskResult`: data class com construtor privado, `@ConsistentCopyVisibility`, fábrica que copia `signals` e `init { require(score in MIN_SCORE..MAX_SCORE) }` com as constantes `private const val` no `companion`. `ScoringContext`: só `trustedPackages`, com construtor privado e fábrica. `RiskScorer`: interface da §7 | BR-006, BR-007, BR-010. A validação fica no `init`, não só na fábrica, para valer em toda construção. Constantes privadas: o teto e as faixas usados pelo motor são da SPEC-017 |
| `core.domain.scan` | `ScanTrigger`: `MANUAL`, `APP_OPEN`, `PERIODIC`. `ScannedApp(app, risk)`: data class comum (sem coleção, construtor público). `ScanReport`: construtor privado, `@ConsistentCopyVisibility`, fábrica que copia `apps` e `init { require(!finishedAt.isBefore(startedAt)) }` (emenda 1.1). `ScanDevice`: interface da §7 (`suspend operator fun invoke`) | BR-011. Sem id e sem contagens |
| `core.domain.card` | `NfcTagHandle`: `interface NfcTagHandle` sem corpo, com KDoc dizendo que o `core-nfc` embrulha a `Tag`. `LastFourDigits`: `@JvmInline value class LastFourDigits(val value: String)` com `init { require(...) }` (tamanho `private const val` 4 e cada caractere entre `'0'` e `'9'`) e `toString()` mascarado. `CardReadResult`: `sealed interface` com `data class Success(applicationLabel: String, lastFourDigits: LastFourDigits, expiry: YearMonth)` com `toString()` mascarado, `data object UnsupportedCard` e `data object ReadInterrupted`. `CardReader`: interface da §7 | BR-012 a BR-015. Comparação com `'0'..'9'`, não `isDigit()`, que aceita dígitos de outros alfabetos. **A mensagem do `require` não repete o valor recebido**: a entrada recusada pode ser um número completo |

**Texto do `toString()` mascarado:** fixo, sem nenhum valor dos campos: `LastFourDigits(****)` e `Success(****)`.

**Dependências entre pacotes:** `scan → risk, inventory`; `risk → signal, inventory`; `signal → inventory` (`SignalCollector` recebe `InstalledApp`); todos → raiz (`unmodifiableCopy`). `card` é independente. Nada aponta para fora do módulo.

**Cópia defensiva, um caminho só:** nos quatro modelos com coleção (`InstalledApp`, `RiskResult`, `ScoringContext`, `ScanReport`), o construtor é privado, e o `copy()` também (`@ConsistentCopyVisibility`). A fábrica `operator fun invoke` do `companion` é a única entrada pública e sempre copia. Na chamada, a forma continua `InstalledApp(...)`, com argumentos nomeados.

## Dependencies
- **Reusing:**
  - `core-domain/build.gradle.kts` (plugins `kotlin.jvm`, `ktlint`, `detekt`, `kover`, regra de 80% e JUnit 5);
  - o padrão de configuração do detekt do `app`;
  - JDK: `java.time.Instant`, `java.time.YearMonth`, `java.util.Collections`, `LinkedHashSet`, `ArrayList`. Todos existem no Android desde a API 26 ou antes.
- **New:** nenhuma. `suspend` nas interfaces é recurso da linguagem e não precisa de `kotlinx-coroutines`.

## Database Changes
Nenhuma. As funções da máscara seguem a ordem de `app_snapshot.signals` (S1 no bit 0 até S9 no bit 8, `conventions.md`). O `core-data` passa a usá-las na SPEC-018.

## Test Strategy Rationale
- **Tudo em teste unitário JUnit 5 na JVM:** os tipos são Kotlin puro e não têm colaboradores. Sem mocks e sem Robolectric (`testing.md`). Nenhum teste de integração: não há adaptador nem I/O nesta spec.
- **Fixtures:** `DomainFixtures.kt` em `src/test` (`anInstalledApp(...)` e `aRiskResult(...)` com argumentos padrão), usado pelos testes de `ScanReport` e `CardReadResult`. Pacotes e rótulos sintéticos (`com.example...`).
- **Imutabilidade:** em cada modelo com coleção, dois testes. O primeiro constrói com uma coleção mutável, altera a origem e confere que o modelo não mudou. O segundo faz cast da coleção exposta para `MutableSet` ou `MutableList` e confere `UnsupportedOperationException` em `add`, `remove` e `clear`. O `var` barrado e o `copy()` privado não são testáveis em tempo de execução: o primeiro tem verificação negativa local do detekt (AC-003), e o segundo, inspeção no review (AC-006).
- **Máscara:** exaustiva. As 512 combinações (2⁹) cabem num teste rápido, e o bit de cada sinal fica fixado um a um, de modo que uma reordenação acidental quebra o teste.
- **Cartão:** `LastFourDigits` com casos válidos e inválidos, incluindo o número de teste sintético `4111111111111111` e dígitos arábico-índicos. `toString()` de `LastFourDigits` e de `Success` sem dígitos, validade ou rótulo. Um `when` exaustivo sem `else` sobre `CardReadResult` fixa os três subtipos: um quarto subtipo quebra a compilação do teste.
- **Cobertura:** as linhas executáveis são as fábricas, os `init`, os enums, a conversão da máscara, os `toString()` e o `UnmodifiableCopies.kt`, este coberto pelos testes dos modelos. Meta de 100%; a regra exige 80%. A verificação negativa (modelo temporário sem teste derruba o `koverVerify`) prova que a regra está medindo.
- **Excluídos:** as seis interfaces (sem código executável; assinaturas conferidas na compilação e no review); `UnmodifiableCopies.kt` sem teste direto (coberto pelos modelos); `detekt.yml` (verificação negativa local); `ScannedApp` sem teste próprio (coberto no `ScanReportTest`).
- **Removido:** `core-domain/src/test/kotlin/io/github/valossa515/tapguard/core/domain/CoreDomainSmokeTest.kt`. O schema do contrato só aceita `create` e `modify`, então a remoção está registrada no cabeçalho do contrato.

## Trade-offs & Alternatives Considered
- **`NfcTagHandle` com canal de comandos (`transceive`) no domínio:** rejeitado. Anteciparia a SPEC-041 e faria a resposta bruta do cartão, com o número completo, passar por um contrato do domínio (ADR-008).
- **`kotlinx-collections-immutable`:** rejeitado pelo usuário. Seria uma dependência anterior à 1.0 num módulo que vai virar SDK.
- **Só tipos de leitura (`Set`, `List`) por convenção:** rejeitado. Não impede alteração pela coleção de origem nem por cast.
- **`buildSet`/`buildList` do Kotlin:** também recusam alteração depois de prontos, mas o comportamento sob cast é detalhe de implementação da stdlib. `Collections.unmodifiable*` é contrato documentado do JDK.
- **Máscara no `core-data` (conversor do Room):** rejeitado pelo usuário. A ordem dos bits é regra do domínio e é testada na JVM com o Kover.
- **`ordinal` como bit:** rejeitado. Reordenar as entradas mudaria valores já gravados.
- **`isSystemApp` no `InstalledApp`:** rejeitado pelo usuário. A regra 1 fica no `AppInventory` (SPEC-011).
- **`NfcUnavailable` e `NfcDisabled` no `CardReadResult`:** rejeitados pelo usuário. Não acontecem em `read(tag)`.
- **Validação só na fábrica:** rejeitado. O `init` cobre também a construção interna.

## Risks
- **Resolução da fábrica com o mesmo nome do construtor privado:** fora da classe, `InstalledApp(...)` precisa resolver para o `invoke` do `companion`; dentro da fábrica, para o construtor (sem recursão). Se a primeira compilação não confirmar isso, o builder para e pergunta.
- **`@ConsistentCopyVisibility` no Kotlin 2.4.21:** se o compilador avisar que a anotação é redundante ou desconhecida, o builder para e pergunta (sem aviso novo, AC-023).
- **`DataClassShouldBeImmutable` com construtor privado:** a regra olha só `var`; não deve reclamar. Se reclamar de outra coisa, o builder para e pergunta, sem `@Suppress`.
- **`MagicNumber`:** bits por argumento nomeado (ignorados pela regra); limites como `private const val` no `companion` (ignorados). Sem `@Suppress`.
- **Value class com `companion` e `toString()`:** suportado pelo Kotlin. Em parâmetros e campos comuns, o `LastFourDigits` vira `String` no bytecode, mas o tipo continua obrigatório no código Kotlin, que é onde está a barreira.
- **`applicationLabel` com dígitos:** o tipo não impede. Fica para o review da SPEC-041 e para o teste de privacidade que busca o número sintético.
