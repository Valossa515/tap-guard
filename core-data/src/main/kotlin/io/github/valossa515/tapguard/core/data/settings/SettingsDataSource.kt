package io.github.valossa515.tapguard.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Preferências do app (especificação, seção 9), com os padrões quando nunca foram gravadas.
 *
 * Arquivo ilegível ou corrompido (`IOException`, inclusive a `CorruptionException` do DataStore):
 * as leituras emitem as preferências vazias, e valem os padrões (comentário da #14, ADR-023). Outra
 * exceção é propagada. As gravações não mudam: com o arquivo corrompido, elas ainda falham.
 *
 * Fonte fina, sem regra: a validação de [scanIntervalHours] (1, 6, 12 ou 24) e a interface de
 * repositório no `core-domain` ficam para a primeira spec que usar preferências. Nenhum dado de
 * cartão entra aqui.
 */
class SettingsDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private val preferences: Flow<Preferences> =
            dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

        val onboardingDone: Flow<Boolean> =
            preferences.map { it[ONBOARDING_DONE] ?: DEFAULT_ONBOARDING_DONE }

        val monitoringEnabled: Flow<Boolean> =
            preferences.map { it[MONITORING_ENABLED] ?: DEFAULT_MONITORING_ENABLED }

        val scanIntervalHours: Flow<Int> =
            preferences.map { it[SCAN_INTERVAL_HOURS] ?: DEFAULT_SCAN_INTERVAL_HOURS }

        /** Componente do app de pagamento padrão na última verificação; nulo se nunca foi registrado. */
        val lastDefaultPaymentComponent: Flow<String?> =
            preferences.map { it[LAST_DEFAULT_PAYMENT_COMPONENT] }

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
