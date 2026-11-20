package io.github.valossa515.tapguard.core.domain

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import io.github.valossa515.tapguard.core.domain.risk.RiskResult
import io.github.valossa515.tapguard.core.domain.signal.Signal
import java.time.Instant

/**
 * Repassa à fábrica do [InstalledApp], uma data class, com valores padrão, e só existe no
 * `testFixtures`. Por isso o `@Suppress("LongParameterList")`: é a exceção do ADR-015 estendida pelo
 * ADR-019 a fixtures que repassam à fábrica de uma data class, com valores padrão, e não vale para
 * outras funções de teste.
 */
@Suppress("LongParameterList")
fun anInstalledApp(
    packageName: String = "com.example.app",
    installerPackageName: String? = "com.android.vending",
    firstInstallTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    lastUpdateTime: Instant = Instant.parse("2026-01-02T00:00:00Z"),
    requestedPermissions: Set<String> = emptySet(),
    services: Set<String> = emptySet(),
    cardEmulationServices: Set<String> = emptySet(),
    enabledAccessibilityServices: Set<String> = emptySet(),
): InstalledApp =
    InstalledApp(
        packageName = packageName,
        label = "Example",
        versionCode = 1L,
        installerPackageName = installerPackageName,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        requestedPermissions = requestedPermissions,
        services = services,
        cardEmulationServices = cardEmulationServices,
        enabledAccessibilityServices = enabledAccessibilityServices,
    )

fun aRiskResult(
    score: Int = 0,
    level: RiskLevel = RiskLevel.GREEN,
    signals: Set<Signal> = emptySet(),
): RiskResult = RiskResult(score = score, level = level, signals = signals)
