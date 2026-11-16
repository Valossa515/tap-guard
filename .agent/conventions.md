# Project Conventions

> Fonte: `docs/especificacao.md` (seções 6, 7, 8, 11 e 12). Sem código ainda: padrões que só o código mostra estão como `TODO: no code yet`. Para padrões de código não fixados aqui, vale o skill base [skills/android/SKILL.md](skills/android/SKILL.md).

## Naming
- **Classes, interfaces e funções:** seguem a tabela "Naming Conventions" de skills/android/SKILL.md. Nomes já definidos na especificação: `AppInventory`, `SignalCollector`, `RiskScorer`, `ScanDevice`, `CardReader`, `InstalledApp`, `Signal`, `ScoringContext`, `RiskResult`, `ScanTrigger`, `ScanReport`, `NfcTagHandle`, `CardReadResult`.
- **Casos de uso:** `ScanDevice` usa `suspend operator fun invoke(...)` (único exemplo na especificação).
- **Tabelas Room:** snake_case (`scan_run`, `app_snapshot`, `user_decision`, `alert_event`); colunas em camelCase (`startedAt`, `packageName`).
- **Valores de enum persistidos:** UPPER_SNAKE (`MANUAL`, `APP_OPEN`, `PERIODIC`, `TRUSTED`, `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED`).
- **Chaves de preferência:** camelCase (`onboardingDone`, `monitoringEnabled`, `scanIntervalHours`, `lastDefaultPaymentComponent`).
- **Módulos Gradle:** kebab-case com prefixo `core-` (`core-domain`, `core-detection`, `core-nfc`, `core-data`), mais `app` e `decoy-app`.
- **IDs de rastreio:** requisitos `RF-xxx` e `RNF-xx`; sinais `S1`–`S9`; telas `T1`–`T9`; specs `SPEC-0xx`.
- **Branches e PRs:** uma branch e um PR por spec, com o ID da spec no título. Formato do nome da branch: `spec-NNN-descricao-curta` (ex.: `spec-001-estrutura`).

## Folder / Package Structure
Planejado (módulos); pacotes internos TODO: no code yet.
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
- **Soft delete:** não previsto; retenção por limpeza (10 varreduras, 90 dias de alertas).
- **Audit fields:** timestamps específicos por tabela (`startedAt`, `finishedAt`, `decidedAt`, `createdAt`, `readAt`); sem padrão genérico.
- **Sinais persistidos** como máscara de bits em `app_snapshot.signals`. S1 no bit 0 até S9 no bit 8. A ordem não muda depois de publicada.

## Testing
- **Framework:** JUnit 5 no `core-domain`; JUnit 4 nos módulos Android (Robolectric e instrumentados).
- **Mock library:** MockK. **Flow:** Turbine. **UI:** testes do Compose.
- **Integration tests:** Robolectric com `PackageManager` simulado e Room em memória.
- **Ponta a ponta:** instalar o `decoy-app` (por `adb install`), rodar a varredura, conferir nível e notificação — emulador no CI e aparelho físico.
- **Manual:** leitura de cartão real em aparelho físico com NFC.
- **Coverage:** mínimo de 80% no módulo de domínio. Ferramenta: Kover (a confirmar na SPEC-002).
- **Test naming:** frase entre crases nos testes de JVM e Robolectric; camelCase nos instrumentados. Ver skills/android/testing.md.
- **Dados de teste:** nunca malware real; nunca dados reais de cartão (vetores TLV sintéticos com número de cartão de teste).

## Dependency Injection
- **Container:** Hilt, configurado no `app`.
- **Style:** injeção por construtor. Módulos Hilt só ligam interface a implementação e fornecem tipos de terceiros. Classes do `core-domain` não têm anotações do Hilt.

## Error Handling
- **Strategy:** estado de erro em toda tela com dados; no F2, mensagem própria para ausência de NFC, NFC desligado (com atalho para as configurações), cartão não suportado e leitura interrompida.
- Falha da API de pagamento padrão em versões novas do Android degrada sem erro.
- **Custom exceptions / tipos de resultado:** TODO: no code yet (`CardReadResult` e `RiskResult` são os únicos tipos de resultado nomeados).
- **Logging:** nunca logar dados de cartão nem enviá-los a análise de falhas. Biblioteca: TODO: no code yet.

## Quality Gates
- ktlint e detekt e a suíte de testes passam antes de uma spec ser dada como concluída.
- CI no GitHub Actions.
- Toda spec entrega código, testes e atualização da documentação do módulo.
