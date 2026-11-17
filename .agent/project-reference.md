# Project Reference

> **Gerado pelo `@bootstrap` em 2026-11-16.** Fonte única: `docs/especificacao.md` (versão de 13/11/2026). Ainda não há código no repositório: tudo o que a especificação descreve está marcado como **(planned)**. Seções que só o código pode responder estão marcadas `TODO: no code yet`.

---

## 1. Project Overview

| Field | Value |
|-------|-------|
| **Name** | `tap-guard` (repositório). Nome de exibição em português: em aberto (especificação, seção 13) |
| **Application id** | `app`: `io.github.valossa515.tapguard`; `decoy-app`: `io.github.valossa515.tapguard.decoy` (ADR-011) |
| **Description** | App Android que avisa o usuário comum quando há no aparelho um app com o perfil dos usados em golpes de retransmissão de cartão por NFC, e ensina por que nenhum banco pede para encostar o cartão no celular |
| **Type** | App Android (sem backend). Projeto Gradle desde a SPEC-001; desde a SPEC-003 o app abre, com tema, componente de nível e nove telas vazias; desde a SPEC-004 tem Hilt, banco Room e DataStore (infraestrutura, sem regra) |
| **Stack** | Kotlin, Jetpack Compose, Material 3, Navigation Compose, corrotinas e Flow, Hilt, Room, DataStore, WorkManager **(planned)** |
| **Build tool** | Gradle 9.7.0 (wrapper) com Kotlin DSL e catálogo de versões (`gradle/libs.versions.toml`), monorepo de seis módulos. Sem `build-logic` nem plugins de convenção: cada módulo declara a própria configuração |
| **Runtime version** | `minSdk` 26 (Android 8.0), `targetSdk` 36, `compileSdk` 37 (ADR-013). Kotlin 2.4.21, AGP 9.3.3 (Kotlin embutido nos módulos Android), JDK 21 (toolchain de todos os módulos, daemon do Gradle e CI) |
| **Source root** | `<módulo>/src/main/kotlin`. Com código: `app` (pacote `io.github.valossa515.tapguard`) e `core-data` (pacote `io.github.valossa515.tapguard.core.data`). Ainda vazios: `core-domain`, `core-detection`, `core-nfc`, `decoy-app` |
| **Test root** | `<módulo>/src/test/kotlin`: teste de fumaça por módulo; no `app`, testes Robolectric de navegação, níveis, tema, cores e manifesto; no `core-data`, DAOs, migração, preferências e Hilt |

### Problem & Purpose
- Golpistas convencem a vítima a instalar um APK fora da loja, encostar o cartão no celular e digitar a senha. O app retransmite o cartão para um comparsa, que paga ou saca.
- Público: pessoas sem conhecimento técnico, foco no Brasil, português simples, sem jargão.
- Princípio de confiança: tudo roda no aparelho; sem backend, sem cadastro, sem envio de dados.

### Key Flows (planned)
1. **F1 Varredura de apps:** lista os apps instalados que não são do sistema, coleta os sinais S1–S9, calcula score e nível (verde/amarelo/vermelho).
2. **F2 Teste do cartão:** o usuário encosta o próprio cartão EMV e vê que dados saem por aproximação (só bandeira/nome da aplicação, 4 últimos dígitos e validade).
3. **F3 Vigia em segundo plano:** varredura periódica (padrão 6 h) via WorkManager; notifica app de risco novo/atualizado e troca do app de pagamento padrão.
4. **F4 Emergência:** passo a passo marcável para quem suspeita que caiu no golpe, funciona sem internet.
5. Onboarding de três telas e configurações.

---

## 2. Architecture (planned)

