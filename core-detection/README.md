# core-detection

Biblioteca Android com os coletores de sinais sobre as APIs do Android (`PackageManager`, `Settings.Secure`, `CardEmulation`) e o caso de uso de varredura `ScanDevice`. Implementa `AppInventory` e `SignalCollector` do `core-domain`.

> **Estado (SPEC-015 BATCH-01):** inventário de apps com os serviços de HCE e os de acessibilidade ligados (`PackageManagerAppInventory`, SPEC-011, SPEC-013 e SPEC-014), coletor do sinal S1 com a lista de lojas confiáveis (`InstallSourceCollector` e `TrustedStores`, SPEC-012), coletores dos sinais S2 e S3 (`NfcPermissionCollector` e `CardEmulationServiceCollector`, SPEC-013) coletores dos sinais S4, S5, S6 e S8 (`EnabledAccessibilityServiceCollector`, `OverlayPermissionCollector`, `RecentInstallOrUpdateCollector` e `SmsPermissionCollector`, SPEC-014) e listas de carteiras e de bancos conhecidos (`KnownWallets`, `KnownBankApp` e `KnownBanks`, SPEC-015 BATCH-01). A comparação de nomes e o coletor de S9 entram nos BATCH-02 e BATCH-03 da SPEC-015, o coletor de S7 na SPEC-016, e o `ScanDevice` e a ligação no Hilt na SPEC-018.

**API pública:** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores`, `NfcPermissionCollector`, `CardEmulationServiceCollector`, `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector`, `EnabledAccessibilityServiceCollector`, `KnownWallets`, `KnownBankApp` e `KnownBanks`. O resto é `internal` ou `private`.

## Inventário de apps

`PackageManagerAppInventory(context, ioDispatcher)` fica no pacote `io.github.valossa515.tapguard.core.detection.inventory`. `installedApps()` devolve um `InstalledApp` para cada app de usuário, com rótulo, código de versão longo, instalador, datas de instalação e de atualização, permissões pedidas, serviços declarados (inclusive os desligados), serviços de HCE (`cardEmulationServices`) e serviços de acessibilidade ligados (`enabledAccessibilityServices`).

- **Ficam de fora:** apps com `FLAG_SYSTEM` ou `FLAG_UPDATED_SYSTEM_APP` (regra 1 da especificação) e o pacote do próprio app que consulta.
- **Quatro etapas, no `ioDispatcher`:**
  1. uma lista leve de todos os pacotes, onde rodam os filtros;
  2. uma consulta dos serviços de HCE de todos os pacotes (abaixo);
  3. uma leitura dos serviços de acessibilidade ligados (abaixo);
  4. o detalhe de cada app de usuário, um por chamada. O cancelamento é verificado antes de cada app.
- **Serviços de HCE:** uma só chamada `queryIntentServices` por varredura, com a ação `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e `MATCH_DISABLED_COMPONENTS`. Entram os serviços que exigem `android.permission.BIND_NFC_SERVICE`, inclusive os desligados e os de app desligado, agrupados por pacote; os pacotes fora da lista leve são ignorados. É a mesma descoberta do sistema NFC (`RegisteredServicesCache` do AOSP), com duas diferenças registradas no ADR-018:
  - off-host (`OFF_HOST_APDU_SERVICE`) e NFC-F (`HOST_NFCF_SERVICE`) ficam fora;
  - o critério é mais largo de propósito: não exige serviço exportado, meta-data nem a permissão `NFC` no pacote.
  - Um app instalado entre a lista e a consulta fica sem serviços de HCE nessa varredura.
