# Architecture

> Fonte: `docs/especificacao.md`, seções 7, 8 e 14. Nada implementado ainda: tudo **(planned)**.

## Overview
- **Style:** monorepo Gradle multimódulo com núcleo de domínio puro e adaptadores de plataforma atrás de interfaces (portas). Regra central: a lógica de detecção fica em módulos sem interface gráfica, para virar SDK depois sem reescrita.
- **Packaging strategy:** por módulo de capacidade. Pacotes internos de cada módulo: TODO: no code yet.
- **Base package:** em aberto (identificador do pacote é decisão pendente, seção 13).

## Modules (planned)

| Módulo | Tipo | Responsabilidade | Depende de |
| --- | --- | --- | --- |
| `core-domain` | Kotlin puro | Modelos (app, sinal, score, nível), motor de score, interfaces dos coletores | nada |
| `core-detection` | Biblioteca Android | Coletores de sinais sobre as APIs do Android, caso de uso de varredura | `core-domain` |
| `core-nfc` | Biblioteca Android | Leitura EMV por aproximação e parser TLV | `core-domain` |
| `core-data` | Biblioteca Android | Banco local, preferências, repositórios, histórico | `core-domain` |
| `app` | Aplicativo | Telas, navegação, vigia agendada, notificações, injeção de dependências | todos os `core-*` |
| `decoy-app` | Aplicativo de teste | App isca inofensivo com o perfil de um app de golpe | nada |

Regras de dependência:
- `core-domain` não usa nenhuma API do Android.
- Nenhuma lógica de score fora do `core-domain`.
- `core-domain` e `core-detection` não podem depender do `app` (serão o SDK para bancos).

## Layers and Responsibilities (dentro do `app`)

| Layer | Onde | Responsibility |
|-------|------|----------------|
| Interface | `app` | Telas em Compose, um ViewModel por tela, estado exposto como fluxo imutável |
| Domínio | módulos `core-*` | Casos de uso (ex.: `ScanDevice`) e motor de score |
| Dados | `core-data` | Repositórios sobre Room e DataStore |

## Module & Class Map

TODO: no code yet. Contratos definidos na especificação **(planned)**:

```kotlin
interface AppInventory { suspend fun installedApps(): List<InstalledApp> }
interface SignalCollector { suspend fun collect(app: InstalledApp): Set<Signal> }
interface RiskScorer { fun score(app: InstalledApp, signals: Set<Signal>, ctx: ScoringContext): RiskResult }
interface ScanDevice { suspend operator fun invoke(trigger: ScanTrigger): ScanReport }
interface CardReader { suspend fun read(tag: NfcTagHandle): CardReadResult }
```

ViewModels, repositórios, DAOs, entidades Room, workers, receptores e módulos Hilt: TODO: no code yet.

## Scan Flow (planned)

1. O ViewModel chama o caso de uso `ScanDevice`.
2. `ScanDevice` pede o inventário de apps ao `AppInventory`.
3. Cada `SignalCollector` devolve os sinais de cada app.
4. O `RiskScorer` (domínio puro) calcula score e nível.
5. O resultado é gravado como nova varredura e comparado com a anterior.
6. A interface observa o repositório e se atualiza.

## Signal Collection (planned, especificação §8)

| Sinal | API do Android |
| --- | --- |
| Inventário | `PackageManager.getInstalledPackages` com permissões e serviços; filtrar `FLAG_SYSTEM` |
| S1 origem | `getInstallSourceInfo` (API 30+) ou `getInstallerPackageName` (abaixo); comparar com a lista de lojas |
| S2 NFC | `android.permission.NFC` em `requestedPermissions` |
| S3 HCE | `queryIntentServices` com `android.nfc.cardemulation.action.HOST_APDU_SERVICE` |
| S4 acessibilidade | `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, lida pelo inventário uma vez por varredura (ADR-019) |
| S5 sobreposição | `SYSTEM_ALERT_WINDOW` em `requestedPermissions` |
| S6 recência | `firstInstallTime` e `lastUpdateTime` do `PackageInfo` |
| S7 pagamento padrão | `CardEmulation.isDefaultServiceForCategory` com `CATEGORY_PAYMENT` para cada serviço HCE. Android 15+: papel de carteira, confirmar na SPEC-016. Isolado em um coletor, degrada sem erro |
| S8 SMS | `READ_SMS` ou `RECEIVE_SMS` em `requestedPermissions` |
| S9 imitação | Nome de exibição normalizado contra a lista de bancos; pacote contra o oficial |

## Card Reading (planned, F2)

- `NfcAdapter.enableReaderMode` com NFC-A e NFC-B, sem checagem NDEF, só enquanto T6 está visível.
- `IsoDep`: selecionar PPSE, selecionar a aplicação, obter opções de processamento (GPO), ler registros.
- Parser TLV extrai nome da aplicação, número e validade. O número é mascarado no `core-nfc`: a interface só recebe os 4 últimos dígitos.

## Background Monitoring (planned, F3)

- `PeriodicWorkRequest` do WorkManager refaz a varredura e compara com a última gravada.
- Receptor de instalação no manifesto não é confiável desde o Android 8: usar receptor registrado em tempo de execução só como complemento, com o app aberto.
- Varredura também ao abrir o app, para compensar fabricantes que matam tarefas em segundo plano.

## Domain Boundaries
- **Detecção:** inventário, sinais, score, varredura (`core-domain` + `core-detection`). Candidato a SDK.
- **Cartão:** leitura EMV e TLV (`core-nfc`). Nada persiste.
- **Histórico e preferências:** varreduras, decisões do usuário, alertas, configurações (`core-data`).
- **Apresentação e orquestração:** telas, navegação, vigia, notificações (`app`).

## C4 Summary
- **Context:** usuário comum e o sistema Android. Nenhum sistema externo de rede.
- **Containers:** um app Android (`app`) com banco Room e DataStore locais; app isca (`decoy-app`) só para testes.
- **Components:** os quatro módulos `core-*` acima.

## External Integrations

| System | Protocol | Purpose |
|--------|----------|---------|
| APIs do Android (`PackageManager`, `Settings.Secure`, `CardEmulation`) | Chamadas locais | Coleta de sinais |
| `NfcAdapter` / `IsoDep` | NFC modo leitor (ISO-DEP) | Teste do cartão |
| WorkManager | Agendador do sistema | Vigia periódica |
| Room / DataStore | Armazenamento local | Histórico e preferências |

Sem rede: o app não declara `INTERNET`.

## Key Design Decisions
Ver `decisions.md` (ADR-001 a ADR-010).
- Database: Room (histórico) — ADR-006.
- Settings: DataStore — ADR-006.
- DI: Hilt — ADR-005.
- Auth: nenhuma (sem login, sem backend) — ADR-001.
- Messaging: nenhuma.