- **Style:** monorepo Gradle multimódulo; lógica de detecção isolada em módulos sem interface gráfica (para virar SDK depois sem reescrita). Dentro do `app`: camadas Interface → Domínio (casos de uso dos `core-*`) → Dados (repositórios do `core-data`).
- **Packaging:** por módulo de capacidade (`core-domain`, `core-detection`, `core-nfc`, `core-data`). Organização de pacotes dentro de cada módulo: TODO: no code yet.
- **Base package / application id:** `io.github.valossa515.tapguard` (decidido na SPEC-001, ADR-011). Namespaces: `…tapguard.core.domain`, `.core.detection`, `.core.nfc`, `.core.data`; `app` = `io.github.valossa515.tapguard`; `decoy-app` = `io.github.valossa515.tapguard.decoy`.

### Layer Map

| Layer | Módulo | Responsibility |
|-------|--------|----------------|
| Interface | `app` | Telas em Compose, um ViewModel por tela, estado exposto como fluxo imutável, navegação, vigia agendada, notificações, injeção de dependências |
| Domínio | `core-domain` (Kotlin puro) | Modelos (app, sinal, score, nível), motor de score, interfaces dos coletores |
| Adaptadores de plataforma | `core-detection`, `core-nfc` | Coletores de sinais sobre as APIs do Android e caso de uso de varredura; leitura EMV e parser TLV |
| Dados | `core-data` | Banco local (Room), preferências (DataStore), repositórios, histórico |
| Alvo de teste | `decoy-app` | App isca inofensivo com o perfil de um app de golpe |

### External Integrations

Nenhuma integração de rede: o app **não declara `INTERNET`** (RNF-01). Integrações apenas com APIs do sistema Android:

| System | Protocol | Purpose |
|--------|----------|---------|
| `PackageManager` | API Android | Inventário de apps, permissões, serviços, origem da instalação, datas |
| `AccessibilityManager` | API Android | S4 — serviços de acessibilidade ativos |
| `CardEmulation` | API Android | S7 — app de pagamento padrão (Android 15+: papel de carteira, a confirmar na SPEC-016) |
| `NfcAdapter` / `IsoDep` | NFC modo leitor | F2 — leitura EMV (PPSE, SELECT, GPO, READ RECORD) |
| WorkManager | API Android | F3 — varredura periódica |
| Notificações | API Android | F3 — alertas |
| Telas do sistema | Intents | Desinstalação, informações do app, configurações de NFC, apps instalados |

---

## 3. Module & Class Map

> TODO: no code yet. Abaixo, apenas os nomes que a especificação define **(planned)**.

### Contratos do domínio (planned, especificação seção 7)

| Interface | Módulo | Responsibility |
|-----------|--------|----------------|
| `AppInventory` | `core-domain` (interface) / `core-detection` (impl.) | `suspend fun installedApps(): List<InstalledApp>` — inventário de apps |
| `SignalCollector` | `core-domain` (interface) / `core-detection` (impl.) | `suspend fun collect(app: InstalledApp): Set<Signal>` — sinais de um app |
| `RiskScorer` | `core-domain` | `fun score(app: InstalledApp, signals: Set<Signal>, ctx: ScoringContext): RiskResult` — score e nível, domínio puro |
| `ScanDevice` | caso de uso (`core-detection`) | `suspend operator fun invoke(trigger: ScanTrigger): ScanReport` — orquestra a varredura |
| `CardReader` | `core-domain` (interface) / `core-nfc` (impl.) | `suspend fun read(tag: NfcTagHandle): CardReadResult` — leitura do cartão |

### Tipos citados (planned)

`InstalledApp`, `Signal`, `ScoringContext`, `RiskResult`, `ScanTrigger`, `ScanReport`, `NfcTagHandle`, `CardReadResult`. Campos e forma: TODO: no code yet.

### App (SPEC-003)