- **Serviços de acessibilidade ligados:** uma só leitura de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` por varredura. É o que o usuário ligou, inclusive um serviço que caiu e ainda vai reconectar (ADR-019).
  - O texto (`pkg/cls:pkg/cls`) é separado por `:`; entradas vazias são puladas; cada uma passa por `ComponentName.unflattenFromString`, que aceita o formato curto (`pkg/.Cls`, o que o sistema grava); as que voltam nulas são puladas sem derrubar a varredura.
  - Os nomes de classe são agrupados por pacote; pacotes fora da lista leve (sistema, próprio app, não instalado, pacote vazio de uma entrada como `/Cls`) são ignorados. Não cruza com `services`: um serviço ligado conta mesmo que a classe não esteja entre os declarados.
  - Chave nula ou vazia quer dizer nenhum serviço ligado. É também o que o framework devolve quando o provedor de configurações falha (ele pega a `RemoteException` e devolve `null`), sem como distinguir.
  - A chave é legível sem permissão da API 26 à 36 (`@Readable` sem `maxTargetSdk` a partir da 31) e não passa pelo filtro de visibilidade de pacotes.
- **Falhas:** um app desinstalado durante a leitura é pulado; os demais erros do `PackageManager` são propagados, e também a `SecurityException` se a chave de acessibilidade deixar de ser legível.
- **Versões do Android:** os ramos ficam em `PackageManagerVersionCompat.kt` (funções `internal`). O código de versão longo vem da API 28; o instalador sai de `getInstallSourceInfo().installingPackageName` a partir da API 30 e de `getInstallerPackageName` abaixo dela; o `PackageInfoFlags` e o `ResolveInfoFlags` são usados a partir da API 33.
- O construtor guarda só o `PackageManager`, o `ContentResolver` e o nome do pacote, não o `Context`.

### Permissão para quem usa a biblioteca

O módulo **não declara permissão nenhuma**: uma permissão no manifesto de uma biblioteca é herdada por todo app que a usa, e este módulo vai virar SDK para bancos. O app que usa o inventário declara no próprio manifesto:

```xml
<uses-permission
    android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

Sem ela, a partir da API 30 o Android só devolve os pacotes visíveis ao app, tanto na lista quanto na consulta de HCE. A permissão é restrita na Play Store e exige a declaração de app de segurança. Um app que use `<queries>` em vez dela precisa, para os serviços de HCE, de:

```xml
<queries>
    <intent>
        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
    </intent>
</queries>
```

e continua precisando ver os próprios pacotes na lista. Para os serviços de acessibilidade ligados, não precisa de nada a mais: a chave não passa pelo filtro de pacotes. No Tap Guard, ela fica no manifesto do `app` (ADR-016), e o `LibraryManifestTest` falha se o manifesto da biblioteca passar a declarar alguma permissão.

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

## Coletores dos sinais S2 e S3 (NFC)

`NfcPermissionCollector()` e `CardEmulationServiceCollector()`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementam `SignalCollector`. Não têm parâmetros, não usam API do Android e não chamam o `PackageManager`: leem o que o inventário já trouxe. Cada um devolve o seu sinal ou um conjunto vazio, nunca outro sinal.

- **S2** (`NFC_PERMISSION`): `requestedPermissions` contém `android.permission.NFC`, com comparação exata. Não contam `android.permission.NFC_TRANSACTION_EVENT` e `android.permission.NFC_PREFERRED_PAYMENT_INFO` (não leem nem emulam cartão sem a `NFC`), nem `android.permission.BIND_NFC_SERVICE` (o app a usa para proteger o serviço, não a pede).
- **S3** (`CARD_EMULATION_SERVICE`): `cardEmulationServices` não está vazio. O critério é o do inventário (acima). Não depende do S2: um serviço de HCE num app sem a permissão `NFC` gera S3.
- Motivos e limitações no ADR-018.

## Coletor do sinal S4 (acessibilidade)

`EnabledAccessibilityServiceCollector()`, no mesmo pacote, devolve `ACCESSIBILITY_SERVICE_ENABLED` quando `enabledAccessibilityServices` não está vazio, ou um conjunto vazio. O critério é o do inventário (acima): serviço só declarado, que o usuário não ligou, não conta. Não tem parâmetros e não usa API do Android. Motivos da fonte (`Settings.Secure` no lugar do `AccessibilityManager`) no ADR-019.

## Coletores dos sinais S5, S6 e S8 (controle)

`OverlayPermissionCollector()`, `SmsPermissionCollector()` e `RecentInstallOrUpdateCollector(clock)`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementam `SignalCollector`. Não usam API do Android e não chamam o `PackageManager`: leem o que o inventário já trouxe. Cada um devolve o seu sinal ou um conjunto vazio, nunca outro sinal.

