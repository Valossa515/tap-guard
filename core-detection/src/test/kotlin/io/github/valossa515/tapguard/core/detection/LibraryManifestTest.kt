package io.github.valossa515.tapguard.core.detection

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lê o manifesto mesclado da biblioteca com as dependências (`isIncludeAndroidResources`). O módulo
 * vai virar SDK para bancos, e toda permissão declarada aqui seria herdada pelo app que o usar:
 * `QUERY_ALL_PACKAGES` fica só no manifesto do `app` (ADR-016).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryManifestTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `library requests no permission besides the one androidx test brings`() {
        // Trazida pelo androidx.test:core, dependência só de teste; não chega a quem usa o SDK.
        val allowed = setOf("android.permission.REORDER_TASKS")

        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
                .orEmpty()

        val notAllowed = requested - allowed
        assertTrue("Permissões declaradas pela biblioteca: $notAllowed", notAllowed.isEmpty())
    }
}