| Classe / função | Pacote | Responsibility |
|-----------------|--------|----------------|
| `MainActivity` | raiz | Única Activity exportada; `setContent { TapGuardTheme { TapGuardNavHost() } }` |
| `OnboardingRoute` … `AlertHistoryRoute`, `AppDetailRoute(packageName)` | `navigation` | Rotas `@Serializable`, uma por tela |
| `TapGuardNavHost` | `navigation` | Grafo provisório da seção 10; único lugar que conhece o `NavController` |
| `TapGuardTheme` | `ui/theme` | Material 3 com paleta fixa clara/escura, sem cores dinâmicas |
| `RiskLevelUi` | `ui/components` | `GREEN`, `YELLOW`, `RED`; tipo da interface até o `RiskLevel` do domínio (SPEC-010) |
| `riskLevelColors` | `ui/components` | Função pura: fundo e conteúdo de cada nível, em claro e escuro |
| `RiskLevelIndicator` | `ui/components` | Nível com cor, ícone e frase exata (RNF-06) |
| `{Feature}Screen` (9) | `ui/<tela>` | Telas vazias: título e botões de saída |

### Persistência e injeção (SPEC-004)

| Classe | Pacote | Responsibility |
|--------|--------|----------------|
| `TapGuardDatabase` | `core.data.database` | Room versão 1, arquivo `tapguard.db`, quatro tabelas |
| `ScanRunEntity`, `AppSnapshotEntity`, `UserDecisionEntity`, `AlertEventEntity` | `core.data.database.entity` | Formas de persistência da seção 9 (sem modelo de domínio ainda) |
| `ScanRunDao`, `AppSnapshotDao`, `UserDecisionDao`, `AlertEventDao` | `core.data.database.dao` | Só inserir e ler |
| `SettingsDataSource` | `core.data.settings` | As quatro preferências com padrões, sobre o DataStore `settings` |
| `DataModule` | `core.data.di` | Hilt: banco e DataStore únicos, e cada DAO |
| `TapGuardApplication` | `app` (raiz) | `@HiltAndroidApp` |

### Demais classes (ViewModels, repositórios, DAOs, workers, receptores)

TODO: no code yet.

---

## 4. Available Endpoints

Não se aplica: o app não tem backend nem API HTTP (especificação, seções 1 e 3).

### Telas (especificação seção 10; vazias e com navegação provisória desde a SPEC-003)

| ID | Tela | Rota | Sai para |
|----|------|------|----------|
| T1 | Onboarding (título "Boas-vindas") | `OnboardingRoute` | T2 (sai da pilha) |
| T2 | Início | `HomeRoute` | T3, T5, T7, T8 |
| T3 | Resultado | `ScanResultRoute` | T4 |
| T4 | Detalhe do app | `AppDetailRoute(packageName)` | Telas do sistema (planned) |
| T5 | Teste do cartão: explicação | `CardTestIntroRoute` | T6 |
| T6 | Teste do cartão: leitura e resultado | `CardTestReadingRoute` | T2 existente |
| T7 | Emergência | `EmergencyRoute` | Telas do sistema (planned) |
| T8 | Configurações | `SettingsRoute` | T9 |
| T9 | Histórico de alertas | `AlertHistoryRoute` | T4 |

Começa sempre na T1, sem persistência (provisório). A T3 e a T9 abrem a T4 com o pacote provisório `io.github.valossa515.tapguard.decoy`.

---

## 5. Database Schema (especificação seção 9; versão 1 desde a SPEC-004)

### Tables (Room)

Colunas com os nomes da especificação (camelCase); datas em milissegundos (`Long`); enums como texto. `?` = opcional.

| Table | Key Columns | Notes |
|-------|-------------|-------|
| `scan_run` | `id` (`Long`, autoincremento); `startedAt`, `finishedAt?`, `trigger`, `totalApps`, `redCount`, `yellowCount` | Uma linha por varredura. Retenção: 10 varreduras mais recentes (SPEC-018) |
| `app_snapshot` | chave `scanId` + `packageName`; FK `scanId` → `scan_run.id` ON DELETE CASCADE; `label`, `versionCode` (`Long`), `installer?`, `firstInstallTime`, `lastUpdateTime`, `signals` (`Int`, máscara de bits), `score` (`Int`), `level` | Estado de cada app em cada varredura |
| `user_decision` | chave `packageName`; `decision`, `decidedAt` | Apps marcados como confiáveis |
| `alert_event` | `id` (`Long`, autoincremento); `type`, `packageName?`, `versionCode?`, `level?`, `createdAt`, `readAt?` | Histórico de alertas e controle de duplicidade. Retenção: 90 dias (SPEC-018) |

