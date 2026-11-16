# Project Reference

> **Gerado pelo `@bootstrap` em 2026-11-16.** Fonte única: `docs/especificacao.md` (versão de 13/11/2026). Ainda não há código no repositório: tudo o que a especificação descreve está marcado como **(planned)**. Seções que só o código pode responder estão marcadas `TODO: no code yet`.

---

## 1. Project Overview

| Field | Value |
|-------|-------|
| **Name** | `tap-guard` (repositório). Nome de exibição em português: em aberto (especificação, seção 13) |
| **Description** | App Android que avisa o usuário comum quando há no aparelho um app com o perfil dos usados em golpes de retransmissão de cartão por NFC, e ensina por que nenhum banco pede para encostar o cartão no celular |
| **Type** | App Android (sem backend) **(planned)** |
| **Stack** | Kotlin, Jetpack Compose, Material 3, Navigation Compose, corrotinas e Flow, Hilt, Room, DataStore, WorkManager **(planned)** |
| **Build tool** | Gradle com Kotlin DSL e catálogo de versões, monorepo de seis módulos **(planned)** |
| **Runtime version** | `minSdk` 26 (Android 8.0). `targetSdk`: o mais recente exigido pela Play Store na data da entrega **(planned)**. Versão do Kotlin/JDK/AGP: TODO: no code yet |
| **Source root** | TODO: no code yet (módulos planejados: `core-domain`, `core-detection`, `core-nfc`, `core-data`, `app`, `decoy-app`) |
| **Test root** | TODO: no code yet |

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
- **Base package / application id:** em aberto (especificação, seção 13).

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

### Demais classes (ViewModels, repositórios, DAOs, workers, receptores)

TODO: no code yet.

---

## 4. Available Endpoints

Não se aplica: o app não tem backend nem API HTTP (especificação, seções 1 e 3).

### Telas (planned, especificação seção 10)

| ID | Tela | Sai para |
|----|------|----------|
| T1 | Onboarding | T2 |
| T2 | Início | T3, T5, T7, T8 |
| T3 | Resultado | T4 |
| T4 | Detalhe do app | Telas do sistema |
| T5 | Teste do cartão: explicação | T6 |
| T6 | Teste do cartão: leitura e resultado | T2 |
| T7 | Emergência | Telas do sistema |
| T8 | Configurações | T9 |
| T9 | Histórico de alertas | T4 |

---

## 5. Database Schema (planned, especificação seção 9)

### Tables (Room)

| Table | Key Columns | Notes |
|-------|-------------|-------|
| `scan_run` | `id`, `startedAt`, `finishedAt`, `trigger`, `totalApps`, `redCount`, `yellowCount` | Uma linha por varredura. Retenção: 10 varreduras mais recentes |
| `app_snapshot` | chave `scanId` + `packageName`; `label`, `versionCode`, `installer`, `firstInstallTime`, `lastUpdateTime`, `signals` (máscara de bits), `score`, `level` | Estado de cada app em cada varredura |
| `user_decision` | `packageName`, `decision`, `decidedAt` | Apps marcados como confiáveis |
| `alert_event` | `id`, `type`, `packageName`, `versionCode`, `level`, `createdAt`, `readAt` | Histórico de alertas e controle de duplicidade. Retenção: 90 dias |

Nenhum dado de cartão entra no banco nem nas preferências.

### Preferências (DataStore)

| Chave | Tipo | Padrão |
|-------|------|--------|
| `onboardingDone` | booleano | TODO: não definido na especificação |
| `monitoringEnabled` | booleano | verdadeiro |
| `scanIntervalHours` | inteiro (1, 6, 12, 24) | 6 |
| `lastDefaultPaymentComponent` | texto | — (usado para detectar troca) |

### Enums / Status Values

| Enum | Values | Used In |
|------|--------|---------|
| Gatilho da varredura | `MANUAL`, `APP_OPEN`, `PERIODIC` | `scan_run.trigger` |
| Decisão do usuário | `TRUSTED` | `user_decision.decision` |
| Tipo de alerta | `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED` | `alert_event.type` |
| Nível de risco | verde, amarelo, vermelho (identificadores no código: TODO: no code yet) | `app_snapshot.level`, `alert_event.level` |
| Sinais | S1–S9 | `app_snapshot.signals` (máscara de bits; S1 no bit 0 até S9 no bit 8. A ordem não muda depois de publicada.) |

Versão do schema: TODO: no code yet.
Migrações: schema do Room exportado em `core-data/schemas` e versionado. Cada mudança de versão traz uma `Migration` e um teste de migração. `fallbackToDestructiveMigration()` é proibido em release.

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

Versões: TODO: no code yet (catálogo de versões ainda não existe).

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

---

## 9. How to Run

```bash
# Build
TODO: no code yet

# Run
TODO: no code yet

# Test
TODO: no code yet

# Lint (ktlint + detekt)
TODO: no code yet
```

O app isca é instalado com `adb install` para simular origem fora da loja (especificação, seção 11).

---

*Last updated: 2026-11-16*
