# app

Aplicativo Tap Guard: telas, navegação, vigia agendada, notificações e injeção de dependências.

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Sem Activity, sem Compose, sem Hilt e sem nenhuma permissão: tudo isso entra nas specs que os usam.

## Dependências

- **Módulos:** `core-domain`, `core-detection`, `core-nfc` e `core-data`.
- **Plugins:** `com.android.application` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4.
- **applicationId e namespace:** `io.github.valossa515.tapguard` · minSdk 26 · targetSdk 36 · compileSdk 36.

## Regras

- O manifesto nunca declara `android.permission.INTERNET`. O CI falha se ela aparecer.
- Só entram as permissões listadas em `.agent/SKILLS.md` › Permissions, e só na spec que as nomeia.

## Comandos

```bash
./gradlew :app:build
./gradlew :app:test
./gradlew :app:assembleDebug
```
