package io.github.valossa515.tapguard.core.detection.inventory

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Inventário contra o `PackageManager` do Robolectric. Cada cenário roda nos cinco níveis de API:
 * a 26 cobre os ramos antigos; a 28, a 30 e a 33 são o primeiro nível de cada ramo novo (código de
 * versão longo, origem da instalação, `PackageInfoFlags`); a 36 é o targetSdk.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 28, 30, 33, 36])
class PackageManagerAppInventoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager
    private val shadowPackageManager = shadowOf(packageManager)

    @Test
    fun `returns user app and skips app with flag system`() =
        runTest {
            install(aPackageInfo(USER_APP))
            install(aPackageInfo(SYSTEM_APP, flags = ApplicationInfo.FLAG_SYSTEM))

            assertEquals(listOf(USER_APP), inventory().installedApps().map { it.packageName })
        }

    @Test
    fun `skips updated system app with both flags`() =
        runTest {
            install(aPackageInfo(USER_APP))
            install(
                aPackageInfo(
                    SYSTEM_APP,
                    flags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                ),
            )

            assertEquals(listOf(USER_APP), inventory().installedApps().map { it.packageName })
        }

    @Test
    fun `skips app with only flag updated system app`() =
        runTest {
            install(aPackageInfo(USER_APP))
            install(aPackageInfo(SYSTEM_APP, flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))

            assertEquals(listOf(USER_APP), inventory().installedApps().map { it.packageName })
        }

    @Test
    fun `skips own package`() =
        runTest {
            val listed = packageManager.getInstalledPackages(0).map { it.packageName }
            assertTrue("O Robolectric registra o próprio pacote", context.packageName in listed)
            install(aPackageInfo(USER_APP))

            val apps = inventory().installedApps().map { it.packageName }

            assertEquals(listOf(USER_APP), apps)
        }

    @Test
    fun `fills label permissions services dates and installer`() =
        runTest {
            val info =
                aPackageInfo(USER_APP, label = "Carteira Exemplo").apply {
                    requestedPermissions = arrayOf(NFC_PERMISSION, SMS_PERMISSION)
                    services = arrayOf(aService(USER_APP, HCE_SERVICE))
                    firstInstallTime = FIRST_INSTALL_MILLIS
                    lastUpdateTime = LAST_UPDATE_MILLIS
                }
            install(info, installer = PLAY_STORE)

            val app = inventory().installedApps().single()

            assertEquals(USER_APP, app.packageName)
            assertEquals("Carteira Exemplo", app.label)
            assertEquals(setOf(NFC_PERMISSION, SMS_PERMISSION), app.requestedPermissions)
            assertEquals(setOf(HCE_SERVICE), app.services)
            assertEquals(Instant.ofEpochMilli(FIRST_INSTALL_MILLIS), app.firstInstallTime)
            assertEquals(Instant.ofEpochMilli(LAST_UPDATE_MILLIS), app.lastUpdateTime)
            assertEquals(PLAY_STORE, app.installerPackageName)
        }

    @Test
    fun `keeps long version code from api 28 and int version code on api 26`() =
        runTest {
            val expected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) LONG_VERSION_CODE else INT_VERSION_CODE
            install(aPackageInfo(USER_APP).withVersionCode(expected))

            assertEquals(expected, inventory().installedApps().single().versionCode)
        }

    @Test
    fun `reads installer from install source info on api 30 and installer package name below`() =
        runTest {
            install(aPackageInfo(USER_APP), installer = PLAY_STORE)
            install(aPackageInfo(OTHER_APP), installer = OTHER_STORE)

            val installers = inventory().installedApps().associate { it.packageName to it.installerPackageName }

            assertEquals(mapOf(USER_APP to PLAY_STORE, OTHER_APP to OTHER_STORE), installers)
        }

    @Test
    fun `returns null installer when none is registered`() =
        runTest {
            install(aPackageInfo(USER_APP), installer = null)

            assertNull(inventory().installedApps().single().installerPackageName)
        }

    @Test
    fun `includes disabled services`() =
        runTest {
            val info =
                aPackageInfo(USER_APP).apply {
                    services =
                        arrayOf(
                            aService(USER_APP, HCE_SERVICE),
                            aService(USER_APP, DISABLED_SERVICE, enabled = false),
                        )
                }
            install(info)

            assertEquals(setOf(HCE_SERVICE, DISABLED_SERVICE), inventory().installedApps().single().services)
        }

    @Test
    fun `returns empty sets without permissions or services`() =
        runTest {
            install(aPackageInfo(USER_APP))

            val app = inventory().installedApps().single()

            assertEquals(emptySet<String>(), app.requestedPermissions)
            assertEquals(emptySet<String>(), app.services)
        }

    @Test
    fun `includes disabled app`() =
        runTest {
            install(aPackageInfo(USER_APP).apply { applicationInfo?.enabled = false })

            assertEquals(listOf(USER_APP), inventory().installedApps().map { it.packageName })
        }

    @Test
    fun `skips app uninstalled between list and detail`() =
        runTest {
            val firstLabel = SecondReadHook("Primeiro") { shadowPackageManager.removePackage(OTHER_APP) }
            install(aPackageInfo(USER_APP, label = firstLabel))
            install(aPackageInfo(OTHER_APP))
            install(aPackageInfo(THIRD_APP))

            val apps = inventory().installedApps().map { it.packageName }

            assertEquals(listOf(USER_APP, THIRD_APP), apps)
        }

    @Test
    fun `cancellation stops before next app`() =
        runTest {
            lateinit var call: Deferred<List<InstalledApp>>
            val labels =
                listOf(
                    SecondReadHook("Primeiro") { call.cancel() },
                    SecondReadHook("Segundo") {},
                    SecondReadHook("Terceiro") {},
                )
            listOf(USER_APP, OTHER_APP, THIRD_APP).zip(labels).forEach { (packageName, label) ->
                install(aPackageInfo(packageName, label = label))
            }
            val inventory = inventory()

            call = async { inventory.installedApps() }
            val result = runCatching { call.await() }

            assertTrue(result.exceptionOrNull() is CancellationException)
            // Uma leitura na cópia da lista leve; só o primeiro app teve o detalhe lido.
            assertEquals(listOf(2, 1, 1), labels.map { it.reads })
        }

    @Test
    fun `read runs on the injected dispatcher`() =
        runTest {
            install(aPackageInfo(USER_APP))
            val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

            val apps = PackageManagerAppInventory(context, dispatcher).installedApps()

            assertEquals(listOf(USER_APP), apps.map { it.packageName })
            assertTrue("A leitura não passou pelo dispatcher injetado", dispatcher.dispatches > 0)
        }

    @Test
    fun `returns empty list without user apps`() =
        runTest {
            install(aPackageInfo(SYSTEM_APP, flags = ApplicationInfo.FLAG_SYSTEM))

            assertEquals(emptyList<InstalledApp>(), inventory().installedApps())
        }

    private fun TestScope.inventory(): PackageManagerAppInventory =
        PackageManagerAppInventory(context, StandardTestDispatcher(testScheduler))

    /**
     * Instala o pacote e registra o instalador pelo caminho do nível de API: a partir da API 30 o
     * inventário lê a origem da instalação, e o Robolectric lança `NameNotFoundException` para
     * pacote sem origem registrada.
     */
    private fun install(
        info: PackageInfo,
        installer: String? = null,
    ) {
        shadowPackageManager.installPackage(info)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            shadowPackageManager.setInstallSourceInfo(info.packageName, null, installer)
        } else {
            packageManager.setInstallerPackageName(info.packageName, installer)
        }
    }

    /**
     * Repassa ao [delegate] (o `StandardTestDispatcher` do próprio `runTest`) e conta os despachos.
     * Prova que a leitura passou pelo dispatcher injetado: se o inventário não fizer o
     * `withContext` nele, não há despacho e o teste falha (verificado trocando, na produção, o
     * dispatcher injetado por `Dispatchers.Unconfined`). Não usa um segundo
     * `TestCoroutineScheduler`: o `runTest` o recusa.
     */
    private class CountingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatches = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches++
            delegate.dispatch(context, block)
        }
    }

    /**
     * Rótulo que executa [onSecondRead] na segunda vez que é lido. O Robolectric copia (Parcel) o
     * `PackageInfo` que devolve: a primeira leitura acontece na cópia da lista leve, e a segunda,
     * na cópia do detalhe deste app. Assim a ação roda durante a leitura deste app, antes do
     * próximo. Se uma versão do Robolectric parar de copiar, a ação roda em outro momento ou
     * nunca, e os testes que usam este gancho falham, em vez de passar por engano.
     *
     * Os testes também dependem da ordem da lista: os nomes dos pacotes (`com.example.a…`,
     * `com.example.b…`, `com.example.c…`) foram escolhidos para a ordem alfabética coincidir com a
     * de instalação.
     */
    private class SecondReadHook(
        private val text: String,
        private val onSecondRead: () -> Unit,
    ) : CharSequence {
        var reads = 0
            private set

        override val length: Int get() = text.length

        override fun get(index: Int): Char = text[index]

        override fun subSequence(
            startIndex: Int,
            endIndex: Int,
        ): CharSequence = text.subSequence(startIndex, endIndex)

        override fun toString(): String {
            reads++
            if (reads == 2) onSecondRead()
            return text
        }
    }

    private companion object {
        const val USER_APP = "com.example.a.user"
        const val OTHER_APP = "com.example.b.other"
        const val THIRD_APP = "com.example.c.third"
        const val SYSTEM_APP = "com.example.system"
        const val PLAY_STORE = "com.android.vending"
        const val OTHER_STORE = "com.example.store"
        const val NFC_PERMISSION = "android.permission.NFC"
        const val SMS_PERMISSION = "android.permission.RECEIVE_SMS"
        const val HCE_SERVICE = "com.example.a.user.PaymentService"
        const val DISABLED_SERVICE = "com.example.a.user.DisabledService"
        const val FIRST_INSTALL_MILLIS = 1_767_225_600_000L
        const val LAST_UPDATE_MILLIS = 1_767_312_000_000L
        const val LONG_VERSION_CODE = 5_000_000_000L
        const val INT_VERSION_CODE = 7L

        fun aPackageInfo(
            packageName: String,
            flags: Int = 0,
            label: CharSequence = "Exemplo",
        ): PackageInfo =
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo =
                    ApplicationInfo().apply {
                        this.packageName = packageName
                        this.flags = flags
                        nonLocalizedLabel = label
                    }
            }

        fun aService(
            packageName: String,
            name: String,
            enabled: Boolean = true,
        ): ServiceInfo =
            ServiceInfo().apply {
                this.packageName = packageName
                this.name = name
                this.enabled = enabled
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
            }

        /** Abaixo da API 28 só existe o `versionCode` de 32 bits (ramo antigo, ADR-016). */
        fun PackageInfo.withVersionCode(value: Long): PackageInfo =
            apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    longVersionCode = value
                } else {
                    @Suppress("DEPRECATION")
                    versionCode = value.toInt()
                }
            }
    }
}
