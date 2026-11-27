# core-detection

Biblioteca Android com os coletores de sinais sobre as APIs do Android (`PackageManager`, `Settings.Secure`, `CardEmulation`) e o caso de uso de varredura `ScanDevice`. Implementa `AppInventory` e `SignalCollector` do `core-domain`.

> **Estado (SPEC-019):** inventário de apps com os serviços de HCE, os de acessibilidade ligados e o serviço de pagamento padrão (`PackageManagerAppInventory`, SPEC-011, SPEC-013, SPEC-014 e SPEC-016), coletor do sinal S1 com a lista de lojas confiáveis (`InstallSourceCollector` e `TrustedStores`, SPEC-012), coletores dos sinais S2 e S3 (`NfcPermissionCollector` e `CardEmulationServiceCollector`, SPEC-013) coletores dos sinais S4, S5, S6 e S8 (`EnabledAccessibilityServiceCollector`, `OverlayPermissionCollector`, `RecentInstallOrUpdateCollector` e `SmsPermissionCollector`, SPEC-014) , listas de carteiras e de bancos conhecidos (`KnownWallets`, `KnownBankApp` e `KnownBanks`, SPEC-015 BATCH-01), a comparação de nomes do S9 (`BankNameMatcher`, `internal`, SPEC-015 BATCH-02) o coletor do sinal S9 (`BankLookalikeCollector`, SPEC-015 BATCH-03) o coletor do sinal S7 (`DefaultPaymentAppCollector`, SPEC-016) e o caso de uso de varredura (`DefaultScanDevice`, SPEC-018 BATCH-03). A ligação no Hilt do `app` entra no BATCH-04 da SPEC-018.

**API pública:** `PackageManagerAppInventory`, `InstallSourceCollector`, `TrustedStores`, `NfcPermissionCollector`, `CardEmulationServiceCollector`, `OverlayPermissionCollector`, `SmsPermissionCollector`, `RecentInstallOrUpdateCollector`, `EnabledAccessibilityServiceCollector`, `KnownWallets`, `KnownBankApp`, `KnownBanks`, `BankLookalikeCollector`, `DefaultPaymentAppCollector`, `KnownCloneApps` e `DefaultScanDevice` (dezesseis classes). O resto é `internal` ou `private`.

## Inventário de apps

`PackageManagerAppInventory(context, ioDispatcher)` fica no pacote `io.github.valossa515.tapguard.core.detection.inventory`. `installedApps()` devolve um `InstalledApp` para cada app de usuário, com rótulo, código de versão longo, instalador, datas de instalação e de atualização, permissões pedidas, serviços declarados (inclusive os desligados), serviços de HCE (`cardEmulationServices`), serviços de acessibilidade ligados (`enabledAccessibilityServices`) e os serviços que o sistema diz serem o padrão de pagamento (`defaultPaymentServices`).

- **Ficam de fora:** apps com `FLAG_SYSTEM` ou `FLAG_UPDATED_SYSTEM_APP` (regra 1 da especificação) e o pacote do próprio app que consulta.
- **Cinco etapas, no `ioDispatcher`:**
  1. uma lista leve de todos os pacotes, onde rodam os filtros;
  2. uma consulta dos serviços de HCE de todos os pacotes (abaixo);
  3. uma leitura dos serviços de acessibilidade ligados (abaixo);
  4. a consulta do pagamento padrão (abaixo);
  5. o detalhe de cada app de usuário, um por chamada. O cancelamento é verificado antes de cada app.