Nenhum dado de cartão entra no banco nem nas preferências.

### Preferências (DataStore)

| Chave | Tipo | Padrão |
|-------|------|--------|
| `onboardingDone` | booleano | falso (decidido na SPEC-004) |
| `monitoringEnabled` | booleano | verdadeiro |
| `scanIntervalHours` | inteiro (1, 6, 12, 24) | 6 |
| `lastDefaultPaymentComponent` | texto | nulo quando nunca registrado (usado para detectar troca) |

### Enums / Status Values

| Enum | Values | Used In |
|------|--------|---------|
| Gatilho da varredura | `MANUAL`, `APP_OPEN`, `PERIODIC` | `scan_run.trigger` |
| Decisão do usuário | `TRUSTED` | `user_decision.decision` |
| Tipo de alerta | `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED` | `alert_event.type` |
| Nível de risco | `GREEN`, `YELLOW`, `RED` | `app_snapshot.level`, `alert_event.level` |
| Sinais | S1–S9 | `app_snapshot.signals` (máscara de bits; S1 no bit 0 até S9 no bit 8. A ordem não muda depois de publicada.) |

Versão do schema: 1 (`core-data/schemas/io.github.valossa515.tapguard.core.data.database.TapGuardDatabase/1.json`, commitado). Arquivos: banco `tapguard.db`, preferências `settings`.
Migrações: até a primeira versão pública, a versão 1 pode mudar com o schema regerado e commitado junto; depois, cada mudança sobe a versão e traz uma `Migration` e um teste de migração (ADR-014). O CI falha se o build regerar o schema com diferença do commitado. `fallbackToDestructiveMigration()` é proibido em release.

---

## 6. Conventions Summary

Ver `conventions.md`. Resumo do que a especificação fixa:

| Concern | Choice |
|---------|--------|
| Linguagem / build | Kotlin, Gradle Kotlin DSL, catálogo de versões |
| UI | Compose + Material 3, um ViewModel por tela, estado como fluxo imutável |
| Persistência | Room (histórico), DataStore (preferências) |
| DI | Hilt |
| Segundo plano | WorkManager, sem serviço permanente em primeiro plano |
| Erros | Estados carregando / vazio / conteúdo / erro em toda tela com dados; mensagens próprias para cada falha de NFC |
| Logging | Nunca logar dados de cartão (RNF-02). Biblioteca de log: TODO: no code yet |
| Testes | JUnit 5 no `core-domain`; JUnit 4 nos módulos Android (Robolectric, instrumentados); MockK, Turbine, testes de interface do Compose |
| Qualidade | ktlint, detekt, GitHub Actions; cobertura mínima de 80% no domínio |
| Textos | Português do Brasil, em arquivo de recursos |

Estrutura real de pastas: TODO: no code yet. Padrões de nomes: ver conventions.md.

---

## 7. Domain Glossary

Ver `glossary.md`.

---

## 8. Dependencies (planned)

Todas as versões ficam em `gradle/libs.versions.toml`. Já no catálogo (SPEC-001 a SPEC-004):

