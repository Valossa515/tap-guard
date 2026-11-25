# core-domain

Domínio do Tap Guard em Kotlin puro: modelos (app, sinal, score, nível), motor de score e interfaces dos coletores (`AppInventory`, `SignalCollector`, `CardReader`).

> **Estado (SPEC-010; `InstalledApp.cardEmulationServices` na SPEC-013, `enabledAccessibilityServices` na SPEC-014 e `defaultPaymentServices` na SPEC-016; motor de score na SPEC-017):** modelos, contratos do domínio e o motor de score (`WeightedRiskScorer`). Os coletores e o inventário vêm da SPEC-011 à SPEC-016 (no `core-detection`), o caso de uso de varredura na SPEC-018 e o leitor do cartão na SPEC-041.

## Pacotes

Raiz: `io.github.valossa515.tapguard.core.domain`.

| Pacote | Tipos |
|--------|-------|
| `inventory` | `InstalledApp`, `AppInventory` |
| `signal` | `Signal`, `toBitMask()` e `signalsFromBitMask()` (`SignalBitMask.kt`), `SignalCollector` |
| `risk` | `RiskLevel`, `RiskResult`, `ScoringContext`, `RiskScorer`, `WeightedRiskScorer` (SPEC-017), `RiskScoreConfig` (`internal`, SPEC-017) |
| `scan` | `ScanTrigger`, `ScannedApp`, `ScanReport`, `ScanDevice` |
| `card` | `NfcTagHandle`, `LastFourDigits`, `CardReadResult`, `CardReader` |
| raiz | `unmodifiableCopy()` (`internal`) |

As cinco interfaces seguem as assinaturas da seção 7 da especificação.

## Modelos

- **`InstalledApp`:** pacote, nome, `versionCode`, instalador (nulo se desconhecido), datas de instalação e de atualização (`Instant`), permissões pedidas, nomes de classe dos serviços declarados e, em `cardEmulationServices`, nomes de classe dos serviços de HCE (os que respondem a `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e exigem `android.permission.BIND_NFC_SERVICE`, inclusive desligados; off-host e NFC-F fora, ADR-018) e, em `enabledAccessibilityServices`, nomes de classe completos dos serviços de acessibilidade que o usuário ligou (`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, inclusive um que caiu; ADR-019) e, em `defaultPaymentServices`, nomes de classe completos dos serviços de HCE do app que o sistema diz serem o padrão de pagamento (`CardEmulation.isDefaultServiceForCategory` com `CATEGORY_PAYMENT`, só para serviços ligados e exportados; parte de `cardEmulationServices`; vazio sem NFC ou sem HCE; ADR-021). Os conjuntos de serviços vêm de leituras diferentes do sistema, então `cardEmulationServices` e `enabledAccessibilityServices` não são validados como parte de `services`. Não tem campo de app do sistema: o `AppInventory` não os devolve (regra 1 da seção 5).
- **`Signal`:** S1 a S9 com nomes descritivos e o bit de cada um na máscara de `app_snapshot.signals` (S1 no bit 0 até S9 no bit 8). Sem peso: os pesos ficam na configuração do motor (SPEC-017).
- **`RiskLevel`:** `GREEN`, `YELLOW`, `RED`, em ordem de gravidade; os nomes são os valores gravados no banco.
- **`RiskResult`:** score de 0 a 100, nível e sinais. O nível não é conferido contra o score (regra 2: app confiável fica verde com qualquer score).
- **`ScoringContext`:** os pacotes marcados como confiáveis pelo usuário.
- **`ScanReport`:** gatilho (`ScanTrigger`), início, fim (não anterior ao início) e os apps avaliados (`ScannedApp`).
- **`CardReadResult`:** `Success` (nome da aplicação, `LastFourDigits` e validade), `UnsupportedCard` ou `ReadInterrupted`. Não há campo para o número completo do cartão. `LastFourDigits` só aceita quatro dígitos ASCII. O `toString()` dos dois não mostra nenhum dado do cartão.
- **`NfcTagHandle`:** marcador opaco do cartão aproximado. O `core-nfc` embrulha a `Tag` do Android; este módulo não a conhece.

## Motor de score (SPEC-017, ADR-022)

`WeightedRiskScorer()` implementa o `RiskScorer` da seção 5. Público, sem parâmetros e sem estado; do `InstalledApp`, só lê o pacote.

1. Soma os pesos dos sinais recebidos e limita a soma a 100.
2. **Regra 1** (apps do sistema): não é do motor. O `AppInventory` não os devolve.
3. **Regra 3:** sem S1 (loja confiável), sem S7 e sem S9, limita o score a 29.
4. **Regra 4:** sem S1 junto com S2, S3 ou S7, limita o score a 59.
5. **Nível:** verde de 0 a 29, amarelo de 30 a 59, vermelho de 60 em diante. **Regra 2:** pacote em `ScoringContext.trustedPackages` (comparação exata) fica verde com qualquer score. A regra 2 só decide o nível: o score continua passando pelas regras 3 e 4.