- **Serviços de HCE:** uma só chamada `queryIntentServices` por varredura, com a ação `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e `MATCH_DISABLED_COMPONENTS`. Entram os serviços que exigem `android.permission.BIND_NFC_SERVICE`, inclusive os desligados e os de app desligado, agrupados por pacote; os pacotes fora da lista leve são ignorados. É a mesma descoberta do sistema NFC (`RegisteredServicesCache` do AOSP), com duas diferenças registradas no ADR-018:
  - off-host (`OFF_HOST_APDU_SERVICE`) e NFC-F (`HOST_NFCF_SERVICE`) ficam fora;
  - o critério é mais largo de propósito: não exige serviço exportado, meta-data nem a permissão `NFC` no pacote.
  - Um app instalado entre a lista e a consulta fica sem serviços de HCE nessa varredura.
- **Serviços de acessibilidade ligados:** uma só leitura de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` por varredura. É o que o usuário ligou, inclusive um serviço que caiu e ainda vai reconectar (ADR-019).
  - O texto (`pkg/cls:pkg/cls`) é separado por `:`; entradas vazias são puladas; cada uma passa por `ComponentName.unflattenFromString`, que aceita o formato curto (`pkg/.Cls`, o que o sistema grava); as que voltam nulas são puladas sem derrubar a varredura.
  - Os nomes de classe são agrupados por pacote; pacotes fora da lista leve (sistema, próprio app, não instalado, pacote vazio de uma entrada como `/Cls`) são ignorados. Não cruza com `services`: um serviço ligado conta mesmo que a classe não esteja entre os declarados.
  - Chave nula ou vazia quer dizer nenhum serviço ligado. É também o que o framework devolve quando o provedor de configurações falha (ele pega a `RemoteException` e devolve `null`), sem como distinguir.
  - A chave é legível sem permissão da API 26 à 36 (`@Readable` sem `maxTargetSdk` a partir da 31) e não passa pelo filtro de visibilidade de pacotes.
- **Pagamento padrão** (ADR-021): o sistema diz qual serviço de HCE é o padrão de pagamento por aproximação. `CardEmulation` não tem método público que devolva o padrão, e a chave `nfc_payment_default_component` do `Settings.Secure` é `@hide` sem `@Readable` (`SecurityException` para app comum da API 31 em diante). Por isso o inventário pergunta serviço por serviço:
  - só quando algum app de usuário tem serviço de HCE e o aparelho tem NFC com HCE (`NfcAdapter.getDefaultAdapter` não nulo e `CardEmulation.getInstance` sem `UnsupportedOperationException`);
  - uma segunda consulta de HCE, **sem** `MATCH_DISABLED_COMPONENTS`, diz quais serviços estão ligados. O `ServiceInfo.enabled` não serve: é o valor do manifesto, e um serviço desligado no manifesto e ligado depois com `setComponentEnabledSetting` conta. Ficam os exportados que também estão em `cardEmulationServices` de um app de usuário. O `cardEmulationServices` continua com o critério largo;
  - para cada um, `isDefaultServiceForCategory(componente, CATEGORY_PAYMENT)`. Até a API 34, o sistema compara com o componente escolhido nas configurações de NFC; da API 35 em diante, com o papel de carteira ligado, responde `true` para todo serviço registrado do app dono do papel;
  - sem NFC, sem HCE ou com o serviço NFC morto (na API 36, `RuntimeException` com causa `RemoteException`; até a 35 o framework já devolve `false`), todos os apps ficam com o campo vazio, sem erro. Funciona com o NFC desligado.
- **Falhas:** um app desinstalado durante a leitura é pulado; os demais erros do `PackageManager` são propagados, e também a `SecurityException` se a chave de acessibilidade deixar de ser legível ou se o app não tiver a permissão `android.permission.NFC`.
- **Versões do Android:** os ramos ficam em `PackageManagerVersionCompat.kt` (funções `internal`). O código de versão longo vem da API 28; o instalador sai de `getInstallSourceInfo().installingPackageName` a partir da API 30 e de `getInstallerPackageName` abaixo dela; o `PackageInfoFlags` e o `ResolveInfoFlags` são usados a partir da API 33.
- O construtor guarda o `PackageManager`, o `ContentResolver`, o nome do pacote e o `Context` da aplicação (para o `NfcAdapter`).

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

e continua precisando ver os próprios pacotes na lista. Para os serviços de acessibilidade ligados e para o pagamento padrão, não precisa de nada a mais: a chave não passa pelo filtro de pacotes, e as duas consultas de HCE usam a mesma ação.

Para o pagamento padrão, o app também declara `android.permission.NFC` (permissão normal, concedida na instalação). Sem ela, o sistema NFC recusa a pergunta e `installedApps()` lança `SecurityException`:

```xml
<uses-permission android:name="android.permission.NFC" />
```

No Tap Guard, as duas ficam no manifesto do `app` (ADR-016), e o `LibraryManifestTest` falha se o manifesto da biblioteca passar a declarar alguma permissão.

