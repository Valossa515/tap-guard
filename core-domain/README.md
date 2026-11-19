# core-domain

Domínio do Tap Guard em Kotlin puro: modelos (app, sinal, score, nível), motor de score e interfaces dos coletores (`AppInventory`, `SignalCollector`, `CardReader`).

> **Estado (SPEC-010):** modelos e contratos do domínio, sem implementação. O motor de score vem na SPEC-017, os coletores e o inventário da SPEC-011 à SPEC-016, o caso de uso de varredura na SPEC-018 e o leitor do cartão na SPEC-041.

## Pacotes

Raiz: `io.github.valossa515.tapguard.core.domain`.

| Pacote | Tipos |
|--------|-------|
| `inventory` | `InstalledApp`, `AppInventory` |
| `signal` | `Signal`, `toBitMask()` e `signalsFromBitMask()` (`SignalBitMask.kt`), `SignalCollector` |
| `risk` | `RiskLevel`, `RiskResult`, `ScoringContext`, `RiskScorer` |
| `scan` | `ScanTrigger`, `ScannedApp`, `ScanReport`, `ScanDevice` |
| `card` | `NfcTagHandle`, `LastFourDigits`, `CardReadResult`, `CardReader` |
| raiz | `unmodifiableCopy()` (`internal`) |

As cinco interfaces seguem as assinaturas da seção 7 da especificação.

## Modelos

- **`InstalledApp`:** pacote, nome, `versionCode`, instalador (nulo se desconhecido), datas de instalação e de atualização (`Instant`), permissões pedidas e nomes de classe dos serviços declarados. Não tem campo de app do sistema: o `AppInventory` não os devolve (regra 1 da seção 5).
- **`Signal`:** S1 a S9 com nomes descritivos e o bit de cada um na máscara de `app_snapshot.signals` (S1 no bit 0 até S9 no bit 8). Sem peso: os pesos ficam na configuração do motor (SPEC-017).
- **`RiskLevel`:** `GREEN`, `YELLOW`, `RED`, em ordem de gravidade; os nomes são os valores gravados no banco.
- **`RiskResult`:** score de 0 a 100, nível e sinais. O nível não é conferido contra o score (regra 2: app confiável fica verde com qualquer score).
- **`ScoringContext`:** os pacotes marcados como confiáveis pelo usuário.
- **`ScanReport`:** gatilho (`ScanTrigger`), início, fim (não anterior ao início) e os apps avaliados (`ScannedApp`).
- **`CardReadResult`:** `Success` (nome da aplicação, `LastFourDigits` e validade), `UnsupportedCard` ou `ReadInterrupted`. Não há campo para o número completo do cartão. `LastFourDigits` só aceita quatro dígitos ASCII. O `toString()` dos dois não mostra nenhum dado do cartão.
- **`NfcTagHandle`:** marcador opaco do cartão aproximado. O `core-nfc` embrulha a `Tag` do Android; este módulo não a conhece.

## Regras

- Nenhuma API do Android aqui dentro.
- Toda a lógica de score fica neste módulo.
- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- **Modelos imutáveis:** data classes só com `val` (o detekt barra `var` pela `DataClassShouldBeImmutable`, em `detekt.yml`). Modelo com coleção tem construtor e `copy()` privados e uma fábrica com o mesmo nome que copia a coleção para uma coleção não modificável, que recusa alteração mesmo por cast.
- **`@Suppress`:** só `@Suppress("LongParameterList")` numa fábrica que espelha exatamente o construtor de uma data class (hoje, a do `InstalledApp`). Fora disso, nenhum (ADR-015).
- Invariantes por `require`, que lança `IllegalArgumentException`: score de 0 a 100, máscara válida, quatro dígitos, fim da varredura não anterior ao início.

## Dependências

- **Módulos:** nenhum.
- **Plugins:** só `kotlin("jvm")` (mais ktlint, detekt e Kover). Nenhum plugin ou dependência Android ou AndroidX.
- **Bibliotecas:** nenhuma além da stdlib do Kotlin e do JDK (`java.time`, `java.util.Collections`, disponíveis no Android desde a API 26).
- **Testes:** JUnit 5 (com `junit-platform-launcher`, exigido pelo Gradle 9), sem mocks.
- **Fixtures** (SPEC-012): `anInstalledApp(...)` e `aRiskResult(...)` em `src/testFixtures/.../DomainFixtures.kt`, pelo plugin `java-test-fixtures`. Os testes deste módulo as enxergam direto; os outros módulos usam `testImplementation(testFixtures(project(":core-domain")))` (hoje, o `core-detection`). Cada fixture expõe só os parâmetros que algum teste troca.
- **detekt e Kover no `testFixtures`:** a tarefa `detekt` não analisa `src/testFixtures` por padrão, então o `build.gradle.kts` o acrescenta (`source.from`). O Kover exclui o source set `testFixtures` (`excludedSourceSets`), para as fixtures não contarem na cobertura. Fora de `src/test`, a regra `MagicNumber` do detekt vale: número literal numa fixture vira constante nomeada.
- **Cobertura:** Kover, com no mínimo 80% de cobertura de linhas (RNF-08). O `./gradlew build` falha abaixo disso. Na SPEC-010: 98,8%; na SPEC-012, sem as fixtures: 98,8% (82 de 83 linhas).

## Comandos

```bash
./gradlew :core-domain:build
./gradlew :core-domain:test
./gradlew :core-domain:detekt
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```
