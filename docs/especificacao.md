# Tap Guard: especificação do MVP e descrição técnica

Versão inicial: 13/11/2026. Este documento é a fonte de contexto para o bootstrap do SDD. O detalhe de cada spec (entregável e critérios de aceite) fica nas issues do repositório.

## 1. Visão geral

O Tap Guard é um app Android que avisa o usuário comum quando há no aparelho um app com o perfil dos usados em golpes de retransmissão de cartão por NFC, e ensina na prática por que nenhum banco pede para encostar o cartão no celular.

- **Problema:** golpistas convencem a vítima a instalar um APK fora da loja, encostar o cartão no celular e digitar a senha. O app retransmite o cartão para um comparsa, que paga ou saca.
- **Público:** pessoas sem conhecimento técnico, com foco no Brasil. Texto em português simples, sem jargão.
- **Objetivo do MVP:** detectar apps de risco já instalados, vigiar novas instalações, demonstrar o risco com o cartão do próprio usuário e orientar quem já caiu no golpe.
- **Princípio de confiança:** tudo roda no aparelho. O app não tem backend, não pede cadastro e não envia dados.
- **Nome:** o repositório é `tap-guard`. O nome de exibição do app em português ainda está em aberto (seção 13).

## 2. Modelo de ameaça

O app cobre o golpe de retransmissão (relay) de cartão por NFC no lado da vítima. Ele não substitui antivírus nem o antifraude do banco.

**Fluxo do golpe**

1. Contato por SMS, WhatsApp ou ligação, se passando por banco ou operadora de cartão.
2. A vítima instala um APK fora da loja oficial ("proteção do cartão", "atualização de segurança").
3. O app pede para encostar o cartão físico no celular e digitar a senha.
4. O app lê o cartão por NFC e retransmite os comandos pela internet.
5. O celular do comparsa emula o cartão (HCE) em uma maquininha ou caixa eletrônico.

**Variações cobertas**

- App malicioso que pede para virar o app de pagamento por aproximação padrão.
- App que usa serviço de acessibilidade ou sobreposição de tela para controlar o aparelho.

**O que o Android não permite (limites do produto)**

- Um app não consegue observar nem bloquear o uso de NFC por outro app.
- Não há como medir, no lado da vítima, se uma leitura está sendo retransmitida.
- Por isso a defesa é por perfil do app instalado (sinais estáticos) e por educação, e não por interceptação.

**Fora do modelo de ameaça**

- Malware bancário sem relação com NFC, phishing por site e golpes de Pix.
- Aparelhos com root ou comprometidos no nível do sistema.

## 3. Escopo do MVP

O MVP tem quatro funcionalidades, todas locais, só para Android.

**Entra**

- **F1 Varredura de apps:** analisa os apps instalados e classifica cada um em verde, amarelo ou vermelho.
- **F2 Teste do cartão:** o usuário encosta o próprio cartão e vê que dados saem por aproximação.
- **F3 Vigia em segundo plano:** notifica quando surge um app de risco ou quando o app de pagamento padrão muda.
- **F4 Emergência:** passo a passo para quem suspeita que caiu no golpe.
- Onboarding curto e tela de configurações.

**Fica fora**

- iOS, backend, login, telemetria e anúncios.
- Alerta em tempo real com base no app em primeiro plano (exige acesso a uso de apps ou acessibilidade).
- Bloqueio ou desinstalação automática de apps. O app só leva o usuário à tela do sistema.
- Análise do código dos APKs, assinaturas de malware ou consulta a serviços de reputação.
- SDK para bancos (seção 14).

## 4. Requisitos funcionais

Cada requisito tem um ID que as specs referenciam.

### F1 Varredura de apps

