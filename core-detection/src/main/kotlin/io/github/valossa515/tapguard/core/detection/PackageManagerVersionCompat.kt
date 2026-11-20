package io.github.valossa515.tapguard.core.detection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/*
 * Ramos por versão do Android para o PackageManager. É o único lugar do código de produção com
 * @Suppress("DEPRECATION"), e só no ramo antigo de cada corte, cujo outro ramo usa a API nova
 * (ADR-016).
 */

/**
 * Pacotes instalados, com as flags de [PackageManager] (as do tipo `PackageInfoFlags` a partir da
 * API 33).
 *
 * O `@SuppressLint("QueryPermissionsNeeded")` existe porque, sem `QUERY_ALL_PACKAGES`, a partir da
 * API 30 a lista só traz os pacotes visíveis. A permissão fica no manifesto de quem usa a
 * biblioteca (o app Tap Guard ou o app do banco), não no do módulo (ADR-016).
 */
@SuppressLint("QueryPermissionsNeeded")
internal fun PackageManager.installedPackagesCompat(flags: Int): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getInstalledPackages(flags)
    }

/**
 * Serviços que respondem a [intent], com as flags de [PackageManager] (as do tipo
 * `ResolveInfoFlags` a partir da API 33).
 *
 * O `@SuppressLint("QueryPermissionsNeeded")` existe pelo mesmo motivo do de
 * [installedPackagesCompat]: a partir da API 30 a lista só traz os pacotes visíveis, e a permissão
 * fica no manifesto de quem usa a biblioteca (ADR-016 e ADR-018).
 */
@SuppressLint("QueryPermissionsNeeded")
internal fun PackageManager.queryIntentServicesCompat(
    intent: Intent,
    flags: Int,
): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        queryIntentServices(intent, flags)
    }

/** Detalhe de um pacote. Lança [PackageManager.NameNotFoundException] se ele não existe mais. */
internal fun PackageManager.packageInfoCompat(
    packageName: String,
    flags: Int,
): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }

/**
 * Pacote que instalou o app, ou nulo se o sistema não informa. Lança
 * [PackageManager.NameNotFoundException] se o pacote não existe mais.
 */
internal fun PackageManager.installerPackageNameCompat(packageName: String): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getInstallSourceInfo(packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        getInstallerPackageName(packageName)
    }

/** Código de versão longo; abaixo da API 28 só existe o `versionCode` de 32 bits. */
internal val PackageInfo.longVersionCodeCompat: Long
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
