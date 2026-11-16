# core-detection

Biblioteca Android com os coletores de sinais sobre as APIs do Android (`PackageManager`, `AccessibilityManager`, `CardEmulation`) e o caso de uso de varredura `ScanDevice`. Implementa `AppInventory` e `SignalCollector` do `core-domain`.

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Nenhuma funcionalidade ainda.

## Dependências

- **Módulos:** `core-domain`.
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4.
- **Namespace:** `io.github.valossa515.tapguard.core.detection` · minSdk 26 · compileSdk 36.

## Regras

- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- Nenhuma lógica de score aqui: os coletores só devolvem sinais.

## Comandos

```bash
./gradlew :core-detection:build
./gradlew :core-detection:test
```
