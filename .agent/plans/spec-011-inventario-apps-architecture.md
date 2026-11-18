# Architecture: SPEC-011 Inventário de apps

> Entrada: `.agent/plans/spec-011-inventario-apps.md`, `.agent/specs/features/spec-011-inventario-apps.spec.toon` (FT-011 v1.1), `.agent/specs/acceptance/spec-011-inventario-apps.acceptance.toon` (v1.1)
> Contrato: `.agent/specs/contracts/spec-011-inventario-apps.contract.toon` (v1.1) · 2026-11-18

## Summary
O `AppInventory` do domínio ganha a primeira implementação, `PackageManagerAppInventory`, no pacote `io.github.valossa515.tapguard.core.detection.inventory`. Os ramos por versão do Android ficam em `PackageManagerVersionCompat.kt`, na raiz `core.detection`, como funções de extensão `internal`; é o único lugar do código de produção com `@Suppress("DEPRECATION")`.

A leitura roda num `withContext` com o dispatcher recebido pelo construtor, em duas etapas:
1. **Lista leve:** flags 0; saem os apps do sistema e o próprio pacote.
2. **Detalhe:** permissões, serviços (inclusive desligados) e instalador, app por app, com `ensureActive()` a cada um. App desinstalado no meio é pulado.

O `core-detection` passa a expor o `core-domain` e as corrotinas como `api`, porque os tipos deles aparecem na API pública. `QUERY_ALL_PACKAGES` entra só no manifesto do `app`.

| Pacote / arquivo | Conteúdo | Visibilidade |
|---|---|---|
| `core.detection` / `PackageManagerVersionCompat.kt` | `installedPackagesCompat`, `packageInfoCompat`, `installerPackageNameCompat`, `longVersionCodeCompat` | `internal` |
| `core.detection.inventory` / `PackageManagerAppInventory.kt` | `class PackageManagerAppInventory(context, ioDispatcher) : AppInventory` | público (classe e construtor); o resto `private` |

## Pattern Analysis
- **Padrões existentes no repositório:**
  1. **Adaptador com construtor e dependências explícitas:** `SettingsDataSource` (`core-data`) recebe o `DataStore` pelo construtor e não cria nada sozinho. O inventário recebe `Context` e `CoroutineDispatcher` do mesmo jeito, mas sem `@Inject`: o Hilt fica para a SPEC-018.
  2. **Pacotes por assunto espelhando o domínio:** o `core-domain` tem `inventory`, `signal`, `risk`, `scan` e `card`; o `core-data` separa `database`, `settings` e `di`. O inventário vai para `core.detection.inventory`, como o `core.domain.inventory`. Utilitário `internal` de uso geral fica na raiz, como `UnmodifiableCopies.kt` no `core-domain`.
  3. **Teste de manifesto com Robolectric e lista permitida:** `MergedManifestTest` lê `requestedPermissions` do próprio pacote e falha com qualquer permissão fora da lista. O `LibraryManifestTest` repete a forma no `core-detection`.
  4. **Constantes nomeadas e KDoc em português:** `DATABASE_NAME` e `DATASTORE_NAME` (`core-data`), `MIN_SCORE`/`MAX_SCORE` (`core-domain`). Flags combinadas viram `private const val` no `companion`.
- **Padrão seguido:** os quatro acima, mais `skills/android/SKILL.md` (adaptador `PackageManagerAppInventory` implementando porta do `core-domain`) e `skills/android/testing.md` (Adapter Tests com Robolectric, `@Config(sdk)` por ramo, nomes com crases).

## Layer Decisions

