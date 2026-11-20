package io.github.valossa515.tapguard.core.detection.inventory

import android.Manifest
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.nfc.cardemulation.HostApduService
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
 * [AppInventory] sobre o [PackageManager] e o [Settings.Secure], em quatro etapas, no [ioDispatcher]:
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
 * 4. para cada app de usuário, permissões pedidas, serviços declarados (inclusive desligados) e
 *    instalador. Cada chamada carrega um só app.
 *
 * Os serviços das etapas 2 e 3 são agrupados por pacote; os de pacotes fora da lista (sistema,
 * próprio app, não instalado) são ignorados. Um app que some entre a lista e o detalhe
 * ([PackageManager.NameNotFoundException]) é pulado. Os demais erros do sistema (por exemplo,
 * `RuntimeException` quando o serviço de pacotes morre, ou `SecurityException` se a chave deixar de
 * ser legível) são propagados. O cancelamento é verificado antes de cada app.
 *
 * Quem usa a biblioteca declara `android.permission.QUERY_ALL_PACKAGES` no próprio manifesto: sem
 * ela, a partir da API 30 só os pacotes visíveis aparecem. O módulo não declara permissão nenhuma.
 */
class PackageManagerAppInventory(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : AppInventory {
    private val packageManager: PackageManager = context.packageManager
    private val ownPackageName: String = context.packageName
    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun installedApps(): List<InstalledApp> =
        withContext(ioDispatcher) {
            val userPackages = packageManager.installedPackagesCompat(flags = 0).filter(::isUserApp)
            val cardEmulationServices = cardEmulationServicesByPackage()
            val accessibilityServices = enabledAccessibilityServicesByPackage()
            userPackages.mapNotNull { info ->
                ensureActive()
                readApp(
                    packageName = info.packageName,
                    cardEmulationServices = cardEmulationServices[info.packageName].orEmpty(),
                    enabledAccessibilityServices = accessibilityServices[info.packageName].orEmpty(),
                )
            }
        }

    private fun isUserApp(info: PackageInfo): Boolean {
        val applicationInfo = info.applicationInfo ?: return false
        return info.packageName != ownPackageName && applicationInfo.flags and SYSTEM_FLAGS == 0
    }

    private fun cardEmulationServicesByPackage(): Map<String, Set<String>> =
        packageManager
            .queryIntentServicesCompat(
                Intent(HostApduService.SERVICE_INTERFACE),
                PackageManager.MATCH_DISABLED_COMPONENTS,
            ).mapNotNull { it.serviceInfo }
            .filter { it.permission == Manifest.permission.BIND_NFC_SERVICE }
            .groupBy(keySelector = { it.packageName }, valueTransform = { it.name })
            .mapValues { (_, names) -> names.toSet() }

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
    ): InstalledApp? =
        try {
            toInstalledApp(
                info = packageManager.packageInfoCompat(packageName, DETAIL_FLAGS),
                installerPackageName = packageManager.installerPackageNameCompat(packageName),
                cardEmulationServices = cardEmulationServices,
                enabledAccessibilityServices = enabledAccessibilityServices,
            )
        } catch (expected: PackageManager.NameNotFoundException) {
            null
        }

    private fun toInstalledApp(
        info: PackageInfo,
        installerPackageName: String?,
        cardEmulationServices: Set<String>,
        enabledAccessibilityServices: Set<String>,
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
        )
    }

    private companion object {
        const val SYSTEM_FLAGS = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        const val DETAIL_FLAGS =
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or
                PackageManager.MATCH_DISABLED_COMPONENTS
    }
}
