package io.github.valossa515.tapguard.core.detection.inventory

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.nfc.cardemulation.HostApduService
import android.os.RemoteException
import android.provider.Settings
import io.github.valossa515.tapguard.core.detection.installedPackagesCompat
import io.github.valossa515.tapguard.core.detection.installerPackageNameCompat
import io.github.valossa515.tapguard.core.detection.longVersionCodeCompat
import io.github.valossa515.tapguard.core.detection.packageInfoCompat
import io.github.valossa515.tapguard.core.detection.queryIntentServicesCompat
import io.github.valossa515.tapguard.core.domain.inventory.AppInventory
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * [AppInventory] sobre o [PackageManager], o [Settings.Secure] e o [CardEmulation], em cinco etapas, no
 * [ioDispatcher]:
 * 1. lista leve dos pacotes, de onde saem os apps do sistema (`FLAG_SYSTEM` ou
 *    `FLAG_UPDATED_SYSTEM_APP`, regra 1) e o pacote do próprio app;
 * 2. uma consulta dos serviços de emulação de cartão (HCE) de todos os pacotes: os que respondem a
 *    [HostApduService.SERVICE_INTERFACE] e exigem [Manifest.permission.BIND_NFC_SERVICE], inclusive
 *    desligados. O critério é mais largo que o do sistema NFC de propósito: não exige serviço
 *    exportado, meta-data nem a permissão `NFC` no pacote. Off-host e NFC-F ficam fora (ADR-018);
 * 3. uma leitura de [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES], os serviços de acessibilidade
 *    que o usuário ligou, inclusive um que caiu. Cada entrada passa por
 *    [ComponentName.unflattenFromString] (formatos completo e curto); as vazias e as que voltam
 *    nulas são puladas. Chave nula ou vazia quer dizer nenhum serviço ligado. É também o que o
 *    framework devolve quando o provedor de configurações falha, sem como distinguir (ADR-019);
 * 4. o pagamento padrão: uma segunda consulta de HCE, sem `MATCH_DISABLED_COMPONENTS`, para o
 *    [PackageManager] aplicar o estado atual do serviço e do app (`ServiceInfo.enabled` é o valor do
 *    manifesto e não serve). Para cada serviço ligado e exportado que também está na etapa 2 de um
 *    app de usuário, pergunta [CardEmulation.isDefaultServiceForCategory] com
 *    [CardEmulation.CATEGORY_PAYMENT]. Sem app de usuário com HCE, a etapa não toca no NFC. Sem NFC
 *    ou sem HCE no aparelho, ou com o serviço NFC morto (`RuntimeException` com causa
 *    [RemoteException]), todos os apps ficam sem serviço padrão, sem erro (ADR-021);
 * 5. para cada app de usuário, permissões pedidas, serviços declarados (inclusive desligados) e
 *    instalador. Cada chamada carrega um só app.
 *
 * Os serviços das etapas 2 a 4 são agrupados por pacote; os de pacotes fora da lista (sistema,
 * próprio app, não instalado) são ignorados. Um app que some entre a lista e o detalhe
 * ([PackageManager.NameNotFoundException]) é pulado. Os demais erros do sistema (por exemplo,
 * `RuntimeException` quando o serviço de pacotes morre, `SecurityException` se a chave deixar de ser
 * legível ou se o app não tiver a permissão `android.permission.NFC`) são propagados. O cancelamento
 * é verificado antes de cada app.
 *
 * Quem usa a biblioteca declara `android.permission.QUERY_ALL_PACKAGES` no próprio manifesto: sem
 * ela, a partir da API 30 só os pacotes visíveis aparecem. Declara também `android.permission.NFC`,
 * que o sistema NFC exige na etapa 4. O módulo não declara permissão nenhuma.
 */