- **RF-101:** listar todos os apps instalados que não são do sistema. O próprio app (o Tap Guard, ou o app que usa o SDK) também fica fora da lista.
- **RF-102:** para cada app, coletar os sinais S1 a S9 da seção 5.
- **RF-103:** calcular score e nível (verde, amarelo, vermelho) conforme a seção 5.
- **RF-104:** mostrar um resumo do aparelho: o pior nível encontrado e a contagem por nível.
- **RF-105:** listar apps amarelos e vermelhos, do maior score para o menor. Verdes ficam em lista recolhida.
- **RF-106:** no detalhe do app, explicar cada sinal encontrado em uma frase simples.
- **RF-107:** oferecer ações no detalhe: abrir a tela de desinstalação do sistema, abrir as informações do app, marcar como confiável.
- **RF-108:** app marcado como confiável sai dos alertas e aparece em lista própria, com opção de desfazer.
- **RF-109:** a varredura roda ao abrir o app e por botão "Verificar agora".

Critérios de aceite de F1:

- Com o app isca instalado por APK, a varredura o mostra em vermelho com os sinais corretos.
- Carteiras e apps de banco instalados pela loja oficial aparecem em verde.
- A varredura de 300 apps termina em até 5 segundos em aparelho intermediário.

### F2 Teste do cartão

- **RF-201:** explicar o teste antes de começar e pedir confirmação.
- **RF-202:** ler um cartão EMV por aproximação usando o modo leitor do NFC.
- **RF-203:** mostrar bandeira ou nome da aplicação, os quatro últimos dígitos e a validade. Nunca o número completo.
- **RF-204:** exibir a mensagem educativa: "Foi assim, encostando, que um app conseguiu ler seu cartão. Nenhum banco pede isso."
- **RF-205:** não gravar nenhum dado do cartão em disco, log ou análise de falhas. Os dados vivem só em memória e são descartados ao sair da tela.
- **RF-206:** tratar ausência de NFC, NFC desligado, cartão não suportado e leitura interrompida, com mensagem própria para cada caso.

Critérios de aceite de F2:

- Leitura concluída em até 3 segundos com cartão Visa ou Mastercard por aproximação.
- Nenhuma ocorrência do número completo em banco local, preferências, logcat ou captura de tela (tela marcada como segura).

### F3 Vigia em segundo plano

- **RF-301:** refazer a varredura periodicamente (padrão: a cada 6 horas) e comparar com a anterior.
- **RF-302:** notificar quando um app novo ou atualizado ficar amarelo ou vermelho.
- **RF-303:** notificar quando o app de pagamento por aproximação padrão mudar.
- **RF-304:** tocar na notificação abre o detalhe do app em questão.
- **RF-305:** o usuário pode desligar a vigia e mudar o intervalo nas configurações.
- **RF-306:** manter histórico local dos alertas emitidos.

Critérios de aceite de F3:

- Instalar o app isca com o Tap Guard fechado gera notificação no próximo ciclo da vigia.
- Nenhuma notificação repetida para o mesmo app na mesma versão.

### F4 Emergência

- **RF-401:** botão "Acho que caí em um golpe" sempre visível na tela inicial.
- **RF-402:** passo a passo em lista marcável: desligar o NFC, desinstalar o app suspeito, bloquear o cartão pelo app oficial ou telefone do verso do cartão, contestar transações, registrar boletim de ocorrência.
- **RF-403:** atalhos para as telas do sistema de NFC e de apps instalados.
- **RF-404:** o conteúdo funciona sem internet.

### Geral

- **RF-501:** onboarding de três telas: o que é o golpe, o que o app faz, pedido de permissão de notificação.
- **RF-502:** configurações: vigia ligada ou desligada, intervalo, apps confiáveis, política de privacidade, versão.

## 5. Regras do score de risco

O score de um app é a soma dos pesos dos sinais presentes, limitada a 100, e depois ajustada pelas regras abaixo. Os pesos são um ponto de partida e ficam em um único arquivo de configuração para calibrar com testes.

