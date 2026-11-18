# Plan: SPEC-011 Inventário de apps

> Issue #7 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-010 (#6, fechada) · Especificação RF-101, §5 (regra 1), §7 e §8 · ADR-010, ADR-015
> Spec: `.agent/specs/features/spec-011-inventario-apps.spec.toon` (FT-011, `interface: library`) · Aceite: `.agent/specs/acceptance/spec-011-inventario-apps.acceptance.toon`
> Arquitetura: `.agent/plans/spec-011-inventario-apps-architecture.md` · Contrato: `.agent/specs/contracts/spec-011-inventario-apps.contract.toon`
> Branch: `spec-011-inventario-apps` · Planejado em 2026-11-18

## Summary
Implementar o `AppInventory` do `core-domain` no `core-detection`, sobre o `PackageManager`. Ele devolve um `InstalledApp` para cada app de usuário, com rótulo, código de versão longo, instalador, datas de instalação e atualização, permissões pedidas e serviços declarados, inclusive os desligados. Ficam de fora os apps do sistema (regra 1 da §5, com teste aqui, como combinado na SPEC-010) e o pacote do próprio app.

A permissão `QUERY_ALL_PACKAGES` é declarada só no manifesto do `app`. A biblioteca não declara permissão nenhuma, porque o módulo vai virar SDK para bancos e quem o usar herdaria a permissão. Os testes usam Robolectric, uma classe que roda em cinco níveis de API.

## Scope
- **Módulos afetados:**
  - `core-detection`: implementação e testes;
  - `app`: manifesto e `MergedManifestTest`;
  - `core-domain`: só o KDoc do `AppInventory`;
  - documentação.
- **Novos arquivos (esperados):**
  - `PackageManagerAppInventory.kt` em `core-detection/src/main/kotlin/io/github/valossa515/tapguard/core/detection/inventory/`;
  - `PackageManagerVersionCompat.kt` (funções `internal` com os ramos por versão do Android) na raiz `core-detection/src/main/kotlin/io/github/valossa515/tapguard/core/detection/`, para os coletores das próximas specs reaproveitarem;
  - testes Robolectric em `core-detection/src/test/...`: `PackageManagerAppInventoryTest` com uma fixture local de `PackageInfo`, e `LibraryManifestTest` (manifesto da biblioteca sem permissões).
- **Arquivos modificados:**
  - `gradle/libs.versions.toml`;
  - `core-detection/build.gradle.kts`;
  - `app/src/main/AndroidManifest.xml`;
  - `app/src/test/.../MergedManifestTest.kt`;
  - `core-domain/.../inventory/AppInventory.kt` (só o KDoc);
  - `core-detection/README.md`;
  - `docs/especificacao.md` (RF-101);
  - `.agent/SKILLS.md`, `.agent/decisions.md` (ADR-016), `.agent/project-reference.md`, `.agent/conventions.md`, `.agent/context.md`;
  - este plano (marcar as tarefas).
- **Arquivo removido:** `core-detection/src/test/.../CoreDetectionSmokeTest.kt`, substituído pelos testes reais.
- **Fora do escopo:**
  - os coletores de sinais (SPEC-012 a SPEC-016);
  - a comparação do instalador com a lista de lojas (SPEC-012);
  - a ligação no Hilt e o qualificador do dispatcher de I/O (SPEC-018);
  - a medição de 300 apps em 5 s (SPEC-018);
  - fixtures do domínio compartilhadas entre módulos (`testFixtures`): ficam para a SPEC-012, com teste prévio no projeto descartável.

## Decisões já tomadas (usuário, 2026-11-18)

