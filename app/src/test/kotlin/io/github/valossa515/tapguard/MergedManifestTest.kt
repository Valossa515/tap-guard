package io.github.valossa515.tapguard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.annotation.XmlRes
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

/**
 * Lê o manifesto mesclado do app (o mesmo que vai no APK). `QUERY_ALL_PACKAGES` tem de estar
 * declarada, porque o inventário depende dela. Qualquer permissão ou componente
 * exportado novo precisa de decisão registrada no `.agent/SKILLS.md` e de entrar na lista abaixo
 * na mesma spec. A ligação das regras de backup no manifesto é conferida pelo CI ("Regras do projeto").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MergedManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `requested permissions are within the explicit allowlist`() {
        val allowed =
            setOf(
                "android.permission.QUERY_ALL_PACKAGES",
                "android.permission.NFC",
                "android.permission.POST_NOTIFICATIONS",
                // Trazida pelo androidx.core; de nível signature, só o próprio app a tem.
                "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            )

        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
                .orEmpty()

        val notAllowed = requested - allowed
        assertTrue("Permissões fora da lista: $notAllowed", notAllowed.isEmpty())
    }

    @Test
    fun `query all packages is requested`() {
        // Sem ela, a partir da API 30 o inventário só enxerga os pacotes visíveis (ADR-016).
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
                .orEmpty()

        assertTrue("QUERY_ALL_PACKAGES não declarada", "android.permission.QUERY_ALL_PACKAGES" in requested)
    }

    @Test
    fun `application class is tap guard application`() {
        assertEquals(TapGuardApplication::class.java.name, context.applicationInfo.className)
    }

    @Test
    fun `exported components are main activity and profile install receiver`() {
        val flags =
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        val components =
            info.activities.orEmpty().toList() + info.services.orEmpty() +
                info.receivers.orEmpty() + info.providers.orEmpty()

        val exported = components.filter { it.exported }.map { it.name }.toSet()

        assertEquals(
            setOf(
                MainActivity::class.java.name,
                // Do androidx.profileinstaller; protegido por android.permission.DUMP.
                "androidx.profileinstaller.ProfileInstallReceiver",
            ),
            exported,
        )
    }

    @Test
    fun `backup is disabled`() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun `data extraction rules exclude every domain in cloud backup and device transfer`() {
        val excluded = excludedDomainsBySection(R.xml.data_extraction_rules)

        assertEquals(ALL_DOMAINS, excluded["cloud-backup"])
        assertEquals(ALL_DOMAINS, excluded["device-transfer"])
    }

    @Test
    fun `full backup content excludes every domain`() {
        val excluded = excludedDomainsBySection(R.xml.full_backup_content)

        assertEquals(ALL_DOMAINS, excluded["full-backup-content"])
    }

    /** Domínios de cada `<exclude>`, agrupados pela seção em que aparecem. */
    private fun excludedDomainsBySection(
        @XmlRes rules: Int,
    ): Map<String, Set<String>> {
        val parser = context.resources.getXml(rules)
        val excluded = mutableMapOf<String, MutableSet<String>>()
        var section = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name == "exclude") {
                    excluded.getOrPut(section) { mutableSetOf() }.add(parser.getAttributeValue(null, "domain"))
                } else {
                    section = parser.name
                }
            }
        }
        return excluded
    }

    private companion object {
        val ALL_DOMAINS =
            setOf(
                "root",
                "file",
                "database",
                "sharedpref",
                "external",
                "device_root",
                "device_file",
                "device_database",
                "device_sharedpref",
            )
    }
}