| Item | Versão | Uso |
|------|--------|-----|
| Gradle (wrapper) | 9.7.0 | Build |
| Android Gradle Plugin | 9.3.3 | Módulos Android (Kotlin embutido; sem `org.jetbrains.kotlin.android`) |
| Kotlin (`org.jetbrains.kotlin.jvm`) | 2.4.21 | `core-domain` |
| JDK | 21 | Toolchain, daemon do Gradle (`gradle/gradle-daemon-jvm.properties`) e CI |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | Módulos Android (compileSdk separado do targetSdk: ADR-013) |
| Plugins `org.jetbrains.kotlin.plugin.compose` e `.serialization` | 2.4.21 | `app` |
| Compose BOM (Material 3, `ui-test-junit4`) | 2026.09.00 | `app` |
| `androidx.activity:activity-compose` | 1.13.0 | `app` |
| `androidx.navigation:navigation-compose` | 2.10.2 | `app` (rotas tipadas) |
| Robolectric | 4.18 | Testes do `app` |
| `androidx.test:core` | 1.7.0 | Testes do `app` e do `core-data` |
| KSP (`com.google.devtools.ksp`) | 2.3.14 | Room e Hilt (sem kapt) |
| Hilt (`com.google.dagger.hilt.android`, `hilt-android`, `hilt-compiler`) | 2.60.1 | `app` e `core-data` |
| Room (`androidx.room`, `room-runtime`, `room-compiler`) | 2.8.5 | `core-data` |
| DataStore Preferences | 1.3.0 | `core-data` |
| `hilt-android-testing`, `room-testing` | 2.60.1 / 2.8.5 | Testes do `core-data` |
| `kotlinx-coroutines-test` | 1.11.0 | Testes do `core-data` |
| ktlint-gradle (`org.jlleitschuh.gradle.ktlint`) | 14.2.0, motor ktlint 1.8.0 | Estilo, todos os módulos |
| detekt (`io.gitlab.arturbosch.detekt`) | 1.23.8 | Análise estática, todos os módulos |
| Kover (`org.jetbrains.kotlinx.kover`) | 0.9.11 | Cobertura, só no `core-domain` (mínimo de 80% de linhas) — SPEC-002 |
| JUnit 5 (BOM) + `junit-platform-launcher` | 5.14.4 / 1.14.4 | Testes do `core-domain` |
| JUnit 4 | 4.13.2 | Testes dos módulos Android |

Ainda planejadas (versões definidas na spec que as introduzir):

| Dependency | Purpose |
|------------|---------|
| Jetpack Compose + Material 3 | Interface |
| Navigation Compose | Navegação entre T1–T9 |
| Kotlin Coroutines + Flow | Concorrência e estado |
| Hilt | Injeção de dependências |
| Room | Histórico de varreduras, decisões e alertas |
| DataStore | Preferências |
| WorkManager | Vigia periódica |
| JUnit 5 | Testes do `core-domain` |
| JUnit 4 + Robolectric | Testes dos módulos Android |
| MockK | Mocks |
| Turbine | Testes de Flow |
| Compose UI Test | Testes de interface |
| ktlint, detekt | Qualidade |

Actions do CI (fora do catálogo, em `.github/workflows/ci.yml`): `actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/wrapper-validation` v6.4.1 (`8da46b5…`) e `actions/upload-artifact` v7.0.2 (`cf430e0…`); as duas últimas por SHA.

---

## 9. How to Run

```bash
# Pré-requisitos: JDK 21 instalado, Android SDK com a plataforma 37, local.properties com sdk.dir

# Build (compila, lint do Android, ktlint, detekt, testes e cobertura do core-domain — o mesmo que o CI)
./gradlew build

# Run (APK de debug; instala e abre no aparelho ou emulador conectado)
./gradlew :app:assembleDebug
./gradlew :app:installDebug

# Test
./gradlew test
./gradlew :core-domain:test
./gradlew :app:testDebugUnitTest   # Robolectric: navegação, níveis, tema, cores e manifesto

# Lint (ktlint + detekt)
./gradlew ktlintCheck detekt
./gradlew ktlintFormat

# Cobertura do core-domain (Kover, mínimo de 80% de linhas)
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```

O CI publica o APK de debug do `app` como artefato `app-debug` em cada run do job "Build e testes".

O app isca é instalado com `adb install` para simular origem fora da loja (especificação, seção 11).

---

*Last updated: 2026-11-16*