| Item | Decisão |
|------|---------|
| Instalador | O inventário lê o instalador. Na API 30 ou maior usa `getInstallSourceInfo(...).installingPackageName`; abaixo, `getInstallerPackageName(...)`. Os dois caminhos são testados aqui. A SPEC-012 fica com a comparação com a lista de lojas. Comentário publicado na issue #8 (2026-11-18), registrando também que `initiatingPackageName` pode virar sinal no futuro, fora do MVP |
| Regra 1 | Ignorar app com `FLAG_SYSTEM` **ou** `FLAG_UPDATED_SYSTEM_APP`, com um teste para cada caso (só `FLAG_SYSTEM`, as duas flags, só `FLAG_UPDATED_SYSTEM_APP`) |
| Próprio app | O pacote do próprio app (`context.packageName`) fica fora do inventário, com teste. Vale também para o SDK: o app do banco não avalia a si mesmo. Resolve ainda o pacote de teste que o Robolectric registra. Vai para o ADR-016 e para o RF-101 em `docs/especificacao.md` |
| Serviços | Inclui os serviços desligados (`MATCH_DISABLED_COMPONENTS`) |
| Busca | Duas etapas: (1) lista leve com flags 0, onde rodam os filtros de sistema e de próprio pacote; (2) `getPackageInfo(pacote, GET_PERMISSIONS \| GET_SERVICES \| MATCH_DISABLED_COMPONENTS)` e o instalador, app por app. Cada chamada carrega um só app, e permissões e serviços dos apps do sistema nunca são pedidos |
| Cancelamento | `ensureActive()` a cada app no laço, com teste de que o cancelamento interrompe a leitura |
| Falhas | App desinstalado entre a lista e o detalhe (`NameNotFoundException` no detalhe ou no instalador) é pulado. Qualquer outro erro do `PackageManager` é propagado. O KDoc do `AppInventory` documenta o que pode ser lançado |
| Dispatcher | Leitura num `withContext` com um `CoroutineDispatcher` recebido pelo construtor. Nos testes, `StandardTestDispatcher(testScheduler)` |
| Dependências | `kotlinx-coroutines-core` 1.11.0 no `core-detection`; no catálogo, a versão `coroutinesTest` passa a se chamar `coroutines` e serve aos dois artefatos. `testImplementation`: `robolectric`, `androidx-test-core`, `kotlinx-coroutines-test` (já no catálogo). **Sem `androidx.core`** |
| `api` × `implementation` (revisão do architect, 2026-11-18) | No `core-detection`: `api(project(":core-domain"))` (supertipo `AppInventory` e `InstalledApp` na API pública) e `api(libs.kotlinx.coroutines.core)` (`CoroutineDispatcher` no construtor público). Regra no `SKILLS.md`: em módulo de biblioteca, dependência cujo tipo aparece na API pública é `api`; o resto é `implementation` |
| Nomes e pacotes (revisão do architect) | `PackageManagerAppInventory` em `core.detection.inventory`. Ramos por versão em `PackageManagerVersionCompat.kt` na raiz `core.detection` (não `PackageManagerCompat.kt`, que coincide com `androidx.core.content.pm.PackageManagerCompat`), como funções de extensão `internal` |
| API pública (revisão do architect) | Público só o `PackageManagerAppInventory` e o construtor `(context: Context, ioDispatcher: CoroutineDispatcher)`. O construtor guarda só o `packageManager` e o `packageName`, não o `Context` (uma Activity passada pelo app do banco não vaza). O `explicitApi()` fica como ponto aberto no ADR-016, para quando o SDK for publicado |
| KDoc (revisão do architect) | O KDoc do `AppInventory` (domínio puro) não cita tipos do Android: diz que erros da fonte de pacotes são propagados, que o cancelamento é lançado e que app que some durante a leitura é pulado. O detalhe (`NameNotFoundException`, `RuntimeException` do sistema, flags, permissão no manifesto de quem usa) vai no KDoc do `PackageManagerAppInventory` |
| Ganchos de teste (revisão do architect) | Cancelamento e desinstalação no meio são testados com um rótulo `CharSequence` que age na segunda leitura (cópia do detalhe pelo Robolectric). O KDoc do gancho explica o mecanismo e diz que uma mudança no Robolectric faz o teste falhar, não passar. Sem ponto de troca de produção só para teste |
| Teste da biblioteca e detekt (revisão do architect) | `LibraryManifestTest` no padrão do `MergedManifestTest`; o pacote de teste vira `...core.detection.test` com `isIncludeAndroidResources`, coberto pelo filtro do próprio pacote. `catch` de `NameNotFoundException` com a exceção chamada `expected` (aceito pela `SwallowedException`) e funções curtas (`ReturnCount`), sem supressão |
| Hilt | Fica para a SPEC-018. Aqui a classe tem construtor comum, sem anotações |
| `@Suppress("DEPRECATION")` | Segunda exceção de alcance fechado, ao lado da `LongParameterList`: só no ramo antigo de um teste de `SDK_INT` cujo outro ramo usa a API nova. No código de produção, só no arquivo `internal` de compatibilidade (hoje `versionCode` abaixo da API 28 e `getInstallerPackageName` abaixo da 30). Registrada no `SKILLS.md`, em `conventions.md` e no ADR-016 |
| Lint | `tools:ignore="QueryAllPackagesPermission"` no elemento `<uses-permission>` do `app` e `@SuppressLint("QueryPermissionsNeeded")` no método que lista os pacotes. Cada uma com comentário explicando o motivo. Sem `lint.xml` |
| `QUERY_ALL_PACKAGES` | Só no manifesto do `app`. O `MergedManifestTest` passa a exigir que ela esteja declarada (a lista permitida não muda). Um teste novo no `core-detection` confere que o manifesto da biblioteca, mesclado com as dependências, não declara permissão nenhuma. A única exceção é `REORDER_TASKS`, que vem do `androidx.test:core` e só existe em teste. O README do módulo diz que quem usar o SDK declara a permissão |
| Fixtures | Esta spec não compartilha as fixtures do domínio: os testes montam `PackageInfo` com uma fixture local |

