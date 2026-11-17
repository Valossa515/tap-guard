package io.github.valossa515.tapguard

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lê o manifesto mesclado do app (o mesmo que vai no APK). Qualquer permissão nova precisa de
 * decisão registrada no `.agent/SKILLS.md` e de entrar na lista abaixo na mesma spec.
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
    fun `main activity is the only exported activity`() {
        val exported =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
                .activities
                .orEmpty()
                .filter { it.exported }
                .map { it.name }

        assertEquals(listOf(MainActivity::class.java.name), exported)
    }
}
