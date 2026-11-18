package io.github.valossa515.tapguard.core.domain.inventory

/**
 * Inventário dos apps instalados. Devolve só apps que não são do sistema (RF-101): é aqui que a
 * regra 1 da seção 5 se cumpre. Também não devolve o próprio app que faz a consulta. A ordem da
 * lista não é garantida.
 *
 * Um app que some durante a leitura é pulado, sem erro. O cancelamento de quem chama é lançado
 * como `CancellationException`, e os erros da fonte de pacotes são propagados para quem chama.
 */
interface AppInventory {
    suspend fun installedApps(): List<InstalledApp>
}