### Experimento (projeto descartável `%TEMP%\tap-guard-spec011-probe-20261118`)
Cópia do repositório com uma implementação provisória e testes Robolectric 4.18 nas APIs 26, 28, 30, 33 e 36.
1. **Filtros e campos:** nas cinco APIs, os apps com `FLAG_SYSTEM`, com as duas flags e só com `FLAG_UPDATED_SYSTEM_APP` ficam de fora. O app de usuário volta com rótulo, permissões, serviços, datas e instalador (`setInstallerPackageName` abaixo da API 30, `ShadowPackageManager.setInstallSourceInfo` a partir dela). `longVersionCode` de 5.000.000.000 é preservado a partir da API 28.
2. **Avisos do compilador (compileSdk 37):** `PackageInfo.versionCode` e `getInstallerPackageName` geram aviso de depreciação; `getInstalledPackages(int)` e `getPackageInfo(String, int)`, não. A fixture de teste que grava `versionCode` para a API 26 também gera aviso. `@Suppress("DEPRECATION")` na expressão elimina todos.
3. **Lint:** declarar `QUERY_ALL_PACKAGES` no `app` gera **erro** (`QueryAllPackagesPermission`) e derruba o build. `getInstalledPackages` no `core-detection` gera aviso (`QueryPermissionsNeeded`). As duas supressões no próprio elemento resolvem, e o `MergedManifestTest` continua passando.
4. **Manifesto da biblioteca:** com `unitTests.isIncludeAndroidResources = true` no `core-detection`, o Robolectric lê o manifesto mesclado da biblioteca. Uma permissão declarada nele aparece em `requestedPermissions` do próprio pacote, junto com `REORDER_TASKS`, que vem do `androidx.test:core` 1.7.0 (conferido no AAR). Sem essa opção, não aparece nada. Com ela, o pacote do teste vira `io.github.valossa515.tapguard.core.detection.test`.
5. **Serviços desligados:** sem `MATCH_DISABLED_COMPONENTS`, os serviços desligados não aparecem; com a flag, aparecem (APIs 26 e 36).
6. **App inteiro desligado:** aparece na lista leve com flags 0 nas APIs 26, 30 e 36, nos três casos testados (`applicationInfo.enabled = false`, `COMPONENT_ENABLED_STATE_DISABLED_USER` e `DISABLED_UNTIL_USED`). Nenhuma flag a mais é necessária.
7. **Cancelamento a cada app:** um rótulo de teste (`CharSequence`) cancela o job na segunda leitura. A primeira leitura acontece quando o Robolectric copia a lista leve; a segunda, quando copia o detalhe do primeiro app. Com `ensureActive()`, os rótulos ficam lidos `[2, 1, 1]` e a chamada termina com `CancellationException`. Sem `ensureActive()`, ficam `[2, 2, 2]`: o teste pega a falta da verificação.
8. **Desinstalado no meio:** o mesmo gancho remove o segundo app durante o detalhe do primeiro. O resultado tem o primeiro e o terceiro, nas APIs 26, 30 e 36.
9. **Detekt** (configuração padrão no `core-detection`): `SwallowedException` aponta `catch (e: NameNotFoundException)` sem uso. O nome `expected` está no padrão aceito pela regra (`allowedExceptionNameRegex`), então não é preciso suprimir. `ReturnCount` é uma questão de formato do código.
10. **Limitações do Robolectric:**
    - devolve `requestedPermissions` mesmo sem `GET_PERMISSIONS`, então o teste não prova essa flag (a `GET_SERVICES` é provada);
    - não aplica a visibilidade de pacotes da API 30, então o teste não prova a necessidade da permissão;
    - não tem binder, então o limite de transferência não é testável.