| Camada | Decisão | Justificativa |
|---|---|---|
| build | Catálogo: versão `coroutinesTest` renomeada para `coroutines`; nova biblioteca `kotlinx-coroutines-core`. `core-detection/build.gradle.kts`: `api(project(":core-domain"))` (era `implementation`), `api(libs.kotlinx.coroutines.core)`, `testImplementation` de `robolectric`, `androidx-test-core`, `kotlinx-coroutines-test`; `testOptions { unitTests.isIncludeAndroidResources = true }` | BR-015. `api` porque `AppInventory`, `InstalledApp` e `CoroutineDispatcher` aparecem na API pública. `isIncludeAndroidResources` faz o Robolectric ler o manifesto mesclado da biblioteca (experimento, item 4) |
| `core-domain` | Só o KDoc do `AppInventory`: devolve apps que não são do sistema e não devolve o próprio app; o cancelamento é lançado; erros da fonte de pacotes são propagados; app que some durante a leitura é pulado. Nenhum tipo do Android citado | BR-011, AC-014. O domínio continua puro |
| `core.detection` | `PackageManagerVersionCompat.kt`, funções `internal`: `PackageManager.installedPackagesCompat(flags: Int): List<PackageInfo>` (API 33+: `PackageInfoFlags.of(flags.toLong())`), com `@SuppressLint("QueryPermissionsNeeded")` e comentário; `PackageManager.packageInfoCompat(packageName, flags): PackageInfo` (API 33+: `PackageInfoFlags`; lança `NameNotFoundException`); `PackageManager.installerPackageNameCompat(packageName): String?` (API 30+: `getInstallSourceInfo().installingPackageName`; abaixo, `getInstallerPackageName` com `@Suppress("DEPRECATION")`; lança `NameNotFoundException`); `PackageInfo.longVersionCodeCompat: Long` (API 28+: `longVersionCode`; abaixo, `versionCode.toLong()` com `@Suppress("DEPRECATION")`). Cortes com `Build.VERSION.SDK_INT >= Build.VERSION_CODES.*` | BR-005 a BR-009, BR-014. Funções e não classe: sem estado, sem necessidade de troca em teste. Arquivo na raiz para os coletores das SPEC-013 e SPEC-016 reaproveitarem. `VERSION_CODES` satisfaz o `NewApi` do lint (experimento: "No issues found") |
| `core.detection.inventory` | `class PackageManagerAppInventory(context: Context, private val ioDispatcher: CoroutineDispatcher) : AppInventory`. Guarda `context.packageManager` e `context.packageName`, não o `Context`. `installedApps()` = `withContext(ioDispatcher) { installedPackagesCompat(0).filter(::isUserApp).mapNotNull { ensureActive(); readApp(it.packageName) } }`. `isUserApp`: `applicationInfo` não nulo, pacote diferente do próprio, `flags and SYSTEM_FLAGS == 0`. `readApp`: `try { … } catch (expected: NameNotFoundException) { null }` em volta do detalhe e do instalador; `applicationInfo` nulo no detalhe também pula. Conversão: `loadLabel(packageManager).toString()`, `longVersionCodeCompat`, `Instant.ofEpochMilli` nas duas datas, `requestedPermissions.orEmpty().toSet()`, `services.orEmpty().map { it.name }.toSet()`. `companion`: `private const val SYSTEM_FLAGS = FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP` e `DETAIL_FLAGS = GET_PERMISSIONS or GET_SERVICES or MATCH_DISABLED_COMPONENTS`. KDoc da classe: duas etapas, o que é pulado e o que é propagado (`NameNotFoundException` pulado; `RuntimeException` do sistema propagado), e que quem usa a biblioteca declara `QUERY_ALL_PACKAGES` | BR-001 a BR-004, BR-010 a BR-012. `ensureActive()` no começo de cada app interrompe antes do próximo detalhe. `expected` passa no `SwallowedException` do detekt (`allowedExceptionNameRegex`). Funções curtas para o `ReturnCount` (máximo 2) |
| `app` | Manifesto: `xmlns:tools` e `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission" />`, com comentário XML (listar os apps instalados é a função do app; declaração de app de segurança na Play Store; ADR-010 e ADR-016). `MergedManifestTest`: novo teste que exige `QUERY_ALL_PACKAGES` em `requestedPermissions`; lista permitida igual | BR-013. Erro de lint sem a supressão (experimento, item 3) |

**API pública do `core-detection` depois desta spec:** só `PackageManagerAppInventory` e o construtor dele. Nenhum tipo do Android vaza além do `Context`. O `explicitApi()` fica como ponto aberto no ADR-016.

## Dependencies
- **Reusing:** `AppInventory` e `InstalledApp` (`core-domain`, SPEC-010); `robolectric` 4.18, `androidx-test-core` 1.7.0 e `kotlinx-coroutines-test` 1.11.0 do catálogo (SPEC-003 e SPEC-004); a forma do `MergedManifestTest`.
- **New:** `org.jetbrains.kotlinx:kotlinx-coroutines-core` 1.11.0 (`api` no `core-detection`), para `CoroutineDispatcher`, `withContext` e `ensureActive`. Mesma versão do `kotlinx-coroutines-test` já usado no `core-data`. Sem `androidx.core`.

## Database Changes
Nenhuma.