| Sinal | Descrição | Peso |
| --- | --- | --- |
| S1 | Instalado fora de uma loja confiável | 30 |
| S2 | Declara a permissão de NFC | 15 |
| S3 | Declara serviço de emulação de cartão (HCE) | 20 |
| S4 | Tem serviço de acessibilidade ativo | 25 |
| S5 | Pede permissão de sobreposição de tela | 10 |
| S6 | Instalado ou atualizado nos últimos 7 dias | 10 |
| S7 | É o app de pagamento padrão e não está na lista de carteiras conhecidas | 30 |
| S8 | Pede leitura ou recebimento de SMS | 10 |
| S9 | Nome parecido com o de um banco conhecido, mas pacote diferente do oficial | 20 |

**Faixas**

- Verde: 0 a 29.
- Amarelo: 30 a 59.
- Vermelho: 60 ou mais.

**Regras de ajuste, nesta ordem**

1. Apps do sistema são ignorados.
2. App marcado como confiável pelo usuário fica sempre verde.
3. App instalado por loja confiável tem score limitado a 29, a não ser que tenha S7 ou S9.
4. Vermelho exige S1 junto com pelo menos um sinal de NFC (S2, S3 ou S7). Sem isso, o teto é amarelo (59).

**Listas de apoio, embutidas no app**

- Lojas confiáveis (pacote do instalador): Google Play (`com.android.vending`), Galaxy Store (`com.sec.android.app.samsungapps`), Xiaomi GetApps (`com.xiaomi.mipicks` e `com.xiaomi.market`), OPPO App Market (`com.heytap.market` e `com.oppo.market`), vivo V-Appstore (`com.vivo.appstore`), HONOR App Market (`com.hihonor.appmarket`), Palm Store (`com.transsnet.store`), Huawei AppGallery (`com.huawei.appmarket`) e Amazon Appstore (`com.amazon.venezia`). Não são lojas: os instaladores de pacote do sistema e o shell (o que aparece num APK instalado à mão ou por adb), F-Droid, Aurora Store e apps de clonagem. Instalador desconhecido conta como fora da loja. Motivos e fontes no ADR-017 (SPEC-012).
- Carteiras conhecidas (pacote que pode ser o app de pagamento padrão sem gerar S7): Carteira do Google (`com.google.android.apps.walletnfcrel`), Samsung Wallet (`com.samsung.android.spay`) e os apps de banco que fazem Pix por aproximação no próprio app: Nubank (`com.nu.production`), Itaú (`com.itau`), Banco do Brasil (`br.com.bb.android`) e Inter (`br.com.intermedium`). Os bancos que fazem o Pix por aproximação pela Carteira do Google (Mercado Pago, PicPay, Bradesco, Caixa, Santander e C6) não entram. Motivos e fontes no ADR-020 (SPEC-015).
- Bancos conhecidos para S9: nome de exibição e pacote oficial de 29 apps das 14 instituições com mais clientes no ranking do Banco Central (dez/2025): Caixa, Nubank, Bradesco, Itaú, Banco do Brasil, Santander, Mercado Pago, PicPay, Inter, PagBank, C6 Bank, BTG Pactual, 99Pay e Neon, com os apps oficiais delas que levam o nome da marca (Empresas, Cartões, Personnalité, CAIXA Tem, next, BTG Pactual Investimentos). Um pacote pode ter mais de um nome (por exemplo, "BB" e "Banco do Brasil"). Lista completa, pacotes e fontes no ADR-020 (SPEC-015).

**Exemplos para os testes**

- APK de fora da loja com NFC e HCE: 30 + 15 + 20 = 65, vermelho.
- Mesmo app, instalado ontem e com acessibilidade ativa: 100, vermelho.
- App de banco da loja oficial com NFC e HCE: 35, limitado a 29, verde.
- Jogo instalado por APK sem NFC: 30, amarelo.
- App de fora da loja com acessibilidade e SMS, sem NFC: 65, limitado a 59, amarelo.

## 6. Requisitos não funcionais

