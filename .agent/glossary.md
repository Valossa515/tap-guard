# Domain Glossary

> Fonte: `docs/especificacao.md`. A coluna "Source" aponta para a seção da especificação e, quando já existe, para a spec que criou o tipo. Os modelos do domínio existem desde a SPEC-010 (`core-domain`).

## Entities
| Term | Definition | Source |
|------|-----------|--------|
| **InstalledApp** | App instalado que não é do sistema, vindo do inventário (`AppInventory`): pacote, nome, versão, instalador, datas, permissões pedidas, serviços declarados e serviços de HCE (`cardEmulationServices`) | §7, SPEC-010, SPEC-013 |
| **Signal** | Característica estática de um app que indica perfil de golpe (S1–S9); enum com o bit de cada sinal na máscara | §5, §7, SPEC-010 |
| **RiskLevel** | Nível de risco: `GREEN`, `YELLOW`, `RED`, em ordem de gravidade | §5, SPEC-010 |
| **RiskResult** | Resultado do `RiskScorer`: score (0 a 100), nível e sinais de um app | §7, SPEC-010 |
| **ScoringContext** | Contexto passado ao `RiskScorer`: os pacotes marcados como confiáveis pelo usuário (`trustedPackages`) | §7, SPEC-010 |
| **ScannedApp** | Um app avaliado numa varredura: o `InstalledApp` e o seu `RiskResult` | SPEC-010 |
| **ScanReport** | Resultado de uma varredura (`ScanDevice`): gatilho, início, fim e apps avaliados | §7, SPEC-010 |
| **CardReadResult** | Resultado da leitura do cartão (`CardReader`): `Success`, `UnsupportedCard` ou `ReadInterrupted` | §7, SPEC-010 |
| **LastFourDigits** | Os quatro últimos dígitos do cartão, único dado numérico do cartão que sai do `core-nfc` | RF-203, ADR-008, SPEC-010 |
| **NfcTagHandle** | Marcador opaco do cartão aproximado entregue ao `CardReader`; o `core-nfc` embrulha a `Tag` do Android | §7, SPEC-010 |
| **scan_run** | Uma varredura gravada | §9 (planned) |
| **app_snapshot** | Estado de um app em uma varredura (chave `scanId` + `packageName`) | §9 (planned) |
| **user_decision** | Decisão do usuário sobre um app (hoje só "confiável") | §9 (planned) |
| **alert_event** | Alerta emitido; também controla duplicidade | §9 (planned) |

## Enum Values / Status Codes
| Enum | Values | Used in |
|------|--------|---------|
| Gatilho (`ScanTrigger`) | `MANUAL`, `APP_OPEN`, `PERIODIC` | `scan_run.trigger` |
| Decisão | `TRUSTED` | `user_decision.decision` |
| Tipo de alerta | `RISKY_APP`, `DEFAULT_PAYMENT_CHANGED` | `alert_event.type` |
| Nível | verde (0–29), amarelo (30–59), vermelho (60+) | `app_snapshot.level`, `alert_event.level` |
| Variante do app isca | `nfcOnly`, `hce`, `full`, `lookalike` | `decoy-app` |

## Signals
| ID | Significado | Peso | `Signal` (bit) |
|----|-------------|------|----------------|
| S1 | Instalado fora de uma loja confiável | 30 | `UNTRUSTED_INSTALL_SOURCE` (0) |
| S2 | Declara a permissão de NFC | 15 | `NFC_PERMISSION` (1) |
| S3 | Declara serviço de emulação de cartão (HCE) | 20 | `CARD_EMULATION_SERVICE` (2) |
| S4 | Tem serviço de acessibilidade ativo | 25 | `ACCESSIBILITY_SERVICE_ENABLED` (3) |
| S5 | Pede permissão de sobreposição de tela | 10 | `OVERLAY_PERMISSION` (4) |
| S6 | Instalado ou atualizado nos últimos 7 dias | 10 | `RECENTLY_INSTALLED_OR_UPDATED` (5) |
| S7 | É o app de pagamento padrão e não está na lista de carteiras conhecidas | 30 | `UNKNOWN_DEFAULT_PAYMENT_APP` (6) |
| S8 | Pede leitura ou recebimento de SMS | 10 | `SMS_PERMISSION` (7) |
| S9 | Nome parecido com o de um banco conhecido, mas pacote diferente do oficial | 20 | `BANK_LOOKALIKE` (8) |

Os pesos não ficam no enum: ficam no arquivo de configuração do motor de score (SPEC-017).

"Sinal de NFC" = S2, S3 ou S7.

## Business Terms
- **Golpe de retransmissão (relay) de cartão por NFC:** app malicioso lê o cartão da vítima por aproximação e retransmite os comandos pela internet para o celular de um comparsa, que emula o cartão.
- **HCE (Host Card Emulation):** emulação de cartão pelo celular. Termo técnico: não aparece na interface.
- **Serviço de HCE:** serviço que responde à ação `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e exige a permissão `android.permission.BIND_NFC_SERVICE`, inclusive desligado. Fica em `InstalledApp.cardEmulationServices` e gera S3. Off-host e NFC-F não são serviços de HCE para o Tap Guard (ADR-018).
- **APDU:** comando trocado com o cartão. Termo técnico: não aparece na interface.
- **EMV:** padrão dos cartões de pagamento lidos no F2.
- **PPSE:** ambiente de pagamento por aproximação selecionado no início da leitura EMV.
- **TLV:** formato dos dados devolvidos pelo cartão; o parser TLV extrai nome da aplicação, número e validade.
- **Loja confiável:** instalador cujo pacote está na lista `TrustedStores` (SPEC-012): Google Play, Galaxy Store, Xiaomi GetApps (dois pacotes), OPPO App Market (dois pacotes), vivo V-Appstore, HONOR App Market, Palm Store, Huawei AppGallery, Amazon Appstore. Instalador nulo ou fora da lista gera S1. Pacotes e motivos no ADR-017.
- **Carteira conhecida:** Google Wallet, Samsung Wallet e apps oficiais dos grandes bancos e carteiras digitais do Brasil (pacotes a levantar na SPEC-015).
- **Banco conhecido (S9):** par nome de exibição + pacote oficial, embutido no app.
- **App de pagamento padrão:** app que o sistema usa para pagamento por aproximação (categoria de pagamento; no Android 15+, papel de carteira).
- **App confiável:** app que o usuário marcou como confiável; fica sempre verde, sai dos alertas e aparece em lista própria, com opção de desfazer.
- **App isca (`decoy-app`):** app próprio, inofensivo, com o perfil de um app de golpe, usado como alvo de testes.
- **Varredura:** análise de todos os apps instalados que não são do sistema.
- **Vigia:** varredura periódica em segundo plano com notificação.
- **Semáforo do aparelho:** resumo em T2 com o pior nível encontrado.
- **Teste do cartão:** demonstração F2 com o cartão do próprio usuário.
- **Emergência:** passo a passo F4 para quem suspeita que caiu no golpe.
- **Spec / Etapa:** unidade de trabalho (issue `SPEC-0xx`) e milestone do repositório.

## Disambiguation
- **Score vs nível:** score é o número de 0 a 100 após as regras de ajuste; nível é a faixa (verde/amarelo/vermelho) derivada do score.
- **Varredura vs vigia:** varredura é uma execução; vigia é o agendamento periódico que dispara varreduras e notificações.
- **App confiável vs loja confiável:** o primeiro é decisão do usuário sobre um app; o segundo é a origem da instalação (S1).
- **Carteira conhecida vs banco conhecido:** a lista de carteiras alimenta S7; a lista de bancos alimenta S9.
