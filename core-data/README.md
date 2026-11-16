# core-data

Biblioteca Android com o banco local (Room), as preferências (DataStore), os repositórios e o histórico de varreduras, decisões e alertas.

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Room e DataStore entram nas specs que os usam.

## Dependências

- **Módulos:** `core-domain`.
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4.
- **Namespace:** `io.github.valossa515.tapguard.core.data` · minSdk 26 · compileSdk 36.

## Regras

- Nenhum dado de cartão entra no banco nem nas preferências.

## Comandos

```bash
./gradlew :core-data:build
./gradlew :core-data:test
```
