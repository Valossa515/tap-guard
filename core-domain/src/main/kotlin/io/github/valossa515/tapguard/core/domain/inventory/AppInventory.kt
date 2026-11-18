package io.github.valossa515.tapguard.core.domain.inventory

/**
 * Inventário dos apps instalados. Devolve só apps que não são do sistema (RF-101): é aqui que a
 * regra 1 da seção 5 se cumpre, com teste na SPEC-011.
 */
interface AppInventory {
    suspend fun installedApps(): List<InstalledApp>
}
