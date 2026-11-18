package io.github.valossa515.tapguard.core.detection.inventory

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.github.valossa515.tapguard.core.detection.installedPackagesCompat
import io.github.valossa515.tapguard.core.detection.installerPackageNameCompat
import io.github.valossa515.tapguard.core.detection.longVersionCodeCompat
import io.github.valossa515.tapguard.core.detection.packageInfoCompat
import io.github.valossa515.tapguard.core.domain.inventory.AppInventory
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * [AppInventory] sobre o [PackageManager], em duas etapas, no [ioDispatcher]:
 * 1. lista leve dos pacotes, de onde saem os apps do sistema (`FLAG_SYSTEM` ou
 *    `FLAG_UPDATED_SYSTEM_APP`, regra 1) e o pacote do próprio app;
 * 2. para cada app de usuário, permissões pedidas, serviços declarados (inclusive desligados) e
 *    instalador. Cada chamada carrega um só app.
 *
 * Um app que some entre a lista e o detalhe ([PackageManager.NameNotFoundException]) é pulado. Os
 * demais erros do sistema (por exemplo, `RuntimeException` quando o serviço de pacotes morre) são
 * propagados. O cancelamento é verificado antes de cada app.
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

    override suspend fun installedApps(): List<InstalledApp> =
        withContext(ioDispatcher) {
            packageManager
                .installedPackagesCompat(flags = 0)
                .filter(::isUserApp)
                .mapNotNull { info ->
                    ensureActive()
                    readApp(info.packageName)
                }
        }

    private fun isUserApp(info: PackageInfo): Boolean {
        val applicationInfo = info.applicationInfo ?: return false
        return info.packageName != ownPackageName && applicationInfo.flags and SYSTEM_FLAGS == 0
    }

    private fun readApp(packageName: String): InstalledApp? =
        try {
            toInstalledApp(
                info = packageManager.packageInfoCompat(packageName, DETAIL_FLAGS),
                installerPackageName = packageManager.installerPackageNameCompat(packageName),
            )
        } catch (expected: PackageManager.NameNotFoundException) {
            null
        }

    private fun toInstalledApp(
        info: PackageInfo,
        installerPackageName: String?,
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
        )
    }

    private companion object {
        const val SYSTEM_FLAGS = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        const val DETAIL_FLAGS =
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or
                PackageManager.MATCH_DISABLED_COMPONENTS
    }
}