- **S5** (`OVERLAY_PERMISSION`): `requestedPermissions` contém `android.permission.SYSTEM_ALERT_WINDOW`, com comparação exata. Vale a permissão declarada, não a concedida (a seção 5 diz "pede"); o `AppOpsManager` não é lido.
- **S8** (`SMS_PERMISSION`): `requestedPermissions` contém `android.permission.READ_SMS` ou `android.permission.RECEIVE_SMS`, com comparação exata; as duas juntas geram um só S8. Não contam `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`, `READ_CELL_BROADCASTS` e `BROADCAST_SMS`.
- **S6** (`RECENTLY_INSTALLED_OR_UPDATED`): o mais recente entre `firstInstallTime` e `lastUpdateTime` não é anterior ao agora menos 7 dias (168 horas corridas, sem fuso).
  - O agora vem só do `java.time.Clock` recebido no construtor, obrigatório. Quem usa a biblioteca passa `Clock.systemUTC()`; a SPEC-018 o liga pelo Hilt.
  - O limite é inclusivo, e um horário no futuro conta. Com o relógio do aparelho atrasado, todos os apps ganham S6. Sozinho ele vale 10 e o app fica verde, mas somado a outros sinais pode mudar o nível.
- Motivos, exemplos e limitações no ADR-019.

## Listas de apoio (carteiras e bancos)

Duas listas no pacote `io.github.valossa515.tapguard.core.detection.signal`, no mesmo formato do `TrustedStores`: `object` público, conjunto que não pode ser alterado (nem por cast) e um comentário por entrada com o nome e a fonte (em `KnownBanks`, um por instituição, com o título de cada app). Motivos, critério e fontes no ADR-020.

- **`KnownWallets.packageNames`** (6 pacotes): apps que podem ser o app de pagamento por aproximação padrão sem gerar S7. Quem a usa é o coletor de S7 (SPEC-016).

| App | Pacote |
|-----|--------|
| Carteira do Google | `com.google.android.apps.walletnfcrel` |
| Samsung Wallet | `com.samsung.android.spay` |
| Nubank | `com.nu.production` |
| Itaú | `com.itau` |
| Banco do Brasil | `br.com.bb.android` |
| Inter | `br.com.intermedium` |

- **Ficam fora de `KnownWallets`:** entram só apps com fonte de que fazem o pagamento por aproximação, as duas carteiras e os bancos com Pix por aproximação no próprio app. Mercado Pago, PicPay, Bradesco, Caixa, Santander e C6 fazem o Pix por aproximação pela Carteira do Google e ficam fora.
- **`KnownBanks.apps`** (29 entradas, 26 pacotes): nome de exibição e pacote oficial dos apps das 14 instituições com mais clientes no ranking do Banco Central (dez/2025): Caixa, Nubank, Bradesco, Itaú, Banco do Brasil, Santander, Mercado Pago, PicPay, Inter, PagBank, C6 Bank, BTG Pactual, 99Pay e Neon, com os apps oficiais delas que levam o nome da marca. Quem vai usá-la é o coletor de S9 (BATCH-03).
  - Cada entrada é um `KnownBankApp(displayName, packageName)`: um app ou um apelido de app. Um pacote pode ter mais de um nome ("BB" e "Banco do Brasil"; "Santander Brasil" e "Santander"; "BTG Pactual Banking" e "BTG Pactual"), e uma instituição, mais de um app (Itaú, Itaú Empresas e Personnalité).
  - `displayName` é o nome da marca, tirado do título da Play ou do desenvolvedor, não o rótulo do aparelho, que é conferido no teste ponta a ponta (SPEC-022). Nome ou pacote em branco lança `IllegalArgumentException`.
  - Ficaram para depois, com o pacote já conferido: Banco Pan, Sicoob, Sicredi, Banrisul, Safra e BV.
- **Para o SDK:** quem usa a biblioteca monta o próprio conjunto a partir das listas padrão:

```kotlin
KnownWallets.packageNames + "com.banco.app"                           // amplia
KnownBanks.apps + KnownBankApp("Banco X", "com.bancox")               // amplia
KnownBanks.apps - KnownBankApp("next", "br.com.bradesco.next")        // reduz
```

