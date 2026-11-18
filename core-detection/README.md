# core-detection

Biblioteca Android com os coletores de sinais sobre as APIs do Android (`PackageManager`, `AccessibilityManager`, `CardEmulation`) e o caso de uso de varredura `ScanDevice`. Implementa `AppInventory` e `SignalCollector` do `core-domain`.

> **Estado (SPEC-012):** inventário de apps (`PackageManagerAppInventory`, SPEC-011) e coletor do sinal S1 com a lista de lojas confiáveis (`InstallSourceCollector` e `TrustedStores`, SPEC-012). Os demais coletores entram da SPEC-013 à SPEC-016, e o `ScanDevice` e a ligação no Hilt, na SPEC-018.

**API pública:** `PackageManagerAppInventory`, `InstallSourceCollector` e `TrustedStores`. O resto é `internal` ou `private`.

## Inventário de apps

`PackageManagerAppInventory(context, ioDispatcher)` fica no pacote `io.github.valossa515.tapguard.core.detection.inventory`. `installedApps()` devolve um `InstalledApp` para cada app de usuário, com rótulo, código de versão longo, instalador, datas de instalação e de atualização, permissões pedidas e serviços declarados (inclusive os desligados).

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

## Coletor do sinal S1 (origem da instalação)

`InstallSourceCollector(trustedInstallers)`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementa `SignalCollector`. Ele não usa API do Android: compara o `installerPackageName` que o inventário já leu com o conjunto recebido na construção.

- **Gera S1** (`UNTRUSTED_INSTALL_SOURCE`) quando o instalador é nulo (instalação por `adb`, ou instalador já desinstalado) ou está fora do conjunto. Fora isso, devolve um conjunto vazio. Nunca devolve outro sinal.
- **Comparação exata**, com maiúsculas e minúsculas. O conjunto é copiado na construção; um conjunto vazio faz todo app receber S1.
- **O parâmetro é obrigatório.** A lista padrão é `TrustedStores.packageNames`:

| Loja | Pacote |
|------|--------|
| Google Play | `com.android.vending` |
| Galaxy Store | `com.sec.android.app.samsungapps` |
| Xiaomi GetApps | `com.xiaomi.mipicks`, `com.xiaomi.market` |
| OPPO App Market (OPPO, realme, OnePlus) | `com.heytap.market`, `com.oppo.market` |
| vivo V-Appstore | `com.vivo.appstore` |
| HONOR App Market | `com.hihonor.appmarket` |
| Palm Store (Tecno, Infinix, itel) | `com.transsnet.store` |
| Huawei AppGallery | `com.huawei.appmarket` |
| Amazon Appstore | `com.amazon.venezia` |

- **Ficam fora** (ADR-017): os instaladores de pacote do sistema (`com.google.android.packageinstaller`, `com.android.packageinstaller` e os de fabricante) e `com.android.shell`, porque são o que aparece num APK instalado à mão; F-Droid; Aurora Store; apps de clonagem (Smart Switch, Mi Mover, Clone Phone).
- **Para o SDK:** `TrustedStores.packageNames` não pode ser alterado. Quem usa o SDK monta o próprio conjunto a partir dele:

```kotlin
InstallSourceCollector(TrustedStores.packageNames + "com.banco.loja")   // amplia
InstallSourceCollector(TrustedStores.packageNames - "com.amazon.venezia") // reduz
```

- **Limitações** (ADR-017):
  - com shell ou root (adb, Shizuku), um app pode ser registrado como instalado por uma loja da lista;
  - num aparelho sem uma das lojas, outro app pode usar o nome de pacote dela. Nesse caso, o app que instalou o golpe recebe S1, e, se ele se desinstalar, o instalador do golpe vira nulo e também gera S1. A defesa (confiar só em instalador que seja app do sistema) está na issue #57.

## Dependências

- **Módulos:** `core-domain` como `api`, porque `AppInventory` e `InstalledApp` aparecem na API pública.
- **Bibliotecas:** `kotlinx-coroutines-core` 1.11.0 como `api` (`CoroutineDispatcher` no construtor público).
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4, Robolectric 4.18, `androidx.test:core`, `kotlinx-coroutines-test` e as fixtures do domínio (`testImplementation(testFixtures(project(":core-domain")))`). `unitTests.isIncludeAndroidResources = true`, para o Robolectric ler o manifesto mesclado da biblioteca.
- **Namespace:** `io.github.valossa515.tapguard.core.detection` · minSdk 26 · compileSdk 37.

## Testes

- `PackageManagerAppInventoryTest` roda cada cenário nas APIs 26, 28, 30, 33 e 36. Cobre:
  - a regra 1 com três casos e o próprio pacote;
  - os campos, o código de versão longo e o instalador nos dois caminhos;
  - os serviços e os apps desligados;
  - a desinstalação durante a leitura, o cancelamento e o dispatcher injetado.
- `LibraryManifestTest` confere que a biblioteca não declara permissões, fora o `REORDER_TASKS` que o `androidx.test:core` traz só em teste.
- Coletor de S1, em JUnit 4 sem Robolectric (não há API do Android nem ramo por versão):
  - `InstallSourceCollectorTrustedStoreTest`: um caso por loja, parametrizado sobre uma lista escrita à mão no teste (`ExpectedTrustedStores.kt`), não sobre a lista de produção;
  - `InstallSourceCollectorUntrustedInstallerTest`: lojas recusadas, app de clonagem e instaladores do sistema;
  - `InstallSourceCollectorTest`: instalador nulo, maiúsculas, só S1, lista maior e menor, cópia do conjunto, lista padrão igual à esperada e imutável.
- As fixtures do domínio (`anInstalledApp`) vêm do `testFixtures` do `core-domain`.

## Regras

- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- Nenhuma lógica de score aqui: os coletores só devolvem sinais.
- `@Suppress("DEPRECATION")` só no ramo antigo de um teste de `SDK_INT`, e no código de produção só em `PackageManagerVersionCompat.kt` (ADR-016).

## Comandos

```bash
./gradlew :core-detection:build
./gradlew :core-detection:testDebugUnitTest
```
