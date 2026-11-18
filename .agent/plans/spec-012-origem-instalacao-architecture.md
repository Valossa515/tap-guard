# Architecture: SPEC-012 Origem da instalação (S1)

> Entrada: `.agent/plans/spec-012-origem-instalacao.md`, `.agent/specs/features/spec-012-origem-instalacao.spec.toon` (FT-012 v1.1), `.agent/specs/acceptance/spec-012-origem-instalacao.acceptance.toon` (v1.0)
> Contrato: `.agent/specs/contracts/spec-012-origem-instalacao.contract.toon` (v1.0) · 2026-11-18

## Summary
O primeiro `SignalCollector` do projeto, `InstallSourceCollector`, fica em `io.github.valossa515.tapguard.core.detection.signal`. Ele é Kotlin puro dentro do `core-detection`: recebe o conjunto de instaladores confiáveis na construção, guarda uma cópia que não pode ser alterada e devolve `{UNTRUSTED_INSTALL_SOURCE}` quando `installerPackageName` é nulo ou está fora do conjunto.

A lista padrão é o `object TrustedStores`, no mesmo pacote, com a propriedade pública `packageNames`. Cada entrada leva um comentário com o nome da loja e a fonte.

As fixtures do domínio vão para `core-domain/src/testFixtures`. O detekt passa a analisar esse diretório, e o Kover o exclui da cobertura.

| Pacote / arquivo | Conteúdo | Visibilidade |
|---|---|---|
| `core.detection.signal` / `TrustedStores.kt` | `object TrustedStores { val packageNames: Set<String> }`, com 11 pacotes | público |
| `core.detection.signal` / `InstallSourceCollector.kt` | `class InstallSourceCollector(trustedInstallers: Set<String>) : SignalCollector` | público (classe e construtor); o campo é `private` |
| `core-domain/src/testFixtures` / `DomainFixtures.kt` | `anInstalledApp(...)` (com `installerPackageName`) e `aRiskResult(...)` | fixtures de teste |

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Implementação de porta do domínio no `core-detection`, num pacote que espelha o domínio:** `PackageManagerAppInventory` em `core.detection.inventory`, implementando `core.domain.inventory.AppInventory`. O coletor vai para `core.detection.signal`, implementando `core.domain.signal.SignalCollector`.
  2. **Coleção exposta que recusa alteração mesmo por cast:** `InstalledApp`, `ScoringContext` e `ScanReport` usam `Collections.unmodifiableSet/List` sobre uma cópia (`unmodifiableCopy()`, `internal` no `core-domain`). A lista padrão e a cópia guardada pelo coletor seguem o mesmo idioma.
  3. **Constantes nomeadas num lugar só, com KDoc em português:** `SYSTEM_FLAGS` e `DETAIL_FLAGS` no `companion` do inventário; `MIN_SCORE`/`MAX_SCORE` no domínio. A lista de lojas é um arquivo só (skill › Configuration: allow-lists num arquivo).
  4. **Fixtures como funções de topo com argumentos padrão, expondo só o que os testes trocam:** `DomainFixtures.kt` (SPEC-010) e `aPackageInfo(...)` (SPEC-011).
  5. **Construtor sem valor padrão para dependência que muda o comportamento:** o `ioDispatcher` do inventário é obrigatório (ADR-016, alternativa rejeitada: padrão `Dispatchers.IO`).
- **Padrão seguido:** os cinco acima, mais `skills/android/testing.md` (JUnit 4 em módulo Android; Test Fixtures no `testFixtures` do `core-domain`; nomes com crases).

## Layer Decisions