- **RNF-01 Privacidade:** o app não declara a permissão de internet. Isso prova, no manifesto, que nada sai do aparelho.
- **RNF-02 Dados de cartão:** nunca persistidos nem logados. Tela do teste marcada como segura contra captura.
- **RNF-03 Desempenho:** varredura de 300 apps em até 5 segundos. Abertura a frio em até 2 segundos.
- **RNF-04 Bateria:** vigia via agendador do sistema, sem serviço permanente em primeiro plano.
- **RNF-05 Compatibilidade:** Android 8.0 (API 26) ou superior. Aparelho sem NFC usa F1, F3 e F4 normalmente.
- **RNF-06 Acessibilidade:** compatível com TalkBack, contraste AA, fonte escalável. O nível de risco nunca é comunicado só por cor: sempre cor, ícone e texto.
- **RNF-07 Linguagem:** português do Brasil, frases curtas, sem termos como HCE ou APDU na interface. Textos em arquivo de recursos para tradução futura.
- **RNF-08 Testabilidade:** o núcleo de detecção não depende de classes do Android na lógica de score. Cobertura mínima de 80% no módulo de domínio.
- **RNF-09 Transparência:** código aberto, com as regras de score documentadas no repositório.

## 7. Arquitetura

O projeto é um monorepo Gradle com seis módulos. A regra central: a lógica de detecção fica isolada em módulos sem interface gráfica, para virar SDK depois sem reescrita.

| Módulo | Tipo | Responsabilidade | Depende de |
| --- | --- | --- | --- |
| `core-domain` | Kotlin puro | Modelos (app, sinal, score, nível), motor de score, interfaces dos coletores | nada |
| `core-detection` | Biblioteca Android | Coletores de sinais sobre as APIs do Android, caso de uso de varredura | `core-domain` |
| `core-nfc` | Biblioteca Android | Leitura EMV por aproximação e parser TLV | `core-domain` |
| `core-data` | Biblioteca Android | Banco local, preferências, repositórios, histórico | `core-domain` |
| `app` | Aplicativo | Telas, navegação, vigia agendada, notificações, injeção de dependências | todos os `core-*` |
| `decoy-app` | Aplicativo de teste | App isca inofensivo com o perfil de um app de golpe | nada |

**Camadas dentro do `app`**

- Interface: telas em Compose, um ViewModel por tela, estado exposto como fluxo imutável.
- Domínio: casos de uso vindos dos módulos `core-*`.
- Dados: repositórios do `core-data`.

**Fluxo da varredura**

1. O ViewModel chama o caso de uso `ScanDevice`.
2. `ScanDevice` pede o inventário de apps ao `AppInventory`.
3. Cada `SignalCollector` devolve os sinais de cada app.
4. O `RiskScorer` (domínio puro) calcula score e nível.
5. O resultado é gravado como nova varredura e comparado com a anterior.
6. A interface observa o repositório e se atualiza.

**Contratos principais do domínio**

```kotlin
interface AppInventory { suspend fun installedApps(): List<InstalledApp> }
interface SignalCollector { suspend fun collect(app: InstalledApp): Set<Signal> }
interface RiskScorer { fun score(app: InstalledApp, signals: Set<Signal>, ctx: ScoringContext): RiskResult }
interface ScanDevice { suspend operator fun invoke(trigger: ScanTrigger): ScanReport }
interface CardReader { suspend fun read(tag: NfcTagHandle): CardReadResult }
```

## 8. Stack técnica, APIs e permissões

**Stack**

- Linguagem: Kotlin. Build: Gradle com Kotlin DSL e catálogo de versões.
- Interface: Jetpack Compose, Material 3, Navigation Compose.
- Concorrência: corrotinas e Flow.
- Injeção de dependências: Hilt.
- Persistência: Room (histórico) e DataStore (preferências).
- Segundo plano: WorkManager.
- Testes: JUnit 5 no `core-domain`; JUnit 4 nos módulos Android (Robolectric e testes instrumentados); MockK, Turbine e testes de interface do Compose.
- Qualidade: ktlint, detekt, GitHub Actions.
- SDK mínimo 26. SDK alvo: o mais recente exigido pela Play Store na data da entrega.

