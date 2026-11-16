# core-domain

Domínio do Tap Guard em Kotlin puro: modelos (app, sinal, score, nível), motor de score e interfaces dos coletores (`AppInventory`, `SignalCollector`, `CardReader`).

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Nenhuma funcionalidade ainda.

## Dependências

- **Módulos:** nenhum.
- **Plugins:** só `kotlin("jvm")` (mais ktlint e detekt). Nenhum plugin ou dependência Android ou AndroidX.
- **Testes:** JUnit 5 (com `junit-platform-launcher`, exigido pelo Gradle 9).
- **Cobertura:** Kover, com no mínimo 80% de cobertura de linhas (RNF-08). O `./gradlew build` falha abaixo disso.

## Regras

- Nenhuma API do Android aqui dentro.
- Toda a lógica de score fica neste módulo.
- Não pode depender do `app` (vai fazer parte do SDK para bancos).

## Comandos

```bash
./gradlew :core-domain:build
./gradlew :core-domain:test
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```
