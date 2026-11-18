# Project Conventions

> Fonte: `docs/especificacao.md` (seções 6, 7, 8, 11 e 12). Sem código ainda: padrões que só o código mostra estão como `TODO: no code yet`. Para padrões de código não fixados aqui, vale o skill base [skills/android/SKILL.md](skills/android/SKILL.md).

## Naming
- **Composables:** nome em PascalCase (`HomeScreen`, `RiskLevelIndicator`). O `.editorconfig` da raiz (ktlint) e o `app/detekt.yml` (detekt, `FunctionNaming`) ignoram a regra de nome de função só para funções `@Composable`; funções comuns continuam em camelCase.
- **Classes, interfaces e funções:** seguem a tabela "Naming Conventions" de skills/android/SKILL.md. Nomes já definidos na especificação: `AppInventory`, `SignalCollector`, `RiskScorer`, `ScanDevice`, `CardReader`, `InstalledApp`, `Signal`, `ScoringContext`, `RiskResult`, `ScanTrigger`, `ScanReport`, `NfcTagHandle`, `CardReadResult`.
- **Casos de uso:** `ScanDevice` usa `suspend operator fun invoke(...)` (único exemplo na especificação).
- **Sinais no código:** nomes descritivos no enum `Signal`, com o código S1–S9 no KDoc (de `UNTRUSTED_INSTALL_SOURCE` a `BANK_LOOKALIKE`; tabela em `glossary.md`).
- **Tabelas Room:** snake_case (`scan_run`, `app_snapshot`, `user_decision`, `alert_event`); colunas em camelCase (`startedAt`, `packageName`).
- **Valores de enum persistidos:** UPPER_SNAKE (`MANUAL`, `APP_OPEN`, `PERIODIC`, `TRUSTED`, `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED`).
- **Chaves de preferência:** camelCase (`onboardingDone`, `monitoringEnabled`, `scanIntervalHours`, `lastDefaultPaymentComponent`).
- **Módulos Gradle:** kebab-case com prefixo `core-` (`core-domain`, `core-detection`, `core-nfc`, `core-data`), mais `app` e `decoy-app`.
- **IDs de rastreio:** requisitos `RF-xxx` e `RNF-xx`; sinais `S1`–`S9`; telas `T1`–`T9`; specs `SPEC-0xx`.
- **Branches e PRs:** uma branch e um PR por spec, com o ID da spec no título. Formato do nome da branch: `spec-NNN-descricao-curta` (ex.: `spec-001-estrutura`).

## Folder / Package Structure
Pacotes por assunto dentro de cada módulo. `core-domain` (SPEC-010): `inventory`, `signal`, `risk`, `scan`, `card`, mais utilitários `internal` na raiz `core.domain`. `core-data` (SPEC-004): `database`, `database.entity`, `database.dao`, `settings`, `di`.
```
tap-guard/
├── core-domain/     # Kotlin puro: modelos, motor de score, interfaces (planned)
├── core-detection/  # coletores de sinais + caso de uso de varredura (planned)
├── core-nfc/        # leitura EMV + parser TLV (planned)
├── core-data/       # Room, DataStore, repositórios, histórico (planned)
├── app/             # telas, navegação, vigia, notificações, Hilt (planned)
├── decoy-app/       # app isca de teste (planned)
└── docs/
    └── especificacao.md
```

## UI
- Telas em Jetpack Compose + Material 3; navegação com Navigation Compose.
- Um ViewModel por tela; estado exposto como fluxo imutável.
- Toda tela com dados tem os estados: carregando, vazio, conteúdo e erro.
- Nível de risco nunca só por cor: sempre cor, ícone e texto. Compatível com TalkBack, contraste AA, fonte escalável.
- Textos em português do Brasil, frases curtas, sem termos técnicos (HCE, APDU) na interface; todos em arquivo de recursos.
- Textos fixos definidos na especificação (níveis, notificações, mensagem educativa do F2) devem ser usados como estão — ver `backlog_rules.md`.

## APIs
Não se aplica: sem backend e sem API HTTP. Os skills `api-design` e `error-handling` (HTTP / RFC 7807) não se aplicam.