**Como cada sinal é coletado**

| Sinal | API do Android |
| --- | --- |
| Inventário | Cinco etapas: `PackageManager.getInstalledPackages` sem flags, filtrando apps com `FLAG_SYSTEM` ou `FLAG_UPDATED_SYSTEM_APP` e o pacote do próprio app; uma consulta dos serviços de HCE de todos os pacotes (linha S3); uma leitura dos serviços de acessibilidade ligados (linha S4); a consulta do pagamento padrão (linha S7); depois `getPackageInfo` de cada app de usuário com permissões e serviços (inclusive desligados) e o instalador |
| S1 origem | `getInstallSourceInfo` (API 30+) ou `getInstallerPackageName` (abaixo); comparar com a lista de lojas |
| S2 NFC | `android.permission.NFC` em `requestedPermissions` |
| S3 HCE | Uma consulta `queryIntentServices` por varredura com a ação `android.nfc.cardemulation.action.HOST_APDU_SERVICE` e `MATCH_DISABLED_COMPONENTS`, ficando com os serviços que exigem `android.permission.BIND_NFC_SERVICE` (inclusive desligados). Off-host e NFC-F não contam; não exige serviço exportado, meta-data nem a permissão `NFC` no pacote (ADR-018) |
| S4 acessibilidade | Uma leitura de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` por varredura: o que o usuário ligou, inclusive um serviço que caiu. Cada entrada passa por `ComponentName.unflattenFromString` (formatos completo e curto); vazias e inválidas são ignoradas; agrupado por pacote e cruzado com os apps de usuário, sem exigir a classe entre os serviços declarados. Chave nula = nenhum serviço ligado. Legível sem permissão da API 26 à 36. Substitui o `AccessibilityManager.getEnabledAccessibilityServiceList`, que só lista os conectados (ADR-019) |
| S5 sobreposição | `SYSTEM_ALERT_WINDOW` em `requestedPermissions` |
| S6 recência | `firstInstallTime` e `lastUpdateTime` do `PackageInfo` |
| S7 pagamento padrão | `CardEmulation.isDefaultServiceForCategory` com `CATEGORY_PAYMENT` para cada serviço de HCE de app de usuário que está ligado e exportado. "Ligado" vem de uma segunda consulta de HCE sem `MATCH_DISABLED_COMPONENTS` (o `ServiceInfo.enabled` é o valor do manifesto). Até a API 34, o sistema compara com o componente escolhido nas configurações de NFC; da API 35 em diante, com o papel de carteira, responde `true` para todo serviço registrado do app dono do papel. A chave `nfc_payment_default_component` do `Settings.Secure` não serve: é `@hide` sem `@Readable` (`SecurityException` da API 31 em diante). Sem NFC, sem HCE ou com o serviço NFC morto, nenhum app é o padrão, sem erro; sem a permissão `NFC`, a `SecurityException` propaga. S7 quando o pacote está fora de `KnownWallets`: `DefaultPaymentAppCollector` (SPEC-016, ADR-021) |
| S8 SMS | `READ_SMS` ou `RECEIVE_SMS` em `requestedPermissions` |
| S9 imitação | Nenhuma API: `label` (do `loadLabel`) e `packageName` que o inventário já leu. O rótulo vira um esqueleto (NFKC, corte no primeiro separador, minúsculas, sem acentos, letras parecidas trocadas, só `a-z0-9`) e é comparado, com e sem os prefixos e sufixos da lista fechada, aos nomes de `KnownBanks.apps` pela distância OSA: até 5 letras, só igual; de 6 a 9, 1 edição; 10 ou mais, 2. O pacote de qualquer entrada da lista tira o S9, com comparação exata. `BankLookalikeCollector` (SPEC-015, ADR-020) |

**Leitura do cartão (F2)**

- `NfcAdapter.enableReaderMode` com NFC-A e NFC-B, sem checagem NDEF, só enquanto a tela do teste está visível.
- `IsoDep` para trocar comandos: selecionar o ambiente de pagamento por aproximação (PPSE), selecionar a aplicação, obter opções de processamento e ler registros.
- Parser TLV extrai nome da aplicação, número do cartão e validade. O número é mascarado ainda no `core-nfc`: a interface só recebe os quatro últimos dígitos.

**Vigia (F3)**

- `PeriodicWorkRequest` do WorkManager refaz a varredura e compara com a última gravada.
- Receptor de instalação de pacotes registrado no manifesto não é confiável desde o Android 8. Usar receptor registrado em tempo de execução só como complemento, enquanto o app está aberto.

**Permissões declaradas**

| Permissão | Motivo |
| --- | --- |
| `QUERY_ALL_PACKAGES` | Listar os apps instalados. Restrita na Play Store: exige declaração de uso como app de segurança |
| `NFC` | Teste do cartão e consulta do pagamento padrão |
| `POST_NOTIFICATIONS` | Alertas da vigia (Android 13+) |

O app não declara `INTERNET`. O recurso de NFC é marcado como não obrigatório (`uses-feature` com `required=false`).

## 9. Modelo de dados local

O banco Room guarda quatro tabelas e o DataStore guarda as preferências. Nenhum dado de cartão entra aqui.

| Tabela | Campos | Uso |
| --- | --- | --- |
| `scan_run` | `id`, `startedAt`, `finishedAt`, `trigger` (MANUAL, APP_OPEN, PERIODIC), `totalApps`, `redCount`, `yellowCount` | Uma linha por varredura |
| `app_snapshot` | `scanId`, `packageName`, `label`, `versionCode`, `installer`, `firstInstallTime`, `lastUpdateTime`, `signals` (máscara de bits), `score`, `level` | Estado de cada app em cada varredura. Chave: `scanId` + `packageName` |
| `user_decision` | `packageName`, `decision` (TRUSTED), `decidedAt` | Apps marcados como confiáveis |
| `alert_event` | `id`, `type` (RISKY_APP, DEFAULT_PAYMENT_CHANGED), `packageName`, `versionCode`, `level`, `createdAt`, `readAt` | Histórico de alertas e controle de duplicidade |

**Preferências (DataStore)**

- `onboardingDone`: booleano.
- `monitoringEnabled`: booleano, padrão verdadeiro.
- `scanIntervalHours`: inteiro, padrão 6, opções 1, 6, 12 e 24.
- `lastDefaultPaymentComponent`: texto, para detectar a troca.

**Retenção**

- Manter as 10 varreduras mais recentes e apagar as anteriores.
- Manter alertas por 90 dias.

## 10. Telas e fluxos

O app tem nove telas. A tela inicial resolve o caso mais comum em um toque: saber se o aparelho está seguro.

| ID | Tela | Conteúdo | Sai para |
| --- | --- | --- | --- |
| T1 | Onboarding | Três páginas: o golpe, o que o app faz, permissão de notificação | T2 |
| T2 | Início | Semáforo do aparelho com ícone e frase, botão "Verificar agora", cartão "Testar meu cartão", botão "Acho que caí em um golpe", data da última verificação | T3, T5, T7, T8 |
| T3 | Resultado | Lista de apps vermelhos e amarelos; verdes recolhidos | T4 |
| T4 | Detalhe do app | Nome, ícone, nível, sinais explicados, ações: desinstalar, ver no sistema, marcar como confiável | Telas do sistema |
| T5 | Teste do cartão: explicação | O que vai acontecer e o que será mostrado | T6 |
| T6 | Teste do cartão: leitura e resultado | Animação "aproxime o cartão", depois bandeira, final do número, validade e mensagem educativa | T2 |
| T7 | Emergência | Lista marcável de passos com atalhos | Telas do sistema |
| T8 | Configurações | Vigia, intervalo, apps confiáveis, privacidade, versão | T9 |
| T9 | Histórico de alertas | Alertas emitidos, do mais recente para o mais antigo | T4 |

**Estados obrigatórios em toda tela com dados**

- Carregando, vazio, conteúdo e erro.
- T2 sem varredura anterior mostra "Ainda não verificamos seu aparelho".
- T6 trata: sem NFC, NFC desligado (atalho para as configurações), cartão não suportado e leitura interrompida.

**Textos dos níveis**

- Verde: "Nada suspeito encontrado."
- Amarelo: "Encontramos apps que merecem atenção."
- Vermelho: "Encontramos um app com perfil de golpe. Veja o que fazer."

**Notificações**

- App de risco: "Um app instalado há pouco tem perfil de golpe. Toque para ver."
- Pagamento padrão: "O app de pagamento por aproximação do seu celular mudou. Foi você?"

## 11. Estratégia de testes

Os testes não usam malware real. O alvo é um app isca próprio, inofensivo, que reproduz o perfil de um app de golpe.

**App isca (`decoy-app`)**

- Declara a permissão de NFC e um serviço HCE vazio, que não responde a nenhum comando.
- Não tem permissão de internet e não lê nada. A única tela diz "App de teste do Tap Guard".
- Variantes de build para cobrir combinações de sinais:
  - `nfcOnly`: só a permissão de NFC.
  - `hce`: NFC e serviço HCE.
  - `full`: NFC, HCE, serviço de acessibilidade vazio, sobreposição e SMS declarados.
  - `lookalike`: nome de exibição parecido com o de um banco fictício da lista de testes.
- Instalado por `adb install`, o que simula a origem fora da loja.

**Níveis de teste**

| Nível | Alvo | Ferramenta |
| --- | --- | --- |
| Unitário | Motor de score, regras de ajuste, parser TLV, comparação entre varreduras | JUnit 5, MockK |
| Integração | Coletores com `PackageManager` simulado, repositórios Room em memória | Robolectric (JUnit 4) |
| Interface | Estados de cada tela e navegação | Testes do Compose |
| Ponta a ponta | Instalar a isca, rodar a varredura, conferir nível e notificação | Emulador no CI e aparelho físico |
| Manual | Leitura de cartão real por aproximação | Aparelho físico com NFC |

**Casos fixos do motor de score**

- Os cinco exemplos da seção 5 viram testes unitários com o resultado esperado.
- Vetores de teste do parser TLV usam respostas sintéticas, com número de cartão de teste, nunca dados reais.

**Verificações de privacidade automatizadas**

- Teste que falha se o manifesto final declarar `INTERNET`.
- Teste que procura o número completo do cartão sintético em banco, preferências e logs depois de uma leitura.

## 12. Plano de desenvolvimento por etapas

São 8 etapas e 38 specs. Cada spec é uma issue com o ID no título, entregável, critérios de aceite em checklist e dependências. As etapas são as milestones do repositório.

**Convenções para todas as specs**

- Toda spec entrega código, testes e atualização da documentação do módulo.
- Toda spec passa em ktlint, detekt e na suíte de testes antes de ser dada como concluída.
- Uma branch e um pull request por spec, com o ID no título. O pull request fecha a issue.
- Nada de lógica de score fora do `core-domain`. Nada de API do Android dentro do `core-domain`.

| Etapa | Objetivo | Specs (issues) |
| --- | --- | --- |
| 0. Fundação | Repositório que compila, testa e publica artefato de debug no CI | SPEC-000 a SPEC-004 (#1 a #5) |
| 1. Núcleo de detecção | Lista de apps com sinais, score e nível, sem interface | SPEC-010 a SPEC-018 (#6 a #14) |
| 2. App isca | Alvo de teste reproduzível antes da interface | SPEC-020 a SPEC-022 (#15 a #17) |
| 3. Varredura na interface (F1) | O usuário entende em segundos se há risco | SPEC-030 a SPEC-033 (#18 a #21) |
| 4. Teste do cartão (F2) | Demonstrar o risco sem guardar nada | SPEC-040 a SPEC-043 (#22 a #25) |
| 5. Vigia em segundo plano (F3) | Avisar sem o usuário abrir o app | SPEC-050 a SPEC-054 (#26 a #30) |
| 6. Emergência e onboarding (F4) | Orientar quem caiu no golpe e apresentar o app | SPEC-060 a SPEC-062 (#31 a #33) |
| 7. Qualidade e lançamento | Versão 1.0 instalável e documentada | SPEC-070 a SPEC-074 (#34 a #38) |

**Ordem e paralelismo**

- Caminho crítico: Etapa 0, Etapa 1, Etapa 3, Etapa 5, Etapa 7.
- A Etapa 2 pode andar junto com a Etapa 1 a partir da SPEC-001.
- A Etapa 4 só depende do domínio e pode andar em paralelo com as Etapas 3 e 5.
- A SPEC-060 pode ser feita a qualquer momento depois da SPEC-003.

**Primeiro marco demonstrável**

Ao fim da SPEC-032 já existe um app que instala, varre o aparelho e mostra a isca em vermelho com a explicação. É o ponto para gravar a primeira demonstração.

## 13. Riscos e decisões em aberto

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| A Play Store recusar `QUERY_ALL_PACKAGES` | Sem publicação na loja | Declarar como app de segurança; distribuir por GitHub enquanto isso. O usuário precisará instalar por APK, o que o próprio app ensina a desconfiar: explicar isso no README |
| O SDD ainda não ter skills de Android | Specs geradas com padrões de Spring ou .NET | SPEC-000 antes de qualquer outra |
| Falsos positivos em apps legítimos instalados por APK | Usuário perde a confiança | Teto amarelo sem sinal de NFC, opção "confiável", pesos calibráveis |
| Falsos negativos: golpe que não declara HCE nem pede acessibilidade | App malicioso fica amarelo | Texto do amarelo já orienta atenção; revisar pesos com amostras públicas de relatórios de segurança |
| API de pagamento padrão mudar entre versões do Android | S7 e RF-303 falham em versões novas | API do Android 15+ confirmada nas fontes do AOSP (ADR-021, SPEC-016): `isDefaultServiceForCategory` responde pelo papel de carteira. Consulta isolada numa etapa do inventário; degrada sem erro (menos a falta da permissão `NFC`); testar em Android 8, 12, 14 e na versão mais recente na SPEC-022 |
| Fabricantes matarem tarefas em segundo plano | Vigia atrasa | Varredura também ao abrir o app; orientar sobre economia de bateria nas configurações |
| O teste do cartão ser visto como ferramenta de leitura de cartão | Rejeição na loja ou mau uso | Mostrar só os quatro últimos dígitos, não exportar nada, não guardar nada |

**Decisões em aberto**

- Nome de exibição do app em português e identificador do pacote.
- Licença do código.
- Lista inicial de bancos e carteiras: quantos entram na versão 1.0.
- Publicar na Play Store já na 1.0 ou só depois de validar com usuários.

## 14. Evolução futura

O SDK para bancos reaproveita `core-domain` e `core-detection` sem mudança de lógica, por isso esses módulos não podem depender do `app`.

- **SDK para bancos:** publicar os dois módulos como biblioteca. O app do banco consulta o risco do aparelho antes de aprovar uma transação e envia o resultado ao próprio antifraude.
- **Como testar sem banco real:** um app "banco de demonstração" no mesmo repositório integra o SDK, do mesmo jeito que a isca serve de alvo.
- **Alerta em tempo real:** avisar quando um app de risco está em primeiro plano, usando acesso a uso de apps, com consentimento explícito.
- **Lista de ameaças atualizável:** arquivo assinado com pacotes e nomes de golpes conhecidos, baixado sob demanda. Exige rever o RNF-01.
- **Outros idiomas:** espanhol primeiro, pelo perfil parecido do golpe na América Latina.
