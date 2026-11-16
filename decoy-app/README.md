# decoy-app

App isca inofensivo com o perfil de um app de golpe. É o alvo dos testes de detecção: nunca se usa malware real.

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. As variantes (`nfcOnly`, `hce`, `full`, `lookalike`) entram na Etapa 2.

## Dependências

- **Módulos:** nenhum.
- **Plugins:** `com.android.application` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4.
- **applicationId e namespace:** `io.github.valossa515.tapguard.decoy` · minSdk 26 · targetSdk 36 · compileSdk 36.

## Regras

- Inofensivo e sem `android.permission.INTERNET`.
- É instalado por `adb install`, para simular origem fora da loja.

## Comandos

```bash
./gradlew :decoy-app:build
./gradlew :decoy-app:test
./gradlew :decoy-app:assembleDebug
```
