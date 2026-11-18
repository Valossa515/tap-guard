package io.github.valossa515.tapguard.core.domain.scan

/** O que disparou uma varredura. Os nomes são os valores gravados em `scan_run.trigger`. */
enum class ScanTrigger {
    MANUAL,
    APP_OPEN,
    PERIODIC,
}
