package io.github.valossa515.tapguard.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Preferências do app (especificação, seção 9), com os padrões quando nunca foram gravadas.
 * Fonte fina, sem regra: a validação de [scanIntervalHours] (1, 6, 12 ou 24) e o repositório
 * ficam para a SPEC-018. Nenhum dado de cartão entra aqui.
 */
class SettingsDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val onboardingDone: Flow<Boolean> =
            dataStore.data.map { it[ONBOARDING_DONE] ?: DEFAULT_ONBOARDING_DONE }

        val monitoringEnabled: Flow<Boolean> =
            dataStore.data.map { it[MONITORING_ENABLED] ?: DEFAULT_MONITORING_ENABLED }

        val scanIntervalHours: Flow<Int> =
            dataStore.data.map { it[SCAN_INTERVAL_HOURS] ?: DEFAULT_SCAN_INTERVAL_HOURS }

        /** Componente do app de pagamento padrão na última verificação; nulo se nunca foi registrado. */
        val lastDefaultPaymentComponent: Flow<String?> =
            dataStore.data.map { it[LAST_DEFAULT_PAYMENT_COMPONENT] }

        suspend fun setOnboardingDone(done: Boolean) {
            dataStore.edit { it[ONBOARDING_DONE] = done }
        }

        suspend fun setMonitoringEnabled(enabled: Boolean) {
            dataStore.edit { it[MONITORING_ENABLED] = enabled }
        }

        suspend fun setScanIntervalHours(hours: Int) {
            dataStore.edit { it[SCAN_INTERVAL_HOURS] = hours }
        }

        /** Grava o componente; `null` apaga o registro. */
        suspend fun setLastDefaultPaymentComponent(component: String?) {
            dataStore.edit {
                if (component == null) {
                    it.remove(LAST_DEFAULT_PAYMENT_COMPONENT)
                } else {
                    it[LAST_DEFAULT_PAYMENT_COMPONENT] = component
                }
            }
        }

        companion object {
            const val DATASTORE_NAME = "settings"
            const val DEFAULT_ONBOARDING_DONE = false
            const val DEFAULT_MONITORING_ENABLED = true
            const val DEFAULT_SCAN_INTERVAL_HOURS = 6

            private val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
            private val MONITORING_ENABLED = booleanPreferencesKey("monitoringEnabled")
            private val SCAN_INTERVAL_HOURS = intPreferencesKey("scanIntervalHours")
            private val LAST_DEFAULT_PAYMENT_COMPONENT = stringPreferencesKey("lastDefaultPaymentComponent")
        }
    }