- **Limitação** (ADR-017 e ADR-020): o nome de pacote é único no aparelho, não no mundo. Num aparelho sem o app original, outro app pode usar o mesmo pacote. A defesa (certificado de assinatura por pacote) está na issue #65.

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
  - os serviços de HCE: filtro e permissão, off-host e NFC-F fora, desligados, app desligado, critério mais largo que o do sistema e isolamento por pacote;
  - os serviços de acessibilidade ligados: formatos completo e curto, chave nula e vazia, entradas malformadas e isolamento por pacote (sistema, próprio pacote, não instalado);
  - a desinstalação durante a leitura, o cancelamento e o dispatcher injetado.
  - A chave de acessibilidade é gravada com `Settings.Secure.putString` (helper `enableAccessibilityServices`), que basta nas cinco APIs; ela volta a `null` em cada teste. O Robolectric não aplica o `@Readable`.
  - Um serviço que precisa aparecer no `queryIntentServices` entra com `addOrUpdateService` e `addIntentFilterForService` (helper `installService`). Posto no `PackageInfo` do `installPackage`, ele não entra na resolução de intents do Robolectric.
- `LibraryManifestTest` confere que a biblioteca não declara permissões, fora o `REORDER_TASKS` que o `androidx.test:core` traz só em teste.
- Coletor de S1, em JUnit 4 sem Robolectric (não há API do Android nem ramo por versão):
  - `InstallSourceCollectorTrustedStoreTest`: um caso por loja, parametrizado sobre uma lista escrita à mão no teste (`ExpectedTrustedStores.kt`), não sobre a lista de produção;
  - `InstallSourceCollectorUntrustedInstallerTest`: lojas recusadas, app de clonagem e instaladores do sistema;
  - `InstallSourceCollectorTest`: instalador nulo, maiúsculas, só S1, lista maior e menor, cópia do conjunto, lista padrão igual à esperada e imutável.
- Coletores de S2 e S3, também em JUnit 4 sem Robolectric:
  - `NfcPermissionCollectorTest`: permissão de NFC, sem permissões, permissão sem relação, maiúsculas e minúsculas, só S2;
  - `NfcPermissionCollectorRelatedPermissionTest`: as três permissões relacionadas, parametrizado sobre uma lista escrita à mão;
  - `CardEmulationServiceCollectorTest`: serviço de HCE, sem serviços, serviço comum, HCE sem NFC, só S3.
- Coletores de S5, S6 e S8, também em JUnit 4 sem Robolectric:
  - `OverlayPermissionCollectorTest`: permissão de sobreposição, sem permissões, permissão sem relação, maiúsculas e minúsculas, só S5;
  - `SmsPermissionCollectorTest`: `READ_SMS`, `RECEIVE_SMS`, as duas, sem permissões, permissão sem relação, maiúsculas e minúsculas, só S8;
  - `SmsPermissionCollectorUnrelatedPermissionTest`: as cinco permissões de SMS que não contam, parametrizado sobre uma lista escrita à mão;
  - `EnabledAccessibilityServiceCollectorTest`: serviço ligado, sem serviço ligado, serviço só declarado, só S4;
  - `RecentInstallOrUpdateCollectorTest`, com `Clock.fixed`: instalação recente, as duas datas antigas, só a atualização recente, exatamente 7 dias, 7 dias e 1 ms, futuro na instalação e só na atualização, o mesmo app contra dois relógios, só S6.
- Listas de apoio, também em JUnit 4 sem Robolectric:
  - `KnownWalletsTest` e `KnownBanksTest`: lista padrão igual à escrita à mão no teste (`ExpectedKnownWallets.kt`, `ExpectedKnownBankApps.kt`) e que não pode ser alterada por cast;
  - `KnownBankAppTest`: nome e pacote preenchidos criam a entrada; nome ou pacote vazio ou só com espaços é recusado.
- As fixtures do domínio (`anInstalledApp`) vêm do `testFixtures` do `core-domain`.

## Regras

- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- Nenhuma lógica de score aqui: os coletores só devolvem sinais.
- `@Suppress("DEPRECATION")` só no ramo antigo de um teste de `SDK_INT`, e no código de produção só em `PackageManagerVersionCompat.kt` (ADR-016).
- `@SuppressLint("QueryPermissionsNeeded")` só em `installedPackagesCompat` e `queryIntentServicesCompat`, com comentário (ADR-016 e ADR-018).

## Comandos

```bash
./gradlew :core-detection:build
./gradlew :core-detection:testDebugUnitTest
```
