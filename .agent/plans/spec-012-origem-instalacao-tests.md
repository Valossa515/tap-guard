# Test Plan: SPEC-012 Origem da instalação (S1)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-012-origem-instalacao.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-012-origem-instalacao.acceptance.toon` (versão 1.0) · 2026-11-18
> São 3 alvos e 10 cenários (1 + 1 + 8), um método de teste por cenário. Os dois parametrizados rodam 11 e 8 casos: 27 execuções no total.

## Scope
- **Coletor de S1:** o `InstallSourceCollector` com a lista padrão (`TrustedStores.packageNames`) e com conjuntos montados pelo SDK. Cobre instalador nulo, fora da lista, instalador do sistema, comparação exata, só S1, lista maior, lista menor e cópia do conjunto recebido.
- **Lista padrão:** exatamente as 11 entradas esperadas, e não pode ser alterada por cast.
- **Onde roda:** JUnit 4 na JVM, no `core-detection/src/test`, sem Robolectric e sem `@Config(sdk)`: o coletor não toca em API do Android. Roda no `./gradlew build`.
- **Fora:** os dois caminhos de API do instalador (AC-010), cobertos pelo `PackageManagerAppInventoryTest` da SPEC-011. A configuração do detekt e do Kover no `testFixtures` (AC-013, AC-014) é conferida localmente, sem teste automatizado.

## Padrões seguidos (pattern analysis)
- **`PackageManagerAppInventoryTest` (`core-detection`):** JUnit 4, `runTest` para função `suspend`, asserções do JUnit 4, constantes num `private companion object`, nomes em frase com crases.
- **`InstalledAppTest` e `ScanReportTest` (`core-domain`):** fixtures do domínio (`anInstalledApp`), coleção que recusa alteração conferida por cast para tipo mutável.
- **Sonda da SPEC-012 (experimento do plano):** `@RunWith(Parameterized::class)` com o parâmetro no construtor e `@Parameters(name = "{0}")` num `companion object` com `@JvmStatic`. Passou no ktlint e no detekt.
- `skills/android/testing.md` › Which runner where (JUnit 4 em módulo Android), Test Fixtures (do `testFixtures` do `core-domain`, dados sintéticos `com.example...`), Naming Convention for Tests.

## Unit tests (JUnit 4)

### Lojas confiáveis — `core-detection/.../signal/InstallSourceCollectorTrustedStoreTest.kt` (parametrizado, 11 casos)
| # | Scenario | Parameters | Expected result | AC |
|---|----------|------------|-----------------|----|
| 1 | store-installer-yields-no-signal | cada pacote de `EXPECTED_TRUSTED_STORES` | conjunto vazio | AC-001 |

### Instaladores fora da lista — `core-detection/.../signal/InstallSourceCollectorUntrustedInstallerTest.kt` (parametrizado, 8 casos)
| # | Scenario | Parameters | Expected result | AC |
|---|----------|------------|-----------------|----|
| 1 | untrusted-installer-yields-s1 | `com.example.store`, `org.fdroid.fdroid`, `com.aurora.store`, `com.sec.android.easyMover`, `com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.miui.packageinstaller`, `com.android.shell` | `{UNTRUSTED_INSTALL_SOURCE}` | AC-003, AC-004 |

### Coletor e lista padrão — `core-detection/.../signal/InstallSourceCollectorTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | null-installer-yields-s1 | `{UNTRUSTED_INSTALL_SOURCE}` | AC-002 |
| 2 | uppercase-store-package-yields-s1 | `COM.ANDROID.VENDING` → `{UNTRUSTED_INSTALL_SOURCE}` | AC-005 |
| 3 | yields-only-s1-for-app-with-other-risk-traits | app de fora com permissão de NFC e serviço declarado → exatamente `{UNTRUSTED_INSTALL_SOURCE}` | AC-006 |
| 4 | extended-list-trusts-extra-store-and-keeps-default | padrão + `com.example.bankstore`: o extra e `com.android.vending` sem sinal | AC-007 |
| 5 | smaller-list-flags-removed-store | padrão − `com.amazon.venezia`: `com.amazon.venezia` → `{UNTRUSTED_INSTALL_SOURCE}` | AC-008 |
| 6 | later-change-to-given-set-does-not-affect-collector | pacote retirado do `MutableSet` depois da construção continua confiável | AC-009 |
| 7 | default-list-has-exactly-the-expected-stores | `TrustedStores.packageNames` igual a `EXPECTED_TRUSTED_STORES` | AC-011 |
| 8 | default-list-rejects-mutation-through-cast | `add` por cast para `MutableSet` lança `UnsupportedOperationException` | AC-011 |

## Edge cases
- Instalador nulo (cenário 1) e comparação com maiúsculas (cenário 2).
- Lista vazia: documentada no KDoc (todo app recebe S1). Não há teste: a especificação não define validação, e o comportamento sai do mesmo `in`.

## Test data & fixtures
- **`ExpectedTrustedStores.kt`** (`core-detection/src/test/.../signal`): `internal val EXPECTED_TRUSTED_STORES`, com os 11 pacotes escritos à mão. Não lê `TrustedStores`: tirar uma loja da produção derruba o caso dela e o cenário 7.
- **`anInstalledApp(installerPackageName = ...)`**, do `testFixtures` do `core-domain`. O cenário 3 usa também `requestedPermissions` e `services`.
- Pacotes sintéticos (`com.example.store`, `com.example.bankstore`) e pacotes reais só de lojas e instaladores públicos.

## Infrastructure
- Nenhuma biblioteca nova: `junit4` e `kotlinx-coroutines-test` já estão no `testImplementation` do `core-detection`, e o `testFixtures(project(":core-domain"))` veio na tarefa 1.
