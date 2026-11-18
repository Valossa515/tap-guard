package io.github.valossa515.tapguard.core.domain

import io.github.valossa515.tapguard.core.domain.inventory.InstalledApp
import io.github.valossa515.tapguard.core.domain.risk.RiskLevel
import io.github.valossa515.tapguard.core.domain.risk.RiskResult
import io.github.valossa515.tapguard.core.domain.signal.Signal
import java.time.Instant

fun anInstalledApp(
    packageName: String = "com.example.app",
    installerPackageName: String? = "com.android.vending",
    requestedPermissions: Set<String> = emptySet(),
    services: Set<String> = emptySet(),
): InstalledApp =
    InstalledApp(
        packageName = packageName,
        label = "Example",
        versionCode = 1L,
        installerPackageName = installerPackageName,
        firstInstallTime = Instant.parse("2026-01-01T00:00:00Z"),
        lastUpdateTime = Instant.parse("2026-01-02T00:00:00Z"),
        requestedPermissions = requestedPermissions,
        services = services,
    )

fun aRiskResult(
    score: Int = 0,
    level: RiskLevel = RiskLevel.GREEN,
    signals: Set<Signal> = emptySet(),
): RiskResult = RiskResult(score = score, level = level, signals = signals)