## Approach
- Seguir `skills/android/SKILL.md`: adaptador sobre API de plataforma no `core-<capacidade>`, implementando uma porta do `core-domain`, com o nome `PackageManagerAppInventory` da tabela de nomes. Seguir também `skills/android/testing.md`: Adapter Tests com Robolectric, um ramo por API com `@Config(sdk)`, nomes com crases.
- **Ramos por versão**, todos no arquivo `internal` de compatibilidade:

  | Chamada | Abaixo do corte | A partir do corte |
  |---|---|---|
  | Código de versão (corte: API 28) | `versionCode` (depreciado, com `@Suppress`) | `longVersionCode` |
  | Instalador (corte: API 30) | `getInstallerPackageName` (depreciado, com `@Suppress`) | `getInstallSourceInfo().installingPackageName` |
  | Flags do `PackageManager` (corte: API 33) | `getInstalledPackages(int)` e `getPackageInfo(String, int)` | `PackageInfoFlags.of(...)` |

- **Testes por versão:** a classe do inventário roda inteira com `@Config(sdk = [26, 28, 30, 33, 36])`. A API 26 cobre todos os ramos antigos; a 28, a 30 e a 33 são o primeiro nível de cada ramo novo; a 36 é o targetSdk. Os limites 27, 29 e 32 ficam de fora, porque cada nível a mais é mais uma imagem baixada no CI. No corte da 33 o resultado é igual nos dois ramos, e o teste confere só o resultado.
- **Datas:** `firstInstallTime` e `lastUpdateTime` (milissegundos) viram `Instant`. `requestedPermissions` e `services` nulos viram conjuntos vazios.
- **Visibilidade:** a partir da API 30, sem `QUERY_ALL_PACKAGES`, o Android devolve só os pacotes visíveis. Isso é responsabilidade do manifesto de quem usa a biblioteca, documentado no README.

## Tasks
1. [ ] **Dependências** (`chore:`):
   - `kotlinx-coroutines-core` no catálogo, com a versão renomeada para `coroutines`;
   - no `core-detection`, `api(project(":core-domain"))` (era `implementation`), `api(libs.kotlinx.coroutines.core)` e as `testImplementation`, mais `testOptions.unitTests.isIncludeAndroidResources = true`;
   - `./gradlew build` verde.
2. [ ] **Inventário** (`feat:`): `PackageManagerVersionCompat.kt` com os ramos por versão e as supressões, e o `PackageManagerAppInventory`: duas etapas, filtros de sistema e de próprio pacote, `ensureActive()` a cada app, desinstalado no meio pulado, dispatcher injetado. Mais o KDoc do `AppInventory` (sem tipos do Android) com o que pode ser lançado.
3. [ ] **Permissão no `app`** (`feat:`): `QUERY_ALL_PACKAGES` no manifesto, com `tools:ignore="QueryAllPackagesPermission"` e comentário.
4. [ ] **Testes** (`test:`, papel tester):
   - o inventário nas cinco APIs: regra 1 com três casos, próprio pacote, campos, código de versão longo, instalador nos dois caminhos e ausente, serviços desligados, app desligado, conjuntos vazios, desinstalado no meio, cancelamento;
   - manifesto da biblioteca sem permissões;
   - `MergedManifestTest` exigindo `QUERY_ALL_PACKAGES`;
   - remoção do `CoreDetectionSmokeTest`.