## Database
- **Data access:** Room (histórico); DataStore (preferências).
- **Migrations:** schema do Room exportado em `core-data/schemas` e versionado. Cada mudança de versão traz uma `Migration` e um teste de migração. `fallbackToDestructiveMigration()` é proibido em release.
- **Regra de schema (ADR-014):**
  - **Até a primeira versão pública**, o schema da versão 1 pode mudar, com o `1.json` regerado e commitado junto, sem `Migration`: não há banco instalado em aparelho de usuário.
  - **A partir da primeira versão pública**, o schema de uma versão publicada fica congelado: mudança exige subir a versão, uma `Migration` e um teste de migração. O CI deve barrar alteração em `N.json` já existente na `main` (passo da SPEC-073).
  - **Desde já**, o passo "Schema do Room commitado" do CI falha se o `./gradlew build` regerar `core-data/schemas` com diferença do commitado (entidade mudou sem o schema junto).
- **Colunas:** nomes exatos da especificação (seção 9), em camelCase (`startedAt`), e não em `snake_case` como na skill de banco. Datas em milissegundos (`Long`); enums como texto.
- **Testes de banco:** DAOs em banco em memória (Robolectric); migração com o `MigrationTestHelper` no construtor com arquivo e driver (`AndroidSQLiteDriver`), lendo os schemas dos assets do teste local (`sourceSets.test.assets.directories`). O construtor clássico falha sob Robolectric.
- **Backup:** desligado (`allowBackup="false"`), com `dataExtractionRules` e `fullBackupContent` excluindo todos os domínios.
- **Soft delete:** não previsto; retenção por limpeza (10 varreduras, 90 dias de alertas).
- **Audit fields:** timestamps específicos por tabela (`startedAt`, `finishedAt`, `decidedAt`, `createdAt`, `readAt`); sem padrão genérico.
- **Sinais persistidos** como máscara de bits em `app_snapshot.signals`. S1 no bit 0 até S9 no bit 8. A ordem não muda depois de publicada. O bit é a propriedade `Signal.bit` (não o `ordinal`), e a conversão fica no `core-domain` (`toBitMask()` e `signalsFromBitMask()`, SPEC-010); o `core-data` só as chama.

