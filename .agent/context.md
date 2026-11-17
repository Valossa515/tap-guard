# Project Context

> Fonte: `docs/especificacao.md` (13/11/2026). A SPEC-001 criou o projeto Gradle com os seis módulos, ainda sem funcionalidade: o resto continua **(planned)** até ser implementado.

## Project Overview
- **Name:** `tap-guard` (repositório). Nome de exibição em português: em aberto. Identificador do pacote: `io.github.valossa515.tapguard` (`decoy-app`: `io.github.valossa515.tapguard.decoy`).
- **Type:** App Android, só local, sem backend.
- **Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, corrotinas e Flow, Hilt, Room, DataStore, WorkManager **(planned)**.
- **Build tool:** Gradle 9.7.0 com Kotlin DSL e catálogo de versões (`gradle/libs.versions.toml`), monorepo de seis módulos (criado na SPEC-001).
- **Persistência e injeção (SPEC-004):** Hilt por KSP; banco Room `tapguard.db` versão 1 com as quatro tabelas da seção 9; DataStore `settings` com as quatro preferências; backup do app desligado. Repositórios e retenção na SPEC-018.
- **Runtime version:** Android 8.0 (API 26) ou superior; compileSdk 37 e targetSdk 36 (separados desde a SPEC-003, ADR-013). Kotlin 2.4.21, AGP 9.3.3 (Kotlin embutido nos módulos Android), JDK 21 (toolchain, daemon do Gradle e CI).

## Problem
- Golpistas entram em contato por SMS, WhatsApp ou ligação, se passando por banco ou operadora de cartão.
- A vítima instala um APK fora da loja oficial ("proteção do cartão", "atualização de segurança").
- O app pede para encostar o cartão físico no celular e digitar a senha, lê o cartão por NFC e retransmite os comandos pela internet.
- O celular do comparsa emula o cartão (HCE) em uma maquininha ou caixa eletrônico e paga ou saca.
- Variações cobertas: app que pede para virar o app de pagamento por aproximação padrão; app que usa acessibilidade ou sobreposição de tela para controlar o aparelho.

## Objectives
- Detectar apps de risco já instalados (F1).
- Demonstrar o risco com o cartão do próprio usuário (F2).
- Vigiar novas instalações e trocas do app de pagamento padrão (F3).
- Orientar quem já caiu no golpe (F4).
- Ensinar que nenhum banco pede para encostar o cartão no celular.

## Personas
- **Usuário comum:** pessoa sem conhecimento técnico, no Brasil. Precisa de português simples, frases curtas, sem jargão (sem "HCE", "APDU" na interface).
- **Vítima em potencial ou recente:** quem suspeita que caiu no golpe e precisa de um passo a passo que funcione sem internet.
- Futuro, fora do MVP: bancos que integrariam um SDK com `core-domain` e `core-detection` (seção 14 da especificação).

## Main Flows
1. **Varredura (F1, planned):** ao abrir o app ou em "Verificar agora", `ScanDevice` pede o inventário ao `AppInventory`, cada `SignalCollector` devolve os sinais, o `RiskScorer` calcula score e nível, o resultado é gravado como nova varredura e comparado com a anterior, e a interface observa o repositório. T2 mostra o semáforo; T3 lista amarelos e vermelhos (verdes recolhidos); T4 explica cada sinal e oferece desinstalar, ver no sistema, marcar como confiável.
2. **Teste do cartão (F2, planned):** T5 explica e pede confirmação; T6 liga o modo leitor NFC só enquanto visível, lê o cartão EMV e mostra bandeira/nome da aplicação, 4 últimos dígitos e validade, mais a mensagem educativa. Nada é gravado; dados descartados ao sair da tela.
3. **Vigia (F3, planned):** `PeriodicWorkRequest` (padrão 6 h) refaz a varredura, compara com a última gravada, notifica app novo/atualizado amarelo ou vermelho e troca do pagamento padrão; tocar na notificação abre T4. Receptor de instalação em tempo de execução só como complemento enquanto o app está aberto.
4. **Emergência (F4, planned):** botão "Acho que caí em um golpe" sempre visível em T2; T7 com lista marcável e atalhos para as telas do sistema de NFC e de apps instalados.
5. **Onboarding e configurações (planned):** T1 com três páginas (o golpe, o que o app faz, permissão de notificação); T8 com vigia, intervalo, apps confiáveis, privacidade, versão; T9 com histórico de alertas.

## Available Endpoints
Não se aplica: sem backend e sem API HTTP. As telas T1–T9 estão em `project-reference.md` §4.

## Constraints
- **Privacidade:** o app não declara `INTERNET`; sem backend, cadastro, login, telemetria ou anúncios.
- **Dados de cartão:** nunca persistidos nem logados; só em memória; tela do teste marcada como segura contra captura; número mascarado já no `core-nfc`.
- **Limites do Android:** um app não consegue observar nem bloquear o uso de NFC por outro app, nem medir se uma leitura está sendo retransmitida. A defesa é por perfil estático do app instalado e por educação.
- **Fora do modelo de ameaça:** malware bancário sem relação com NFC, phishing por site, golpes de Pix, aparelhos com root ou comprometidos.
- **Fora do escopo do MVP:** iOS; alerta em tempo real pelo app em primeiro plano; bloqueio ou desinstalação automática; análise de código de APK, assinaturas de malware ou serviços de reputação; SDK para bancos.
- **Desempenho:** varredura de 300 apps em até 5 s; abertura a frio em até 2 s; leitura do cartão em até 3 s.
- **Bateria:** vigia via agendador do sistema, sem serviço permanente em primeiro plano.
- **Compatibilidade:** API 26+; aparelho sem NFC usa F1, F3 e F4 normalmente (`uses-feature` NFC com `required=false`).
- **Acessibilidade:** TalkBack, contraste AA, fonte escalável; nível de risco sempre por cor + ícone + texto.
- **Loja:** `QUERY_ALL_PACKAGES` é restrita na Play Store e exige declaração como app de segurança.
- **Testabilidade:** lógica de score sem classes do Android; cobertura mínima de 80% de linhas no domínio, exigida pelo Kover no `./gradlew build` e no CI desde a SPEC-002.
- **Transparência:** código aberto, regras de score documentadas no repositório. Licença: em aberto.
- **Processo:** 8 etapas e 38 specs (SPEC-000 a SPEC-074, issues #1 a #38); uma branch e um PR por spec.

## Dependencies
Ver `project-reference.md` §8. Versões: só em `gradle/libs.versions.toml`. Já no catálogo: AGP 9.3.3, Kotlin 2.4.21, ktlint-gradle 14.2.0 (motor 1.8.0), detekt 1.23.8, Kover 0.9.11, JUnit 5.14.4 e JUnit 4.13.2; desde a SPEC-004, KSP 2.3.14, Hilt 2.60.1, Room 2.8.5 e DataStore 1.3.0. As demais bibliotecas de produção entram nas specs que as usam.

## Open Decisions (especificação, seção 13)
- Nome de exibição do app em português. (O identificador do pacote foi decidido na SPEC-001: ADR-011.)
- Licença do código.
- Lista inicial de bancos e carteiras na versão 1.0 (pacotes de carteiras a levantar na SPEC-015).
- Publicar na Play Store já na 1.0 ou só depois de validar com usuários.
- API do pagamento padrão no Android 15+ (papel de carteira): confirmar na SPEC-016.
