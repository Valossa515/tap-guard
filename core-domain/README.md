# core-domain

Domínio do Tap Guard em Kotlin puro: modelos (app, sinal, score, nível), motor de score e interfaces dos coletores (`AppInventory`, `SignalCollector`, `CardReader`).

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Nenhuma funcionalidade ainda.

## Dependências

- **Módulos:** nenhum.
- **Plugins:** só `kotlin("jvm")` (mais ktlint e detekt). Nenhum plugin ou dependência Android ou AndroidX.
- **Testes:** JUnit 5 (com `junit-platform-launcher`, exigido pelo Gradle 9).

## Regras

- Nenhuma API do Android aqui dentro.
- Toda a lógica de score fica neste módulo.
- Não pode depender do `app` (vai fazer parte do SDK para bancos).

## Comandos

```bash
./gradlew :core-domain:build
./gradlew :core-domain:test
```