## Testing
- **Framework:** JUnit 5 no `core-domain`; JUnit 4 nos módulos Android (Robolectric e instrumentados).
- **Mock library:** MockK. **Flow:** Turbine. **UI:** testes do Compose.
- **Integration tests:** Robolectric com `PackageManager` simulado e Room em memória.
- **Testes de Compose (sem aparelho, em `src/test`):** tela inteira pela Activity real com `createAndroidComposeRule<MainActivity>()`; composable isolado com `Robolectric.buildActivity(ComponentActivity::class.java)` + `createEmptyComposeRule()`, sem `ui-test-manifest` (para não pôr uma Activity de teste no APK de debug). Regras do pacote `androidx.compose.ui.test.junit4.v2`.
- **Manifesto mesclado:** `MergedManifestTest` (Robolectric) falha com permissão fora da lista explícita, sem `QUERY_ALL_PACKAGES` declarada, ou com componente exportado além da `MainActivity` e do `ProfileInstallReceiver`. `LibraryManifestTest` (`core-detection`) falha se o manifesto mesclado da biblioteca declarar permissão além de `REORDER_TASKS` (do `androidx.test:core`, só em teste).
- **Ramos por versão do Android:** testados com `@Config(sdk = [...])` no primeiro nível de cada ramo novo, mais o minSdk e o targetSdk (inventário: 26, 28, 30, 33 e 36).
- **Dispatcher injetado:** para provar que o código roda no `CoroutineDispatcher` que recebe, envolver o `StandardTestDispatcher(testScheduler)` do `runTest` num dispatcher de teste que conta despachos. Não criar um segundo `TestCoroutineScheduler`.
- **Ponta a ponta:** instalar o `decoy-app` (por `adb install`), rodar a varredura, conferir nível e notificação — emulador no CI e aparelho físico.
- **Manual:** leitura de cartão real em aparelho físico com NFC.
- **Coverage:** mínimo de 80% de cobertura de linhas no `core-domain`, medido pelo Kover 0.9.11 e verificado no `./gradlew build` (`:core-domain:koverVerify`). Os outros módulos não têm meta. Sem código de produção a regra passa; interfaces não contam, `data class` conta (ADR-012).
- **Fixtures do domínio:** em `core-domain/src/testFixtures` (`java-test-fixtures`), usadas pelos outros módulos com `testImplementation(testFixtures(project(":core-domain")))`. O detekt analisa o `testFixtures`; o Kover o exclui (SPEC-012).
- **Teste parametrizado de lista embutida:** parametrizar sobre uma lista escrita à mão no teste, nunca sobre a lista de produção, e conferir à parte que a lista de produção é igual à esperada. Assim, tirar ou acrescentar uma entrada derruba um teste (SPEC-012).
- **Coletor sem API do Android:** JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`.
- **Test naming:** frase entre crases nos testes de JVM e Robolectric; camelCase nos instrumentados. Ver skills/android/testing.md.
- **Dados de teste:** nunca malware real; nunca dados reais de cartão (vetores TLV sintéticos com número de cartão de teste).

## Domain Models (SPEC-010)
- **Imutáveis:** data classes só com `val`, verificado pelo detekt (`DataClassShouldBeImmutable` em `core-domain/detekt.yml`). Nenhum array nos modelos.
- **Coleções:** modelo com coleção tem construtor privado, `copy()` privado (`@ConsistentCopyVisibility`) e uma fábrica `companion operator fun invoke` com o mesmo nome e os mesmos parâmetros, que copia cada coleção por `unmodifiableCopy()` (`Collections.unmodifiableSet/List` sobre uma cópia). A coleção exposta recusa alteração, inclusive por cast para `MutableSet` ou `MutableList`. Não usar `Set.copyOf`/`List.copyOf` (API 30 no Android) nem `toSet()`/`toList()` como cópia defensiva.
- **`@Suppress`:** proibido, com duas exceções de alcance fechado:
  - `@Suppress("LongParameterList")` só numa fábrica que espelha exatamente o construtor de uma data class (o detekt isenta o construtor por `ignoreDataClasses`, mas não a fábrica). Hoje: `InstalledApp` (ADR-015). Fixtures de teste expõem só os parâmetros que os testes usam, em vez de espelhar o construtor.
  - `@Suppress("DEPRECATION")` só no ramo antigo de um teste de `SDK_INT` cujo outro ramo usa a API nova. No código de produção, só no `PackageManagerVersionCompat.kt` do `core-detection`; nos testes, só na fixture que monta o `PackageInfo` para a API 26 (ADR-016).
- **Supressões de lint:** só no próprio elemento, com comentário do motivo ao lado; sem `lint.xml`. Hoje: `tools:ignore="QueryAllPackagesPermission"` no `<uses-permission>` do `app` e `@SuppressLint("QueryPermissionsNeeded")` em `installedPackagesCompat` (ADR-016).
- **Ramos por versão do Android:** num arquivo só por módulo (`PackageManagerVersionCompat.kt` no `core-detection`), com cortes `Build.VERSION.SDK_INT >= Build.VERSION_CODES.*`.
- **`api` × `implementation` em módulo de biblioteca:** dependência cujo tipo aparece na API pública é `api`; o resto é `implementation`. No `core-detection`: `api` para o `core-domain` e para `kotlinx-coroutines-core`.
- **Dados de cartão:** só os quatro últimos dígitos, pelo tipo `LastFourDigits`; o `toString()` dos tipos com dado de cartão não mostra nenhum valor.
- **Datas:** `java.time.Instant` e `YearMonth` nos modelos; a conversão para milissegundos fica no `core-data`.

## Dependency Injection
- **Container:** Hilt, configurado no `app`.
- **Style:** injeção por construtor. Módulos Hilt só ligam interface a implementação e fornecem tipos de terceiros. Classes do `core-domain` não têm anotações do Hilt.

## Error Handling
- **Strategy:** estado de erro em toda tela com dados; no F2, mensagem própria para ausência de NFC, NFC desligado (com atalho para as configurações), cartão não suportado e leitura interrompida.
- Falha da API de pagamento padrão em versões novas do Android degrada sem erro.
- **Falhas esperadas como valores:** sealed no `core-domain`, como `CardReadResult` (`Success`, `UnsupportedCard`, `ReadInterrupted`). NFC ausente ou desligado não é resultado de `CardReader.read`.
- **Invariantes dos modelos:** `require` no `init`, que lança `IllegalArgumentException` (score de 0 a 100, máscara válida, quatro dígitos, fim da varredura não anterior ao início). Mensagem sobre dado do cartão nunca repete o valor recebido.
- Sem exceções próprias por enquanto.
- **Logging:** nunca logar dados de cartão nem enviá-los a análise de falhas. Biblioteca: TODO: no code yet.

## Quality Gates
- ktlint e detekt e a suíte de testes passam antes de uma spec ser dada como concluída.
- CI no GitHub Actions: jobs "Regras do projeto" e "Build e testes", obrigatórios na ruleset da `main`. Actions novas são referenciadas pelo SHA do commit, com a versão em comentário na mesma linha.
- Toda spec entrega código, testes e atualização da documentação do módulo.
