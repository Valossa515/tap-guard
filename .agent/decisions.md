# Architecture Decision Records

> Decisões já tomadas na especificação (`docs/especificacao.md`, 13/11/2026). Ainda não implementadas: status **(planned)**. Alternativas só aparecem quando a especificação as cita.

## ADR-001 — Tudo local, sem backend e sem `INTERNET`
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o produto pede confiança de um público leigo e lida com dados de cartão.
- **Decision:** o app não tem backend, não pede cadastro, não envia dados e não declara a permissão `INTERNET`. Sem login, telemetria ou anúncios.
- **Consequences:** a ausência de `INTERNET` no manifesto prova que nada sai do aparelho; um teste automatizado falha se ela aparecer. Listas de lojas, carteiras e bancos ficam embutidas no app. Uma lista de ameaças atualizável (evolução futura) exigirá rever o RNF-01.

## ADR-002 — Defesa por perfil estático e educação, não por interceptação
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o Android não permite a um app observar ou bloquear o uso de NFC por outro app, nem medir se uma leitura está sendo retransmitida.
- **Decision:** detectar por sinais estáticos do app instalado (S1–S9) e ensinar o usuário (F2, F4).
- **Alternatives citadas e excluídas do MVP:** alerta em tempo real pelo app em primeiro plano (exige acesso a uso de apps ou acessibilidade); análise de código de APK, assinaturas de malware ou serviços de reputação; bloqueio/desinstalação automática.
- **Consequences:** risco de falsos negativos (golpe sem HCE nem acessibilidade fica amarelo) e falsos positivos (apps legítimos por APK), mitigados pelas regras de ajuste e pela opção "confiável".

## ADR-003 — Score por soma de pesos com regras de ajuste
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** é preciso classificar apps de forma explicável e calibrável.
- **Decision:** soma dos pesos dos sinais presentes, limitada a 100, ajustada por quatro regras em ordem (sistema ignorado; confiável sempre verde; loja confiável limitada a 29 salvo S7/S9; vermelho exige S1 + sinal de NFC). Pesos em um único arquivo de configuração.
- **Consequences:** regras documentadas no repositório (RNF-09); cinco exemplos da especificação viram testes unitários. Detalhes em `backlog_rules.md`.

## ADR-004 — Monorepo Gradle de seis módulos com domínio puro
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** a detecção deve virar SDK para bancos sem reescrita, e o score deve ser testável sem Android.
- **Decision:** módulos `core-domain` (Kotlin puro), `core-detection`, `core-nfc`, `core-data`, `app`, `decoy-app`. Nada de lógica de score fora do `core-domain`; nada de API do Android dentro dele; `core-domain` e `core-detection` não dependem do `app`.
- **Consequences:** coletores ficam atrás de interfaces (`AppInventory`, `SignalCollector`, `CardReader`); cobertura de 80% no domínio é viável com JUnit 5 puro.

## ADR-005 — Stack Kotlin + Compose + Hilt
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** Kotlin; Gradle Kotlin DSL com catálogo de versões; Jetpack Compose, Material 3, Navigation Compose; corrotinas e Flow; Hilt; um ViewModel por tela com estado como fluxo imutável.
- **Consequences:** versões exatas: TODO: no code yet.

## ADR-006 — Room para histórico, DataStore para preferências
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** Room com as tabelas `scan_run`, `app_snapshot`, `user_decision`, `alert_event`; DataStore com `onboardingDone`, `monitoringEnabled`, `scanIntervalHours`, `lastDefaultPaymentComponent`. Retenção: 10 varreduras, 90 dias de alertas.
- **Consequences:** nenhum dado de cartão entra no banco nem nas preferências.

## ADR-007 — Vigia com WorkManager periódico
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** RNF-04 proíbe serviço permanente em primeiro plano; receptor de instalação no manifesto não é confiável desde o Android 8.
- **Decision:** `PeriodicWorkRequest` (padrão 6 h; opções 1, 6, 12, 24) compara com a última varredura gravada. Receptor em tempo de execução só como complemento com o app aberto; varredura também ao abrir o app.
- **Consequences:** a notificação de um app novo chega no próximo ciclo, não na hora; fabricantes podem atrasar a vigia.

## ADR-008 — Dados de cartão só em memória e mascarados no `core-nfc`
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Context:** o teste do cartão não pode virar ferramenta de leitura de cartão nem vazar dados.
- **Decision:** modo leitor só com T6 visível; número mascarado no `core-nfc` (a interface recebe só os 4 últimos dígitos); nada em disco, log ou análise de falhas; tela marcada como segura; nada exportado.
- **Consequences:** teste automatizado procura o número completo sintético em banco, preferências e logs.

## ADR-009 — Testes com app isca próprio, nunca malware real
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** `decoy-app` inofensivo, sem `INTERNET`, com variantes `nfcOnly`, `hce`, `full`, `lookalike`, instalado por `adb install`.
- **Consequences:** a Etapa 2 entrega a isca antes da interface; testes ponta a ponta usam emulador no CI e aparelho físico.

## ADR-010 — Permissões mínimas
- **Date:** 2026-11-13
- **Status:** Aceita (planned)
- **Decision:** declarar apenas `QUERY_ALL_PACKAGES`, `NFC` e `POST_NOTIFICATIONS`; recurso NFC com `required=false`.
- **Consequences:** `QUERY_ALL_PACKAGES` exige declaração de app de segurança na Play Store; se recusada, distribuição pelo GitHub (o README deve explicar a contradição de instalar por APK).

## Pending decisions (especificação, seção 13)
- Nome de exibição em português e identificador do pacote.
- Licença do código.
- Lista inicial de bancos e carteiras na 1.0.
- Play Store já na 1.0 ou depois de validar com usuários.
- API de pagamento padrão no Android 15+ (SPEC-016).