5. [ ] **Documentação** (`docs:`):
   - `core-detection/README.md`: estado, permissão a declarar por quem usa o SDK, dependências, e o compileSdk 37 no lugar do 36 que ficou da SPEC-001;
   - `docs/especificacao.md`: RF-101 passa a dizer que o próprio app fica fora;
   - `project-reference.md`: classe do inventário e dependências;
   - `conventions.md`: exceção do `@Suppress("DEPRECATION")`, supressões de lint no elemento, ramos por versão num arquivo só;
   - `context.md`;
   - `SKILLS.md`: as duas exceções juntas, `QUERY_ALL_PACKAGES` só no `app`, o que o `MergedManifestTest` e o `LibraryManifestTest` conferem, regra 1 implementada, regra `api` × `implementation` em módulo de biblioteca;
   - **ADR-016** em `decisions.md`: permissão fora da biblioteca, regra 1 com as duas flags, exclusão do próprio app, busca em duas etapas, instalador no inventário, supressões, `api` para tipos da API pública; ponto aberto: `explicitApi()` quando o SDK for publicado.
6. [ ] **Conferência dos critérios**, localmente e desfeita sem commit:
   - tirar o filtro de `FLAG_UPDATED_SYSTEM_APP` derruba o teste correspondente;
   - tirar `ensureActive()` derruba o teste de cancelamento;
   - declarar `QUERY_ALL_PACKAGES` no manifesto do `core-detection` derruba o teste da biblioteca;
   - tirar a permissão do `app` derruba o `MergedManifestTest`;
   - `./gradlew build` verde sem aviso novo do compilador nem do lint.

## Database changes
Nenhuma. `installerPackageName` e o código de versão chegam à `app_snapshot` pela SPEC-018.

## Risks & open questions
- **Teste do cancelamento e da desinstalação depende de como o Robolectric copia o `PackageInfo`** (Parcel). Se uma versão futura parar de copiar, os dois testes **falham** (não passam por engano: verificado sem `ensureActive()`). O KDoc do gancho no teste explica o mecanismo.
- **App desligado na lista leve:** verificado só no Robolectric. Em aparelho, a confirmação vem do teste ponta a ponta com o `decoy-app`.
- **Limite de transferência entre processos:** não testável sem aparelho. O AOSP manda a lista em pedaços (`ParceledListSlice`), e as duas etapas limitam cada chamada a um app.
- **Custo das N+1 chamadas:** medido na SPEC-018 (300 apps simulados em 5 s) e em aparelho no teste ponta a ponta.
- **CI mais lento:** quatro imagens novas do Robolectric (26, 28, 30, 33) baixadas no primeiro build.
- **`PackageInfo.applicationInfo` nulo** (anotado como anulável no SDK recente): o app é pulado, porque não dá para saber se é do sistema. Na prática o Android sempre o preenche na lista.
- **Ordem da lista:** a especificação não define, e o inventário não garante ordem.

## Acceptance criteria
- [ ] Teste com Robolectric devolve só apps de usuário: sistema, sistema atualizado, só `FLAG_UPDATED_SYSTEM_APP` e próprio pacote ficam fora (AC-001 a AC-004)
- [ ] Permissões, serviços (inclusive desligados), datas, rótulo, código de versão longo e instalador preenchidos nas cinco APIs (AC-005 a AC-010)
- [ ] App inteiro desligado aparece (AC-011)
- [ ] Desinstalado no meio é pulado; cancelamento interrompe a leitura; erro do `PackageManager` é propagado e documentado no KDoc do `AppInventory` (sem tipos do Android) e do `PackageManagerAppInventory` (AC-012 a AC-014)
- [ ] Leitura no dispatcher injetado (AC-015)
- [ ] `QUERY_ALL_PACKAGES` só no `app` e exigida; biblioteca sem permissões (AC-016, AC-017)
- [ ] Supressões só onde as exceções permitem (AC-018)
- [ ] Sem Hilt, sem coletores e sem score no `core-detection` (AC-019)
- [ ] ktlint, detekt, lint e testes passam, sem aviso novo (AC-020)
- [ ] Documentação atualizada, com o ADR-016 e o RF-101 (AC-021)
- [ ] CI da PR verde (AC-022)
