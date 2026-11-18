# Test Plan: SPEC-011 Inventário de apps

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-011-inventario-apps.contract.toon` (versão 1.1) e critérios de `.agent/specs/acceptance/spec-011-inventario-apps.acceptance.toon` (versão 1.1) · 2026-11-18
> Um único desvio da estratégia original, aprovado como emenda 1.1: o cenário do dispatcher usa um `CountingDispatcher`. São 3 alvos e 17 cenários (15 + 1 + 1), um método de teste por cenário. O inventário roda em 5 APIs: 75 execuções.

## Scope
- **Inventário:** o `PackageManagerAppInventory` contra o `PackageManager` do Robolectric, com os filtros da regra 1 e do próprio pacote, o preenchimento dos campos, os ramos por versão (código de versão, instalador, flags), os serviços e apps desligados, a desinstalação no meio, o cancelamento e o dispatcher.
- **Manifestos:** o manifesto mesclado da biblioteca não pode declarar permissões, e o do `app` tem de declarar `QUERY_ALL_PACKAGES`.
- **Onde roda:** tudo em JUnit 4 com Robolectric, em `src/test`, no `./gradlew build`. O `PackageManagerVersionCompat.kt` não tem teste próprio: é coberto pelo inventário nas cinco APIs.

## Padrões seguidos (pattern analysis)
- **Testes existentes:**
  - `MergedManifestTest` (`app`): Robolectric, `@Config(sdk = [36])`, `ApplicationProvider`, lista permitida e mensagem com o que sobrou;
  - `SettingsDataSourceTest` (`core-data`): `runTest`, uma classe por unidade, constantes num `private companion object`;
  - os testes do `core-domain`: nomes em frase com crases e fixtures como funções com argumentos padrão.
- `skills/android/testing.md` › Adapter Tests — Robolectric (shadows do `PackageManager`, `@Config(sdk)` por ramo) e Test Fixtures (dados sintéticos `com.example...`).
- Asserções do JUnit 4 (`assertEquals`, `assertTrue`, `assertNull`); `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`). Nenhuma biblioteca nova.

## Integration tests (Robolectric)

### `PackageManagerAppInventory` — `core-detection/.../inventory/PackageManagerAppInventoryTest.kt`, `@Config(sdk = [26, 28, 30, 33, 36])`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | returns-user-app-and-skips-app-with-flag-system | só o app com flags 0 | AC-001 |
| 2 | skips-updated-system-app-with-both-flags | app com `FLAG_SYSTEM \| FLAG_UPDATED_SYSTEM_APP` fora | AC-002 |
| 3 | skips-app-with-only-flag-updated-system-app | app só com `FLAG_UPDATED_SYSTEM_APP` fora | AC-003 |
| 4 | skips-own-package | o próprio pacote, que o Robolectric registra (pré-condição conferida), fica fora | AC-004 |
| 5 | fills-label-permissions-services-dates-and-installer | rótulo, permissões, serviço, datas (`Instant` dos milissegundos) e instalador iguais aos informados | AC-005 |
| 6 | keeps-long-version-code-from-api-28-and-int-version-code-on-api-26 | 5.000.000.000 a partir da API 28; 7 na 26 | AC-006 |
| 7 | reads-installer-from-install-source-info-on-api-30-and-installer-package-name-below | cada app com o instalador registrado pelo caminho da API | AC-007 |
| 8 | returns-null-installer-when-none-is-registered | `installerPackageName` nulo e o app devolvido | AC-008 |
| 9 | includes-disabled-services | serviço ligado e serviço desligado em `services` | AC-009 |
| 10 | returns-empty-sets-without-permissions-or-services | conjuntos vazios | AC-010 |
| 11 | includes-disabled-app | app com `applicationInfo.enabled = false` devolvido | AC-011 |
| 12 | skips-app-uninstalled-between-list-and-detail | o segundo app, removido durante o detalhe do primeiro, fica fora; o primeiro e o terceiro voltam | AC-012 |
| 13 | cancellation-stops-before-next-app | `CancellationException`; leituras dos rótulos `[2, 1, 1]` (o detalhe do segundo e do terceiro não é lido) | AC-013 |
| 14 | read-runs-on-injected-dispatcher | resultado correto e pelo menos um despacho no `CountingDispatcher` (emenda 1.1) | AC-015 |
| 15 | returns-empty-list-without-user-apps | lista vazia só com app do sistema e o próprio pacote | AC-001 |

### Manifesto da biblioteca — `core-detection/.../LibraryManifestTest.kt`, `@Config(sdk = [36])`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | library-requests-no-permission-besides-reorder-tasks-from-androidx-test | nenhuma permissão além de `REORDER_TASKS` (do `androidx.test:core`) | AC-017 |

### Manifesto do app — `app/.../MergedManifestTest.kt`
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | query-all-packages-is-requested | `QUERY_ALL_PACKAGES` em `requestedPermissions` | AC-016 |

## Test data and infrastructure
- **Fixtures**, locais no `companion object` do teste do inventário, com dados sintéticos (`com.example...`):
  - `aPackageInfo(packageName, flags, label)`;
  - `aService(packageName, name, enabled)`;
  - `PackageInfo.withVersionCode(value)`: abaixo da API 28 grava o `versionCode` com `@Suppress("DEPRECATION")`, a única supressão em teste (ADR-016).
  - Os demais campos são definidos com `apply` no próprio teste, para a fixture ficar abaixo do limite da `LongParameterList`.
- **Instalador:** `install(info, installer)` registra o instalador pelo caminho da API: `setInstallerPackageName` abaixo da 30 e `ShadowPackageManager.setInstallSourceInfo` a partir dela. O registro vale para todo app de usuário, porque o Robolectric lança `NameNotFoundException` para pacote sem origem.
- **`SecondReadHook`:** um rótulo `CharSequence` que age na segunda leitura, ou seja, na cópia (Parcel) do detalhe daquele app. Se o Robolectric parar de copiar, os testes falham em vez de passar; o KDoc explica isso.
- **`CountingDispatcher`:** repassa ao `StandardTestDispatcher(testScheduler)` do `runTest` e conta os despachos. Não se cria um segundo `TestCoroutineScheduler`, porque o `runTest` o recusa.
- **Opção de build:** `isIncludeAndroidResources = true` no `core-detection`, para o Robolectric ler o manifesto mesclado da biblioteca. Com ela, o pacote do teste vira `io.github.valossa515.tapguard.core.detection.test`, e o filtro do próprio pacote o exclui.

## Não testado aqui (contrato › excluded)
- **Ramo `applicationInfo` nulo (emenda 1.3):** o Robolectric preenche o `applicationInfo` sozinho ao instalar um pacote sem ele (APIs 26 e 36). Conferido no review.
- **Visibilidade de pacotes (API 30), `GET_PERMISSIONS` e erro do `PackageManager`:** o Robolectric não reproduz nenhum dos três. Ficam para o review, o KDoc e o teste ponta a ponta.
- **Limite de transferência e custo das chamadas:** sem binder no Robolectric; desempenho na SPEC-018.

## Verificação local (desfeita sem commit)
- Tirar `FLAG_UPDATED_SYSTEM_APP` de `SYSTEM_FLAGS` → falha o cenário 3 (o cenário 2 também tem `FLAG_SYSTEM` e continua filtrado).
- Tirar `ensureActive()` → falha o cenário 13.
- Tirar `QUERY_ALL_PACKAGES` do manifesto do `app` → falha o `MergedManifestTest`.
- Declarar `QUERY_ALL_PACKAGES` no manifesto do `core-detection` → falha o `LibraryManifestTest`.
- Dispatcher injetado trocado por `Dispatchers.Unconfined` na produção → falha o cenário 14 (já verificado em 2026-11-18).