| Camada | Decisão | Justificativa |
|---|---|---|
| build (`core-domain`) | Plugin `` `java-test-fixtures` `` (núcleo do Gradle, sem versão no catálogo). `detekt { source.from(files("src/testFixtures/kotlin")) }`. `kover { currentProject { sources { excludedSourceSets.add("testFixtures") } } }`, ao lado do `reports { verify { … } }` | BR-009, BR-010. Experimento: sem essas linhas, o `detekt` do `check` ignora o `testFixtures` e o Kover conta `DomainFixturesKt` |
| build (`core-detection`) | `testImplementation(testFixtures(project(":core-domain")))`, antes de `libs.junit4` | BR-009. Nenhuma dependência nova de produção: `core-domain` e corrotinas já são `api` |
| `core-domain` (testes) | `DomainFixtures.kt` de `src/test` para `src/testFixtures`, mesmo pacote `io.github.valossa515.tapguard.core.domain`. `anInstalledApp` ganha `installerPackageName: String? = "com.android.vending"` logo depois de `packageName`. `aRiskResult` igual. `src/main` sem mudança | BR-009. Os testes do `core-domain` enxergam o `testFixtures` sem mudar import (mesmo pacote) |
| `core.detection.signal` / lista | `object TrustedStores` com `val packageNames: Set<String> = Collections.unmodifiableSet(linkedSetOf(...))`, com os 11 pacotes na ordem da BR-004. Cada linha tem um comentário com a loja e a fonte (lista de verificação do Google, §5, Talsec, APKMirror, bayton, Stack Overflow). KDoc do objeto: o que entra, o que fica fora e por quê (instaladores do sistema, F-Droid, Aurora, apps de clonagem; ADR-017), e como o SDK amplia (`TrustedStores.packageNames + "com.banco.loja"`) ou reduz (`- "com.amazon.venezia"`) | BR-004 a BR-007. `object` dá um nome ao conjunto para quem usa o SDK; a SPEC-015 segue a forma (um `object` por tipo no mesmo pacote). Para S9 os bancos precisam do nome como dado; o tipo da entrada fica para a SPEC-015 |
| `core.detection.signal` / coletor | `class InstallSourceCollector(trustedInstallers: Set<String>) : SignalCollector`. Guarda `private val trustedInstallers = Collections.unmodifiableSet(LinkedHashSet(trustedInstallers))`. `override suspend fun collect(app: InstalledApp): Set<Signal> = if (app.installerPackageName in trustedInstallers) emptySet() else UNTRUSTED`, com `private val UNTRUSTED = setOf(Signal.UNTRUSTED_INSTALL_SOURCE)` no `companion`. Parâmetro **obrigatório**, sem padrão. KDoc: nulo e fora geram S1; comparação exata; lista vazia faz todo app receber S1; não chama o `PackageManager`; limitações do ADR-017 (falsificação com shell ou root, nome de pacote emprestado) | BR-001 a BR-003, BR-006, BR-008. `in` sobre `String?` trata o nulo sem ramo à parte (`null in Set<String>` é `false`). Sem dispatcher: não bloqueia. `unmodifiableCopy()` é `internal` no `core-domain` e não é reaproveitado; tornar o helper público mudaria `core-domain/src/main` e a API do SDK por uma linha |

**API pública do `core-detection` depois desta spec:** `PackageManagerAppInventory`, `InstallSourceCollector` e `TrustedStores`. Nenhum tipo do Android nos dois novos. O `explicitApi()` continua como ponto aberto no ADR-016.

**Emenda 1.1 na spec (feita nesta revisão):** a linha `trustedInstallers` dizia "padrão = lista pública", o que dava para ler como valor padrão do parâmetro. Passa a dizer "obrigatório, sem valor padrão; a lista padrão pública é `TrustedStores.packageNames`".

## Dependencies
- **Reusing:**
  - `SignalCollector`, `Signal` e `InstalledApp` (`core-domain`, SPEC-010);
  - `junit4` e `kotlinx-coroutines-test` (`testImplementation` do `core-detection` desde a SPEC-011);
  - os testes do instalador da SPEC-011 (AC-010).
- **New:** nenhuma biblioteca. O plugin `java-test-fixtures` vem com o Gradle.

## Database Changes
Nenhuma.