class PackageManagerAppInventory(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : AppInventory {
    private val packageManager: PackageManager = context.packageManager
    private val ownPackageName: String = context.packageName
    private val contentResolver: ContentResolver = context.contentResolver
    private val applicationContext: Context = context.applicationContext

    override suspend fun installedApps(): List<InstalledApp> =
        withContext(ioDispatcher) {
            val userPackages = packageManager.installedPackagesCompat(flags = 0).filter(::isUserApp)
            val cardEmulationServices = cardEmulationServicesByPackage()
            val accessibilityServices = enabledAccessibilityServicesByPackage()
            val userCardEmulationServices =
                userPackages
                    .associate { it.packageName to cardEmulationServices[it.packageName].orEmpty() }
                    .filterValues { it.isNotEmpty() }
            val defaultPaymentServices = defaultPaymentServicesByPackage(userCardEmulationServices)
            userPackages.mapNotNull { info ->
                ensureActive()
                readApp(
                    packageName = info.packageName,
                    cardEmulationServices = cardEmulationServices[info.packageName].orEmpty(),
                    enabledAccessibilityServices = accessibilityServices[info.packageName].orEmpty(),
                    defaultPaymentServices = defaultPaymentServices[info.packageName].orEmpty(),
                )
            }
        }

    private fun isUserApp(info: PackageInfo): Boolean {
        val applicationInfo = info.applicationInfo ?: return false
        return info.packageName != ownPackageName && applicationInfo.flags and SYSTEM_FLAGS == 0
    }

    private fun hceServices(flags: Int): List<ServiceInfo> =
        packageManager
            .queryIntentServicesCompat(Intent(HostApduService.SERVICE_INTERFACE), flags)
            .mapNotNull { it.serviceInfo }
            .filter { it.permission == Manifest.permission.BIND_NFC_SERVICE }

    private fun cardEmulationServicesByPackage(): Map<String, Set<String>> =
        hceServices(PackageManager.MATCH_DISABLED_COMPONENTS)
            .groupBy(keySelector = { it.packageName }, valueTransform = { it.name })
            .mapValues { (_, names) -> names.toSet() }

    private fun defaultPaymentServicesByPackage(userServices: Map<String, Set<String>>): Map<String, Set<String>> {
        val cardEmulation = if (userServices.isEmpty()) null else cardEmulationOrNull()
        return if (cardEmulation == null) {
            emptyMap()
        } else {
            val candidates =
                hceServices(flags = 0)
                    .filter { it.exported && it.name in userServices[it.packageName].orEmpty() }
                    .map { ComponentName(it.packageName, it.name) }
            askDefaultPaymentServices(cardEmulation, candidates)
        }
    }

    private fun cardEmulationOrNull(): CardEmulation? {
        val adapter = NfcAdapter.getDefaultAdapter(applicationContext) ?: return null
        return try {
            CardEmulation.getInstance(adapter)
        } catch (expected: UnsupportedOperationException) {
            null
        }
    }

    private fun askDefaultPaymentServices(
        cardEmulation: CardEmulation,
        candidates: List<ComponentName>,
    ): Map<String, Set<String>> =
        try {
            candidates
                .filter { cardEmulation.isDefaultServiceForCategory(it, CardEmulation.CATEGORY_PAYMENT) }
                .groupBy(keySelector = { it.packageName }, valueTransform = { it.className })
                .mapValues { (_, names) -> names.toSet() }
        } catch (expected: RuntimeException) {
            if (expected.cause !is RemoteException) throw expected
            emptyMap()
        }

    private fun enabledAccessibilityServicesByPackage(): Map<String, Set<String>> =
        Settings.Secure
            .getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
            .split(':')
            .filter { it.isNotEmpty() }
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .groupBy(keySelector = { it.packageName }, valueTransform = { it.className })
            .mapValues { (_, names) -> names.toSet() }

    private fun readApp(
        packageName: String,
        cardEmulationServices: Set<String>,
        enabledAccessibilityServices: Set<String>,
        defaultPaymentServices: Set<String>,
    ): InstalledApp? =
        try {
            toInstalledApp(
                info = packageManager.packageInfoCompat(packageName, DETAIL_FLAGS),
                installerPackageName = packageManager.installerPackageNameCompat(packageName),
                cardEmulationServices = cardEmulationServices,
                enabledAccessibilityServices = enabledAccessibilityServices,
                defaultPaymentServices = defaultPaymentServices,
            )
        } catch (expected: PackageManager.NameNotFoundException) {
            null
        }

    private fun toInstalledApp(
        info: PackageInfo,
        installerPackageName: String?,
        cardEmulationServices: Set<String>,
        enabledAccessibilityServices: Set<String>,
        defaultPaymentServices: Set<String>,
    ): InstalledApp? {
        val applicationInfo = info.applicationInfo ?: return null
        return InstalledApp(
            packageName = info.packageName,
            label = applicationInfo.loadLabel(packageManager).toString(),
            versionCode = info.longVersionCodeCompat,
            installerPackageName = installerPackageName,
            firstInstallTime = Instant.ofEpochMilli(info.firstInstallTime),
            lastUpdateTime = Instant.ofEpochMilli(info.lastUpdateTime),
            requestedPermissions = info.requestedPermissions.orEmpty().toSet(),
            services =
                info.services
                    .orEmpty()
                    .map { it.name }
                    .toSet(),
            cardEmulationServices = cardEmulationServices,
            enabledAccessibilityServices = enabledAccessibilityServices,
            defaultPaymentServices = defaultPaymentServices,
        )
    }

    private companion object {
        const val SYSTEM_FLAGS = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        const val DETAIL_FLAGS =
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or
                PackageManager.MATCH_DISABLED_COMPONENTS
    }
}