## Coletor do sinal S1 (origem da instalação)

`InstallSourceCollector(trustedInstallers, knownBankPackages, knownCloneApps)`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementa `SignalCollector`. Lê somente os campos do `InstalledApp` recebidos do inventário, sem chamada ao Android.

- **Regra (SPEC-019):** Sem S1 quando o instalador está nas lojas OU (pacote está nos bancos E instalador está nos clonadores). Instalador nulo e todos os demais casos geram S1. Retorna somente vazio ou `UNTRUSTED_INSTALL_SOURCE`.
- **Comparação exata**, com maiúsculas e minúsculas. Três conjuntos obrigatórios, sem padrão ou overload antigo, copiados separadamente. Bancos vazios ou clonadores vazios desligam só a exceção; lojas vazias ainda permitem banco com clonador; todos vazios geram S1 para qualquer app.
- A SPEC-018 monta no Hilt do `app`: `TrustedStores.packageNames`, `KnownBanks.apps.map { it.packageName }.toSet()` e `KnownCloneApps.packageNames`. O mapeamento deduplica as 29 entradas em 26 pacotes; o construtor faz a cópia defensiva.
- A lista de lojas é `TrustedStores.packageNames`:

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
val bankPackages = KnownBanks.apps.map { it.packageName }.toSet()
InstallSourceCollector(
    trustedInstallers = TrustedStores.packageNames + "com.banco.loja",
    knownBankPackages = bankPackages,
    knownCloneApps = KnownCloneApps.packageNames,
) // amplia lojas; os três conjuntos podem ser personalizados
InstallSourceCollector(TrustedStores.packageNames - "com.amazon.venezia", bankPackages, KnownCloneApps.packageNames)
```

- **Limitações** (ADR-017):
  - com shell ou root (adb, Shizuku), um app pode ser registrado como instalado por uma loja da lista;
  - num aparelho sem uma das lojas, outro app pode usar o nome de pacote dela. Nesse caso, o app que instalou o golpe recebe S1, e, se ele se desinstalar, o instalador do golpe vira nulo e também gera S1. A defesa (confiar só em instalador que seja app do sistema) está na issue #57.

- **Clonadores conhecidos (SPEC-019):** Samsung Smart Switch (`com.sec.android.easyMover`), Xiaomi Mi Mover (`com.miui.huanji`), OPPO Clone Phone (`com.coloros.backuprestore`), OnePlus Clone Phone (`com.oneplus.backuprestore`), Huawei Phone Clone (`com.hicloud.android.clone`), HONOR Device Clone (`com.hihonor.android.clone`) e vivo EasyShare (`com.vivo.easyshare`). `KnownCloneApps.packageNames` é público, não modificável nem por cast, com nome e fonte por entrada. ShareMe (`com.xiaomi.midrop`), SHAREit (`com.lenovo.anyshare.gps`), SHAREit Lite (`shareit.lite`) e MIUI Backup (`com.miui.backup`) ficam fora. Fontes no ADR-017 e nos comentários da lista.
- **Limitação da exceção:** pacote de banco emprestado já no aparelho antigo e pacote de clonador emprestado continuam possíveis. Defesas nas #57/#65; atribuição real no aparelho fica para a SPEC-022. Sem S1, o teto é 29 salvo S7/S9; com esses sinais, o teto é 59. Não há verde incondicional.

## Coletores dos sinais S2 e S3 (NFC)

`NfcPermissionCollector()` e `CardEmulationServiceCollector()`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementam `SignalCollector`. Não têm parâmetros, não usam API do Android e não chamam o `PackageManager`: leem o que o inventário já trouxe. Cada um devolve o seu sinal ou um conjunto vazio, nunca outro sinal.

- **S2** (`NFC_PERMISSION`): `requestedPermissions` contém `android.permission.NFC`, com comparação exata. Não contam `android.permission.NFC_TRANSACTION_EVENT` e `android.permission.NFC_PREFERRED_PAYMENT_INFO` (não leem nem emulam cartão sem a `NFC`), nem `android.permission.BIND_NFC_SERVICE` (o app a usa para proteger o serviço, não a pede).
- **S3** (`CARD_EMULATION_SERVICE`): `cardEmulationServices` não está vazio. O critério é o do inventário (acima). Não depende do S2: um serviço de HCE num app sem a permissão `NFC` gera S3.
- Motivos e limitações no ADR-018.

## Coletor do sinal S4 (acessibilidade)

`EnabledAccessibilityServiceCollector()`, no mesmo pacote, devolve `ACCESSIBILITY_SERVICE_ENABLED` quando `enabledAccessibilityServices` não está vazio, ou um conjunto vazio. O critério é o do inventário (acima): serviço só declarado, que o usuário não ligou, não conta. Não tem parâmetros e não usa API do Android. Motivos da fonte (`Settings.Secure` no lugar do `AccessibilityManager`) no ADR-019.

## Coletor do sinal S7 (pagamento padrão)

`DefaultPaymentAppCollector(knownWallets)`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementa `SignalCollector`. Não usa API do Android: lê o `defaultPaymentServices` e o `packageName` que o inventário já trouxe.

- **Gera S7** (`UNKNOWN_DEFAULT_PAYMENT_APP`) quando `defaultPaymentServices` não está vazio e o pacote não está no conjunto. Fora isso, devolve um conjunto vazio. Nunca devolve outro sinal.
- **Comparação exata**, com maiúsculas e minúsculas. O conjunto é copiado na construção; um conjunto vazio faz todo app padrão receber S7.
- **O parâmetro é obrigatório**, sem valor padrão. A lista padrão é `KnownWallets.packageNames` (abaixo):

```kotlin
DefaultPaymentAppCollector(KnownWallets.packageNames)
DefaultPaymentAppCollector(KnownWallets.packageNames + "com.banco.app")   // amplia
```

- Serviço padrão de um app do sistema ou do próprio app não gera S7 em nenhum app: o inventário não lista esses pacotes.
- **Limitação do pacote emprestado** (ADR-017 e ADR-020): num aparelho sem a carteira, um app com o pacote dela não recebe S7. Instalado à mão, recebe S1. Defesa na issue #65.
- Fonte, comportamento por versão do Android e ressalvas no ADR-021.

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

- **`KnownWallets.packageNames`** (6 pacotes): apps que podem ser o app de pagamento por aproximação padrão sem gerar S7. Quem a usa é o coletor de S7 (acima).

| App | Pacote |
|-----|--------|
| Carteira do Google | `com.google.android.apps.walletnfcrel` |
| Samsung Wallet | `com.samsung.android.spay` |
| Nubank | `com.nu.production` |
| Itaú | `com.itau` |
| Banco do Brasil | `br.com.bb.android` |
| Inter | `br.com.intermedium` |

- **Ficam fora de `KnownWallets`:** entram só apps com fonte de que fazem o pagamento por aproximação, as duas carteiras e os bancos com Pix por aproximação no próprio app. Mercado Pago, PicPay, Bradesco, Caixa, Santander e C6 fazem o Pix por aproximação pela Carteira do Google e ficam fora.
- **`KnownBanks.apps`** (29 entradas, 26 pacotes): nome de exibição e pacote oficial dos apps das 14 instituições com mais clientes no ranking do Banco Central (dez/2025): Caixa, Nubank, Bradesco, Itaú, Banco do Brasil, Santander, Mercado Pago, PicPay, Inter, PagBank, C6 Bank, BTG Pactual, 99Pay e Neon, com os apps oficiais delas que levam o nome da marca. Quem a usa é o coletor de S9 (abaixo); seus pacotes distintos também configuram a exceção do S1 via Hilt na SPEC-018.
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

## Comparação de nomes do S9

`BankNameMatcher(displayNames)`, em `BankNameMatcher.kt` no pacote `io.github.valossa515.tapguard.core.detection.signal`, diz se o rótulo de um app casa com algum nome da lista de bancos. É `internal`: quem o usa é o coletor de S9 (abaixo), que junta a comparação ao pacote e ao sinal. Não usa API do Android (`java.text.Normalizer` é do JDK). Motivos e exemplos no ADR-020.

1. **Esqueleto do nome** (`bankNameSkeleton`), o mesmo para o rótulo e para os nomes da lista:
   - NFKC (largura cheia, ligaduras), antes do corte, para que `：` e `｜` de largura cheia também cortem;
   - corta no primeiro separador (`:`, `|`, ` - `, ` – `, ` — `), se houver algo antes dele: "Nubank: conta, cartão e mais" vira "Nubank";
   - minúsculas invariantes, NFD e remoção dos acentos pelo tipo do caractere, sem regex;
   - letras parecidas: `0→o`; `1`, `!` e `l` por `i`; `3→e`; `4` e `@` por `a`; `5` e `$` por `s`; `7→t`; cirílico e grego que imitam letras latinas; `ı ɑ ɪ ɩ օ`; maiúsculas pequenas latinas (`ʟ` vira `i`, como o `l`); `rn→m`; `vv→w`. O mapa é uma lista parcial: uma letra parecida fora dele é apagada, e nos nomes de até 5 letras a imitação escapa. A geração a partir do `confusables.txt` do UTS #39 está na issue #69;
   - fica só com `a-z` e `0-9`.
2. **Variantes:** o esqueleto; sem um prefixo (`banco`, `meu`, `app`); e sem sufixos, repetidamente (`token`, `empresas`, `empresa`, `cartoes`, `cartao`, `pj`, `app`, `mobile`, `oficial`, `digital`, `seguranca`). Funciona com as palavras coladas ("ItauToken"), e um prefixo ou sufixo só sai se sobrar algo. Os prefixos e sufixos também passam pelo esqueleto quando o arquivo carrega (`BANK_NAME_PREFIXES` e `BANK_NAME_SUFFIXES`, `internal`): sem isso, `mobile`, `oficial` e `digital` nunca sairiam, porque o esqueleto troca `l` por `i`.
3. **Casa** quando alguma variante fica a uma distância de Damerau-Levenshtein (versão OSA: inserção, remoção, troca e transposição de letras vizinhas, `editDistance`) dentro do limite do nome da lista:

| Esqueleto do nome da lista | Limite | Exemplos |
|---|---|---|
| até 5 letras | 0 (só igual) | `itau`, `caixa`, `bb`, `inter`, `neon`, `next`, `99pay` |
| de 6 a 9 letras | 1 | `nubank`, `bradesco`, `santander`, `picpay` |
| 10 letras ou mais | 2 | `bancodobrasii`, `mercadopago` |

4. **Esqueleto vazio** (rótulo só com emoji ou pontuação) nunca casa; um nome da lista com esqueleto vazio é ignorado.

- **Comportamentos aceitos:**
  - nome curto igual de outro desenvolvedor casa ("Inter" de um clube, "NEXT" de uma loja);
  - o corte no separador também casa um app legítimo que comece com o nome de um banco ("Next - Launcher", "Caixa – Organizador");
  - palavras fora da lista fechada escapam ("Atualização Nubank", "Nubank Wallpapers", "Inter&Co", "Neon Pay");
  - "Baixa" não casa com "Caixa", nem "Caixa de Ferramentas" (nome inteiro, sem "contém").
- O pacote oficial, que tira o app do S9, é tratado pelo coletor (abaixo).

## Coletor do sinal S9 (imitação de banco)

`BankLookalikeCollector(knownBankApps)`, no pacote `io.github.valossa515.tapguard.core.detection.signal`, implementa `SignalCollector`. Ele não usa API do Android e não chama o `PackageManager`: lê o `label` e o `packageName` que o inventário já trouxe.

- **Gera S9** (`BANK_LOOKALIKE`) quando o rótulo casa com o nome de alguma entrada da lista (comparação de nomes, acima) e o pacote do app não é o de nenhuma entrada. Fora isso, devolve um conjunto vazio. Nunca devolve outro sinal.
- **Pacote da lista não gera S9**, de qualquer entrada e não só da que casou: "Itaú Empresas" (`com.itau.empresas`) casa com "Itaú" depois de tirar o sufixo, mas o pacote está na lista. A comparação do pacote é exata, com maiúsculas e minúsculas: `com.nu.production.fake` e `COM.NU.PRODUCTION` com o nome "Nubank" geram S9.
- **O parâmetro é obrigatório**, sem valor padrão. O conjunto é copiado na construção, e um conjunto vazio nunca gera S9. A lista padrão é `KnownBanks.apps`:

```kotlin
BankLookalikeCollector(KnownBanks.apps)
BankLookalikeCollector(KnownBanks.apps + KnownBankApp("Banco X", "com.bancox"))   // amplia
```

- O banco fictício da variante `lookalike` do app isca entra do mesmo jeito; como o teste ponta a ponta o injeta fica para a SPEC-021 ou a SPEC-022.
- **Limitação do pacote emprestado** (ADR-017 e ADR-020): num aparelho sem o app do banco, um app com o pacote oficial e o nome do banco não recebe S9. Instalado à mão, ele recebe S1, e com NFC e HCE fica vermelho mesmo sem o S9. A defesa pelo certificado de assinatura está na issue #65.

## Caso de uso de varredura (SPEC-018, ADR-023)

`DefaultScanDevice(inventory, collectors, trustedApps, scans, clock, dispatcher)`, no pacote `scan`, implementa o `ScanDevice` do `core-domain`. Em cada varredura, nesta ordem:

1. o início vem do `clock`;
2. o `inventory` devolve os apps de usuário;
3. os apps confiáveis são lidos uma vez de `trustedApps` e viram o `ScoringContext` (regra 2);
4. no `dispatcher`, para cada app na ordem do inventário, confere o cancelamento (`ensureActive()`), roda os coletores um depois do outro e passa a união dos sinais ao `WeightedRiskScorer`, que ele mesmo cria (ADR-022);
5. o fim é o maior entre o início e o agora, porque o relógio do aparelho pode voltar no meio;
6. o `ScanReport` é gravado uma vez em `scans` e devolvido.

- **Cancelamento:** antes da gravação, nada é gravado.
- **Erros:** sem `try`. Erro do inventário, de um coletor, da leitura dos confiáveis ou da gravação chega a quem chama, sem nada gravado pela metade.
- **Lista de coletores:** é copiada.
- **Uso pelo SDK:** quem usa o SDK passa as próprias implementações das portas `TrustedAppRepository` e `ScanRepository` (uma em memória basta), além do `Clock` (`Clock.systemUTC()`) e de um dispatcher para o laço (por exemplo, `Dispatchers.Default`). No Tap Guard, a ligação é feita no Hilt do `app` (SPEC-018 BATCH-04).

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
  - o pagamento padrão: serviço padrão de um app, padrão fora dos serviços consultados (app do sistema, classe sem o filtro), papel de carteira, serviços desligados ou não exportados e app desligado não consultados, serviço ligado em tempo de execução consultado, sem NFC, NFC sem HCE, serviço NFC morto e sem a permissão `NFC`;
  - a desinstalação durante a leitura, o cancelamento e o dispatcher injetado.
  - A chave de acessibilidade é gravada com `Settings.Secure.putString` (helper `enableAccessibilityServices`), que basta nas cinco APIs; ela volta a `null` em cada teste. O Robolectric não aplica o `@Readable`.
  - Um serviço que precisa aparecer no `queryIntentServices` entra com `addOrUpdateService` e `addIntentFilterForService` (helper `installService`). Posto no `PackageInfo` do `installPackage`, ele não entra na resolução de intents do Robolectric.
  - O NFC simulado liga `FEATURE_NFC`, `FEATURE_NFC_HOST_CARD_EMULATION` e `ShadowNfcAdapter.setNfcHardwareExists(true)` (helper `enableCardEmulation`); sem as features, `getDefaultAdapter` devolve nulo. O padrão é definido com `ShadowCardEmulation.setDefaultServiceForCategory` e a categoria literal `payment` (helper `setDefaultPaymentService`). O shadow responde `true` mesmo para serviço desligado: campo vazio prova que o inventário não perguntou. Como no aparelho, o `ServiceInfo.enabled` é o do manifesto e o estado de `setComponentEnabledSetting` só filtra a consulta sem `MATCH_DISABLED_COMPONENTS`.
  - Três shadows próprios em `CardEmulationShadows.kt`, por `@Config(shadows = [...])` no método: serviço NFC morto (`RemoteException(...).rethrowAsRuntimeException()`, porque o detekt recusa `throw RuntimeException(...)`), sem a permissão `NFC` (`SecurityException`) e papel de carteira (`true` para todo serviço de um pacote). O método do shadow usa os dois parâmetros (`UnusedParameter`). O Robolectric não simula o registro do serviço no sistema NFC, o papel de carteira real nem a permissão: conferência em aparelho na SPEC-022.
- `LibraryManifestTest` confere que a biblioteca não declara permissões, fora o `REORDER_TASKS` que o `androidx.test:core` traz só em teste.
- Coletor de S1, em JUnit 4 sem Robolectric (não há API do Android nem ramo por versão):
  - `InstallSourceCollectorTrustedStoreTest`: um caso por loja, parametrizado sobre uma lista escrita à mão no teste (`ExpectedTrustedStores.kt`), não sobre a lista de produção;
  - `InstallSourceCollectorUntrustedInstallerTest`: lojas recusadas, app de clonagem e instaladores do sistema;
  - `InstallSourceCollectorTest`: instalador nulo, maiúsculas, só S1, lista maior e menor, cópia do conjunto, lista padrão igual à esperada e imutável.
  - `InstallSourceCollectorCloneAppTest`: sete clonadores com banco e com app comum (14 casos).
  - `InstallSourceCollectorRejectedCloneTest`: banco com nulo, instaladores manuais e quatro apps excluídos (10 casos).
  - `InstallSourceCollectorConfigurationTest`: caixa, rótulo, vazios e conjuntos próprios (15 casos).
  - `InstallSourceCollectorCopiedSetsTest`: remoção e adição posteriores nos três conjuntos (3 casos).
  - `KnownCloneAppsTest` e `ExpectedKnownCloneApps`: sete pacotes escritos à mão e imutabilidade (2 casos).
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
- Comparação de nomes do S9, também em JUnit 4 sem Robolectric:
  - `BankNameSkeletonTest`: o esqueleto esperado de cada rótulo (maiúsculas, acentos, cada separador, separador no início, largura cheia, dígitos e símbolos, `l`, cirílico, grego, `rn`, `vv`, espaço de largura zero, só emoji), parametrizado. Letras que não se distinguem na tela vão em escape `\uXXXX`;
  - `EditDistanceTest`: igual, troca, inserção e remoção, transposição, cadeia vazia, duas edições;
  - `BankNameMatcherMatchTest` e `BankNameMatcherNoMatchTest`: os imitadores e os legítimos da tabela do plano, com os falsos positivos aceitos e as imitações que escapam em seções comentadas, parametrizados sobre uma lista de 13 nomes escrita à mão (`HAND_WRITTEN_BANK_NAMES`);
  - `BankNameMatcherTest`: prefixos e sufixos (cada entrada carregada sai), limites 5/6 e 9/10, esqueleto vazio, lista vazia e nomes de `KnownBanks.apps` que só casam entre apps do mesmo banco. Este é o único teste da comparação que lê a lista de produção, de propósito: uma entrada nova que colida com outra instituição o derruba.
- Coletor de S9, também em JUnit 4 sem Robolectric, com uma lista pequena escrita à mão (Nubank, Itaú e Itaú Empresas):
  - `BankLookalikeCollectorTest`: pacote oficial; pacote de outro app do mesmo banco; nome de banco com outro pacote; pacote com sufixo ou maiúsculas; pacote emprestado (documenta a limitação); nome sem relação; lista ampliada com um banco fictício; lista vazia; conjunto alterado depois; só S9.
- Coletor de S7, também em JUnit 4 sem Robolectric, com uma lista pequena escrita à mão (`com.wallet.one`, `com.wallet.two`):
  - `DefaultPaymentAppCollectorTest`: padrão fora da lista; padrão na lista; app que não é o padrão; pacote com maiúsculas; conjunto alterado depois; lista vazia; só S7.
- `DefaultScanDeviceTest` (JUnit 4, sem Robolectric, falsos escritos no teste) cobre:
  - a ordem das etapas, com coletores em sequência (um coletor que suspende não deixa o próximo começar) e uma gravação no fim;
  - o relatório devolvido igual ao gravado;
  - a união dos sinais;
  - o dispatcher injetado (`CountingDispatcher`);
  - o cancelamento no segundo de três apps: o coletor cancela o job de quem chama e volta normalmente, e o terceiro app não é coletado nem nada é gravado;
  - erros do inventário, de um coletor, dos confiáveis e da gravação;
  - o relógio que avança e o que volta;
  - o inventário vazio;
  - o app confiável verde com score 65;
  - o `defaultPaymentServices` preservado.
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