## Test Strategy Rationale
- **Robolectric (JUnit 4) no `core-detection`**, com o `PackageManager` simulado: o código real de consulta roda contra as shadows (`testing.md`, Adapter Tests).
- **Uma classe, cinco APIs:** `PackageManagerAppInventoryTest` com `@Config(sdk = [26, 28, 30, 33, 36])`, para cada cenário passar por todos os ramos. A 26 cobre os ramos antigos; a 28, a 30 e a 33 são o primeiro nível de cada ramo novo; a 36 é o targetSdk.
- **Fixture local** `aPackageInfo(...)` no próprio arquivo de teste, com só os parâmetros que os testes trocam. O `versionCode` abaixo da API 28 é gravado num ramo de `SDK_INT` com `@Suppress("DEPRECATION")`, a única supressão em teste (exceção do ADR-016). Instalador: `setInstallerPackageName` abaixo da API 30; `ShadowPackageManager.setInstallSourceInfo` a partir dela, registrado para todo app de usuário, porque o Robolectric lança `NameNotFoundException` para pacote sem origem.
- **Ganchos:** um `CharSequence` de rótulo que conta leituras e executa uma ação na segunda. A primeira leitura acontece quando o Robolectric copia (Parcel) a lista leve; a segunda, quando copia o detalhe daquele app. O mesmo gancho cancela o job (cancelamento) ou remove o próximo pacote (desinstalação no meio). O KDoc explica isso e diz que, se o Robolectric parar de copiar, os testes falham em vez de passar (verificado sem `ensureActive()`: `[2, 2, 2]`).
- **Dispatcher (emenda 1.1):** o inventário recebe um `CountingDispatcher` local que envolve o `StandardTestDispatcher(testScheduler)` do próprio `runTest` e conta os despachos. O teste confere o resultado e que houve pelo menos um despacho. Trocar, na produção, o dispatcher injetado por `Dispatchers.Unconfined` faz o teste falhar.
- **`LibraryManifestTest`** (Robolectric, API 36): `requestedPermissions` do próprio pacote, que com `isIncludeAndroidResources` reflete o manifesto mesclado da biblioteca, não pode ter nada além de `REORDER_TASKS` (do `androidx.test:core`, só em teste).
- **Excluídos:** `PackageManagerVersionCompat.kt` sem teste direto (coberto pelo inventário nas cinco APIs); visibilidade de pacotes da API 30, `GET_PERMISSIONS`, limite de transferência e erro do `PackageManager` (o Robolectric não reproduz; conferidos no review e no teste ponta a ponta).
- **Removido:** `core-detection/src/test/.../CoreDetectionSmokeTest.kt`, registrado no cabeçalho do contrato (o schema só aceita `create` e `modify`).

## Trade-offs & Alternatives Considered
- **`QUERY_ALL_PACKAGES` no manifesto da biblioteca:** rejeitado; todo app que usasse o SDK herdaria uma permissão restrita da Play Store.
- **Uma chamada com `GET_PERMISSIONS | GET_SERVICES` para todos os pacotes:** rejeitado; carrega dados dos apps do sistema, que são descartados, e põe todos os detalhes numa resposta.
- **`androidx.core` (`PackageInfoCompat`):** rejeitado; dependência a mais no SDK que resolveria só o `versionCode`.
- **`lint.xml` por módulo:** rejeitado; supressão no elemento, com motivo ao lado.
- **Ponto de troca `internal` no construtor só para teste:** rejeitado; acrescentaria um tipo de produção que só existe para teste. Os ganchos do Robolectric falham em vez de passar se o mecanismo mudar.
- **Classe de compatibilidade em vez de funções:** rejeitado; sem estado e sem troca em teste, funções de extensão bastam.
- **Dispatcher com padrão `Dispatchers.IO` no construtor:** rejeitado; um teste que esquecesse de passar o dispatcher rodaria em thread real.

## Risks
- **Ganchos dependem da cópia por Parcel do Robolectric 4.18:** mitigado pelo modo de falha (o teste quebra, não passa).
- **Teste do dispatcher (materializado e resolvido na emenda 1.1):** a forma planejada, com um `StandardTestDispatcher` sobre um segundo `TestCoroutineScheduler`, falhou nas cinco APIs: o `runTest` recusa um segundo scheduler (`IllegalStateException: Detected use of different schedulers`), e o `advanceUntilIdle()` num scheduler avulso exige `@OptIn(ExperimentalCoroutinesApi::class)`, que geraria aviso novo. O usuário aprovou o `CountingDispatcher` (2026-11-18).
- **`@SuppressLint` em função de extensão de topo:** no experimento a anotação estava num método de classe. Se o lint não a respeitar na função de topo, o builder para e pergunta.
- **`const val` com `or` de constantes do Android:** deve compilar como expressão constante; se não compilar, o builder usa `private val` no `companion` e avisa.