## Test Strategy Rationale
- **JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`:** o coletor não toca em API do Android, então não há shadow a usar nem ramo por versão (testing.md › Which runner where: JUnit 4 em módulo Android). `runTest` porque `collect` é `suspend`.
- **Três classes:**
  - **`InstallSourceCollectorTrustedStoreTest`:** `@RunWith(Parameterized::class)`, um caso por pacote da lista esperada, com o pacote no nome do caso (`@Parameters(name = "{0}")`). Cada caso devolve conjunto vazio (AC-001).
  - **`InstallSourceCollectorUntrustedInstallerTest`:** `Parameterized`, oito instaladores: os quatro de AC-003 e os quatro de AC-004. Cada um devolve `{S1}`.
  - **`InstallSourceCollectorTest`:** os cenários com nome próprio: nulo, maiúsculas, só S1, lista ampliada, lista menor, cópia do conjunto, lista padrão igual à esperada e lista padrão que recusa alteração.
  - O `Parameterized` do JUnit 4 recebe os parâmetros pelo construtor da classe. Por isso cada grupo parametrizado é uma classe.
- **Lista esperada literal no teste:** `ExpectedTrustedStores.kt` em `core-detection/src/test/.../signal`, com `internal val EXPECTED_TRUSTED_STORES` (os 11 pacotes escritos à mão). A classe parametrizada não lê `TrustedStores.packageNames`: se lesse, tirar uma loja da produção só sumiria com o caso, sem falha. Tirar ou acrescentar uma loja derruba `default list has exactly the expected stores`.
- **Fixture:** `anInstalledApp(installerPackageName = ...)` do `testFixtures`. AC-006 usa também `requestedPermissions` e `services`, que a fixture já expõe.
- **Verificação de build (AC-013, AC-014):** feita localmente e desfeita sem commit, como na SPEC-011. Não há teste automatizado de configuração do Gradle no projeto.
- **Excluídos:** os dois caminhos de API (AC-010, já cobertos pela SPEC-011); `TrustedStores` sem teste próprio além da igualdade e da imutabilidade (é dado); fontes nos comentários (conferidas no review).

## Trade-offs & Alternatives Considered
- **Coletor no `core-domain`:** rejeitado pelo usuário. Separaria os coletores em dois módulos, e a §7 põe os coletores no `core-detection`. Custo aceito: o coletor fica fora do Kover.
- **Valor padrão no construtor (`= TrustedStores.packageNames`):** rejeitado. Segue o padrão 5: quem constrói (o Hilt, na SPEC-018, ou o banco) declara a lista. Um esquecimento não passa calado.
- **`TrustedStores` com tipo `TrustedStore(name, packageName)`:** rejeitado. O coletor só usa o pacote, e o nome no comentário basta para quem mantém a lista. O tipo com nome fica para a SPEC-015, onde o S9 compara nomes.
- **Lista num recurso (JSON ou `raw`):** rejeitado. Ler recurso Android exige `Context`, e um arquivo Kotlin compila, não tem erro de leitura em tempo de execução e é fácil de editar.
- **Tornar `unmodifiableCopy()` público no `core-domain`:** rejeitado. Mudaria `src/main` do domínio e a API pública do SDK por uma linha.
- **Parametrizar sobre `TrustedStores.packageNames`:** rejeitado. Explicado na estratégia de testes.
- **`detektTestFixtures` no `check`:** rejeitado. É experimental, usa resolução de tipos e teria configuração diferente da `detekt` dos outros diretórios.
- **Excluir as fixtures do Kover por classe (`DomainFixturesKt`):** rejeitado. Excluir o source set cobre fixtures futuras.

## Risks
- **`MagicNumber` vale no `testFixtures`** (o padrão do detekt só o desliga em `test`). Uma fixture futura com número literal pede uma constante nomeada. Sem mudança no `detekt.yml` agora (nada a corrigir).
- **Kover e detekt nas próximas versões:** a sintaxe `excludedSourceSets` e `source.from` foi verificada no Kover 0.9.11 e no detekt 1.23.8. Se uma atualização mudar a sintaxe, o build falha em vez de passar.
- **Dois artefatos do `core-domain` no teste do `core-detection`:** `testFixtures` depende do `main`, e o `core-detection` já recebe o `main` por `api`. O experimento compilou e passou; sem conflito de capacidade.
- **Pacotes com fonte de terceiros:** conferir em aparelho real quando houver (plano).