As regras 3 e 4 são tetos e comutam (quando as duas valem, vence 29). Sem S1 a regra 4 sempre vale: um app sem S1 nunca fica vermelho. `RiskResult.signals` são os sinais recebidos.

**Configuração:** `RiskScoreConfig` (`internal object`) é o único arquivo de configuração: os nove pesos, os tetos (100, 29, 59) e o início das faixas (30, 60), como `const val`. `weightOf` usa `when` exaustivo: um sinal novo sem peso não compila. Mudar um peso exige atualizar, na mesma spec, os cinco exemplos da seção 5 e a tabela de testes.

**Testes:** `WeightedRiskScorerExamplesTest` (os cinco exemplos da seção 5), `WeightedRiskScorerTableTest` (37 combinações das regras 2 a 4 e dos limites 29/30 e 59/60, com `@ParameterizedTest` e `@MethodSource`; os sinais são escritos como na seção 5 e traduzidos por um mapa escrito à mão, sem ler o `RiskScoreConfig`) e `RiskScoreConfigTest` (faixas).

A regra dos apps de clonagem não fica aqui: fica no coletor de S1 (SPEC-019).

## Regras

- Nenhuma API do Android aqui dentro.
- Toda a lógica de score fica neste módulo.
- Não pode depender do `app` (vai fazer parte do SDK para bancos).
- **Modelos imutáveis:** data classes só com `val` (o detekt barra `var` pela `DataClassShouldBeImmutable`, em `detekt.yml`). Modelo com coleção tem construtor e `copy()` privados e uma fábrica com o mesmo nome que copia a coleção para uma coleção não modificável, que recusa alteração mesmo por cast.
- **`@Suppress`:** só `@Suppress("LongParameterList")` numa fábrica que espelha exatamente o construtor de uma data class (hoje, a do `InstalledApp`, ADR-015) e numa fixture do `testFixtures` que repassa à fábrica de uma data class, com valores padrão (hoje, a `anInstalledApp`, ADR-019). Fora disso, nenhum, nem em outras funções de teste.
- Invariantes por `require`, que lança `IllegalArgumentException`: score de 0 a 100, máscara válida, quatro dígitos, fim da varredura não anterior ao início.

## Dependências

- **Módulos:** nenhum.
- **Plugins:** só `kotlin("jvm")` (mais ktlint, detekt e Kover). Nenhum plugin ou dependência Android ou AndroidX.
- **Bibliotecas:** nenhuma além da stdlib do Kotlin e do JDK (`java.time`, `java.util.Collections`, disponíveis no Android desde a API 26).
- **Testes:** JUnit 5 (com `junit-platform-launcher`, exigido pelo Gradle 9), sem mocks. Testes parametrizados com `@ParameterizedTest` e `@MethodSource` num `companion object` com `@JvmStatic` (o artefato `junit-jupiter` já traz o `junit-jupiter-params`; SPEC-017).
- **Fixtures** (SPEC-012): `anInstalledApp(...)` (com `cardEmulationServices` desde a SPEC-013 e, desde a SPEC-014, `firstInstallTime` e `lastUpdateTime`, com os valores de antes como padrão, e `enabledAccessibilityServices`, vazio por padrão; desde a SPEC-015, `label`, com `"Example"` como padrão; desde a SPEC-016, `defaultPaymentServices`, vazio por padrão) e `aRiskResult(...)` em `src/testFixtures/.../DomainFixtures.kt`, pelo plugin `java-test-fixtures`. Os testes deste módulo as enxergam direto; os outros módulos usam `testImplementation(testFixtures(project(":core-domain")))` (hoje, o `core-detection`). Cada fixture expõe só os parâmetros que algum teste troca.
- **`@Suppress("LongParameterList")` na `anInstalledApp`** (SPEC-014): com 10 parâmetros desde a SPEC-016, ela passa do limite do detekt (6). Repassa à fábrica do `InstalledApp`, uma data class, com valores padrão, e só existe no `testFixtures`. É a exceção do ADR-015, estendida pelo ADR-019 a fixtures que repassam à fábrica de uma data class, com valores padrão; não vale para outras funções de teste.
- **detekt e Kover no `testFixtures`:** a tarefa `detekt` não analisa `src/testFixtures` por padrão, então o `build.gradle.kts` o acrescenta (`source.from`). O Kover exclui o source set `testFixtures` (`excludedSourceSets`), para as fixtures não contarem na cobertura. Fora de `src/test`, a regra `MagicNumber` do detekt vale: número literal numa fixture vira constante nomeada.
- **Cobertura:** Kover, com no mínimo 80% de cobertura de linhas (RNF-08). O `./gradlew build` falha abaixo disso. Na SPEC-010: 98,8%; na SPEC-012, sem as fixtures: 98,8% (82 de 83 linhas); na SPEC-016: 98,9%; na SPEC-017: 97,5% (117 de 120; as duas linhas novas sem cobertura são as declarações `val` do `companion object` privado do `WeightedRiskScorer`, cujo getter o código não chama).

## Comandos

```bash
./gradlew :core-domain:build
./gradlew :core-domain:test
./gradlew :core-domain:detekt
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```
