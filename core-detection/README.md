# core-detection

Biblioteca Android com os coletores de sinais sobre as APIs do Android (`PackageManager`, `AccessibilityManager`, `CardEmulation`) e o caso de uso de varredura `ScanDevice`. Implementa `AppInventory` e `SignalCollector` do `core-domain`.

> **Estado (SPEC-011):** inventário de apps (`PackageManagerAppInventory`). Os coletores de sinais entram da SPEC-012 à SPEC-016, e o `ScanDevice` e a ligação no Hilt, na SPEC-018.

## Inventário de apps

`PackageManagerAppInventory(context, ioDispatcher)`, no pacote `io.github.valossa515.tapguard.core.detection.inventory`, é a única classe pública do módulo. `installedApps()` devolve um `InstalledApp` para cada app de usuário, com rótulo, código de versão longo, instalador, datas de instalação e de atualização, permissões pedidas e serviços declarados (inclusive os desligados).

- **Ficam de fora:** apps com `FLAG_SYSTEM` ou `FLAG_UPDATED_SYSTEM_APP` (regra 1 da especificação) e o pacote do próprio app que consulta.
- **Duas etapas, no `ioDispatcher`:** uma lista leve de todos os pacotes, onde rodam os filtros; depois, o detalhe de cada app de usuário, um por chamada. O cancelamento é verificado antes de cada app.
- **Falhas:** um app desinstalado durante a leitura é pulado; os demais erros do `PackageManager` são propagados.
- **Versões do Android:** os ramos ficam em `PackageManagerVersionCompat.kt` (funções `internal`). O código de versão longo vem da API 28; o instalador sai de `getInstallSourceInfo().installingPackageName` a partir da API 30 e de `getInstallerPackageName` abaixo dela; o `PackageInfoFlags` é usado a partir da API 33.
- O construtor guarda só o `PackageManager` e o nome do pacote, não o `Context`.

### Permissão para quem usa a biblioteca

O módulo **não declara permissão nenhuma**: uma permissão no manifesto de uma biblioteca é herdada por todo app que a usa, e este módulo vai virar SDK para bancos. O app que usa o inventário declara no próprio manifesto:

```xml
<uses-permission
    android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

Sem ela, a partir da API 30 o Android só devolve os pacotes visíveis ao app. A permissão é restrita na Play Store e exige a declaração de app de segurança. No Tap Guard, ela fica no manifesto do `app` (ADR-016), e o `LibraryManifestTest` falha se o manifesto da biblioteca passar a declarar alguma permissão.

## Dependências

- **Módulos:** `core-domain` como `api`, porque `AppInventory` e `InstalledApp` aparecem na API pública.
- **Bibliotecas:** `kotlinx-coroutines-core` 1.11.0 como `api` (`CoroutineDispatcher` no construtor público).
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4, Robolectric 4.18, `androidx.test:core` e `kotlinx-coroutines-test`. `unitTests.isIncludeAndroidResources = true`, para o Robolectric ler o manifesto mesclado da biblioteca.
- **Namespace:** `io.github.valossa515.tapguard.core.detection` · minSdk 26 · compileSdk 37.

## Testes

- `PackageManagerAppInventoryTest` roda cada cenário nas APIs 26, 28, 30, 33 e 36. Cobre:
  - a regra 1 com três casos e o próprio pacote;
  - os campos, o código de versão longo e o instalador nos dois caminhos;
  - os serviços e os apps desligados;
  - a desinstalação durante a leitura, o cancelamento e o dispatcher injetado.
- `LibraryManifestTest` confere que a biblioteca não declara permissões, fora o `REORDER_TASKS` que o `androidx.test:core` traz só em teste.

## Regras

- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- Nenhuma lógica de score aqui: os coletores só devolvem sinais.
- `@Suppress("DEPRECATION")` só no ramo antigo de um teste de `SDK_INT`, e no código de produção só em `PackageManagerVersionCompat.kt` (ADR-016).

## Comandos

```bash
./gradlew :core-detection:build
./gradlew :core-detection:testDebugUnitTest
```
