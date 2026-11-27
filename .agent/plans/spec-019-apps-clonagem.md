# Plan: SPEC-019 — Apps de clonagem no coletor de S1

> Planejado em 2026-11-26 · [Issue #73](https://github.com/Valossa515/tap-guard/issues/73) · RF-102 · Etapa 1.
> A #73 foi movida com `gh` de “Pronta” para **“Em andamento”** no [Project 2](https://github.com/users/Valossa515/projects/2), antes da elaboração deste plano; status confirmado por nova consulta.
> FT-019 v1.0, `interface: library`, **`feature.estimate-lines: 450`**, um PR. Dependências: SPEC-012 (#8) e SPEC-015 (#11), ambas fechadas. Deve fechar antes da SPEC-018 (#14).
> Entrega deste planejamento: plano, feature spec e critérios de aceite. Não inclui implementação, alteração dos ADRs vigentes nem execução de testes do aplicativo; essas mudanças estão descritas como tarefas abaixo.

## Resumo e referências

Adicionar a lista pública de sete apps de clonagem aprovada na SPEC-017 e estender o `InstallSourceCollector` para não gerar S1 quando o pacote do app pertence ao conjunto de bancos **e**, simultaneamente, seu instalador pertence ao conjunto de apps de clonagem. A dispensa atual por loja confiável continua independente dessa exceção. Um banco instalado à mão e um app qualquer transferido por um clonador continuam recebendo S1.

A forma **(B), no coletor de S1**, já está aprovada. As fontes de requisitos são a [#73](https://github.com/Valossa515/tap-guard/issues/73), o [segundo comentário da #13](https://github.com/Valossa515/tap-guard/issues/13#issuecomment-7069937199), a seção “Regra dos apps de clonagem” de `.agent/plans/spec-017-motor-score.md` e os ADR-017, ADR-020 e ADR-022 em `.agent/decisions.md`. O plano segue `.agent/SKILLS.md`, o papel `agents/planner.md`, as skills de análise de lacunas, anti-invenção, padrões, validação de specs e DoR, e as convenções Android e de testes do projeto.

## Análise de lacunas

| Ponto conferido | Resultado | Tratamento |
| --- | --- | --- |
| Forma da regra e lista inicial | Definidas pelo usuário e pela #73 | Preservar (B) e os sete pacotes aprovados |
| Nome antigo `KnownBanks.entries` no comentário | Resolvido pelo código e ADR-020 | Usar `KnownBanks.apps`; cada entrada é `KnownBankApp` |
| “Dois parâmetros” no comparativo da SPEC-017 | São dois parâmetros **novos**, além das lojas | Construtor com três conjuntos obrigatórios, sem padrões, todos copiados |
| Quem monta os pacotes de banco | Definido neste pedido | SPEC-018, na composição Hilt; não a cada app ou a cada coleta |
| Nome da lista nova e organização final dos testes | Escolha técnica delegada pelo usuário ao architect | Proposta `KnownCloneApps.packageNames`, seguindo as listas existentes; nomes finais no contrato |
| Nulo, conjuntos vazios e diferenças de caixa | Derivados da regra aprovada e da comparação exata já existente | Comportamentos explicitados abaixo; sem validação ou normalização nova |
| Instalador efetivamente atribuído por cada clonador | Validação de plataforma já destinada à SPEC-022 | Não afirmar que foi comprovado em aparelho; não bloqueia a regra sobre `InstalledApp` |

**Nenhuma lacuna bloqueante.** Não foi necessária uma regra de negócio adicional. A nomenclatura proposta e o uso das convenções existentes ficam registrados em `assumptions` na feature spec. Se a implementação encontrar uma ambiguidade de negócio não coberta aqui, deve perguntar antes de alterar o comportamento.

## Escopo e padrões existentes

Produção e testes ficam em `core-detection`, pacote `io.github.valossa515.tapguard.core.detection.signal`. O padrão da lista vem de três exemplos existentes: `TrustedStores`, `KnownWallets` e `KnownBanks`: `object` público, arquivo próprio, conjunto não modificável e comentários de fontes. Os padrões de testes vêm de `KnownWalletsTest`, `KnownBanksTest` e das três classes de testes de `InstallSourceCollector`.

Arquivos previstos, sujeitos à organização final do architect:

| Ação | Arquivos / alcance |
| --- | --- |
| Criar | `core-detection/src/main/kotlin/.../signal/KnownCloneApps.kt` |
| Alterar | `InstallSourceCollector.kt`: construtor, cópias, predicado e KDoc |
| Atualizar comentários | `KnownBanks.kt`: uso dos pacotes pelo S1; `TrustedStores.kt`: clonadores continuam fora das lojas, com remissão à exceção estreita |
| Criar testes | Lista esperada escrita à mão (`ExpectedKnownCloneApps.kt`), teste da lista e testes do coletor para clonagem e condições de contorno, em `core-detection/src/test/kotlin/.../signal/` |
| Adaptar testes | `InstallSourceCollectorTest.kt`, `InstallSourceCollectorTrustedStoreTest.kt` e `InstallSourceCollectorUntrustedInstallerTest.kt`: seis chamadas atuais do construtor |
| Atualizar documentação na implementação | `core-detection/README.md`, `docs/especificacao.md`, `.agent/decisions.md`, `.agent/SKILLS.md`, `.agent/project-reference.md`, `.agent/context.md`, `.agent/architecture.md`, `.agent/glossary.md` e `.agent/backlog_rules.md`, nos trechos relacionados a S1 e às listas |

Sem mudança no motor, em `ScoringContext`, nos modelos do domínio, no inventário, nas dependências, nas permissões, na interface ou na persistência. A ligação Hilt e seu teste pertencem à SPEC-018. A SPEC-019 não implementa as defesas das issues #57 e #65 nem antecipa a conferência em aparelho da SPEC-022.

## Lista pública de apps de clonagem

Proposta: **`KnownCloneApps.packageNames: Set<String>`**, `object` público num arquivo próprio, no mesmo pacote das listas da SPEC-015. O conjunto deve rejeitar modificação inclusive após cast para `MutableSet`, usando o padrão existente `Collections.unmodifiableSet(linkedSetOf(...))`, sem expor o conjunto mutável de suporte. `val` ou `Set` sozinhos não satisfazem o requisito. Não usar `Set.copyOf`, que não atende ao minSdk 26.

Cada entrada terá comentário com nome do app e fonte. A KDoc explicará o uso condicionado ao pacote de banco, as exclusões e a possibilidade de o consumidor do SDK montar outro conjunto com `+` ou `-`, sem modificar o original. Não juntar esses pacotes a `TrustedStores`.

Os pacotes e as fontes abaixo são os **já aprovados e registrados no plano da SPEC-017**, consultados naquele planejamento em 2026-11-25. Não representam uma nova verificação de publicação ou comportamento em aparelho realizada nesta tarefa. A fonte comum registrada é [Talsec — Installation Sources Cookbook, “Cloners & Movers”](https://docs.talsec.app/appsec-articles/glossary/sideloading/installation-sources-cookbook).

| App | Pacote exato | Fonte por entrada a preservar |
| --- | --- | --- |
| Samsung Smart Switch | `com.sec.android.easyMover` | Talsec; [Google Play — Samsung Smart Switch Mobile](https://play.google.com/store/apps/details?id=com.sec.android.easyMover); ADR-017 |
| Xiaomi Mi Mover | `com.miui.huanji` | Talsec; [Google Play — Mi Mover](https://play.google.com/store/apps/details?id=com.miui.huanji); ADR-017 |
| OPPO Clone Phone | `com.coloros.backuprestore` | Talsec; [Google Play — OPPO Clone Phone](https://play.google.com/store/apps/details?id=com.coloros.backuprestore); ADR-017 |
| OnePlus Clone Phone | `com.oneplus.backuprestore` | Talsec; [Google Play — Clone Phone - OnePlus app](https://play.google.com/store/apps/details?id=com.oneplus.backuprestore) |
| Huawei Phone Clone | `com.hicloud.android.clone` | Talsec; [APKMirror — Huawei Phone Clone](https://apkmirror.com/apk/huawei-internet-services/phone-clone/), referência registrada na SPEC-017; não citar a Play como confirmação, pois retornou 404 naquele levantamento |
| HONOR Device Clone | `com.hihonor.android.clone` | Talsec; [Google Play — Device Clone](https://play.google.com/store/apps/details?id=com.hihonor.android.clone) |
| vivo EasyShare | `com.vivo.easyshare` | Talsec; [Google Play — EasyShare](https://play.google.com/store/apps/details?id=com.vivo.easyshare) |

Continuam excluídos **ShareMe** (`com.xiaomi.midrop`), **SHAREit** (`com.lenovo.anyshare.gps`) e **SHAREit Lite** (`shareit.lite`): a decisão aprovada distingue compartilhamento de arquivos entre pessoas da migração do próprio celular. **MIUI Backup** (`com.miui.backup`) também fica fora, conforme a decisão de escopo registrada na SPEC-017. Nenhum pacote adicional será inferido por fabricante ou sem fonte.

## Contrato esperado do coletor e integração futura

O construtor passa a receber, nesta ordem proposta, **três `Set<String>` não nulos, obrigatórios e sem valor padrão**:

| Parâmetro proposto | Conteúdo | Configuração do app na SPEC-018 |
| --- | --- | --- |
| `trustedInstallers` | Pacotes de lojas confiáveis | `TrustedStores.packageNames` |
| `knownBankPackages` | Pacotes dos apps de banco | `KnownBanks.apps.map { it.packageName }.toSet()` |
| `knownCloneApps` | Pacotes de instaladores de clonagem | `KnownCloneApps.packageNames` |

Cada conjunto deve ser copiado separadamente na construção, pelo padrão já usado no coletor: `Collections.unmodifiableSet(LinkedHashSet(conjuntoRecebido))`. Alterar qualquer conjunto original depois da construção não pode mudar os resultados. O `toSet()` no mapeamento dos bancos serve para deduplicar os pacotes; **não substitui a cópia defensiva do construtor**.

O mapeamento usa **todas** as entradas de `KnownBanks.apps`, incluindo variantes oficiais; os aliases convergem para um só pacote. A lista atual tem 29 entradas e 26 pacotes distintos. Não usar `KnownWallets`, rótulos, comparação de nomes do S9 nem uma segunda lista manual de bancos na produção.

A **SPEC-018 monta esses valores no módulo/provedor Hilt do `app` que fornecerá o coletor para a varredura**. O coletor apenas recebe os conjuntos; não consulta listas globais por conta própria e não ganha anotação Hilt nesta spec. `ScanDevice` usa os coletores já compostos. Nome, escopo Hilt e organização dos providers serão definidos na SPEC-018, sem introduzir agora três bindings genéricos indistinguíveis de `Set<String>`.

Este é um breaking change do construtor público: chamadas com só o conjunto de lojas deixam de compilar. Atualizar todas as chamadas existentes, sem overload de compatibilidade ou valores padrão que ocultem a configuração. O SDK ainda não foi publicado, conforme o contexto do projeto.

## Regra e condições de contorno

O `collect(app)` continua devolvendo somente conjunto vazio ou `{UNTRUSTED_INSTALL_SOURCE}`. Lê `app.packageName` e `app.installerPackageName`; não chama `PackageManager`.

**Sem S1 se, e somente se:**

`instalador ∈ lojas OU (pacote ∈ bancos E instalador ∈ clonadores)`.

As duas condições da exceção de clonagem permanecem sempre juntas. Todas as comparações são exatas e sensíveis a maiúsculas e minúsculas, inclusive o `M` de `com.sec.android.easyMover`. Não normalizar os nomes de pacote.

| Instalador nas lojas? | Pacote nos bancos? | Instalador nos clonadores? | Resultado |
| --- | --- | --- | --- |
| Sim | Qualquer | Qualquer | Vazio |
| Não | Sim | Sim | Vazio |
| Não | Não | Sim | S1 |
| Não | Sim | Não | S1 |
| Não | Não | Não | S1 |
| Nulo | Qualquer | Não pertence a nenhum conjunto | S1 |

Conjuntos vazios são aceitos: bancos vazios ou clonadores vazios desligam somente a exceção de clonagem; lojas continuam dispensando S1. Lojas vazias não desligam a exceção quando banco e clonador coincidem. Os três conjuntos vazios geram S1 para todo app. O mesmo predicado resolve eventual sobreposição em configurações próprias do SDK: se o instalador estiver em lojas, a primeira condição basta.

Sem S1, o motor existente já aplica o teto 29 quando não há S7 nem S9. Havendo S7 ou S9, a regra 3 não limita a 29, mas a regra 4 mantém o teto 59 por faltar S1. Portanto, a exceção **não promete verde incondicional** e não exige nova regra de score.

## Estratégia de testes

JUnit 4 na JVM, `runTest` e fixture `anInstalledApp`, sem Robolectric, mocks ou matriz de APIs: o coletor não usa APIs do Android. Casos parametrizados usam entradas esperadas escritas à mão, nunca a lista de produção como geradora dos casos. O coletor sob teste pode receber a lista de produção para que a ausência de um pacote seja detectada; a expectativa é independente.

| Grupo | Casos e resultado esperado |
| --- | --- |
| Lista pública | Igualdade com os sete pacotes escritos à mão; nenhum pacote extra; tentativa de alteração por cast lança `UnsupportedOperationException` e preserva o conteúdo |
| Banco copiado | Para **cada um dos sete clonadores**, pacote de banco presente no conjunto → vazio |
| App qualquer copiado | Para **cada um dos sete clonadores**, pacote fora do conjunto de bancos → exatamente S1 |
| Banco instalado à mão | Pacote presente nos bancos, com instalador nulo, desconhecido, shell ou instalador de pacotes do sistema → exatamente S1 |
| Compartilhamento e backup excluídos | Banco presente no conjunto, com `com.xiaomi.midrop`, `com.lenovo.anyshare.gps`, `shareit.lite` ou `com.miui.backup` como instalador → exatamente S1 |
| Conjunção e igualdade exata | Banco conhecido com clonador desconhecido → S1; clonador conhecido com banco desconhecido → S1; variar somente a caixa do pacote do banco ou do clonador → S1; rótulo de banco sem pacote correspondente não dá exceção |
| Conjuntos vazios | Bancos vazios e clonadores vazios testados separadamente impedem a exceção; loja continua dispensando S1 em ambos; lojas vazias ainda permitem banco + clonador; todos vazios → S1 |
| Cópias defensivas | Três cenários independentes, um para cada conjunto; após construir, remover entrada usada e acrescentar entrada antes ausente não muda nem o caso aceito nem o caso rejeitado |
| Configuração do consumidor | Usar banco e clonador fictícios explicitamente passados, exercitar ampliação/redução; a regra segue os conjuntos recebidos e não listas embutidas consultadas internamente |
| Regressão | Manter todos os testes atuais: onze lojas aceitas, instaladores recusados, nulo, caixa, retorno apenas de S1, extensão/redução da lista de lojas e sua cópia |

Nas seis chamadas atuais, passar explicitamente os novos argumentos. Os testes antigos voltados somente às lojas podem usar conjuntos novos vazios. O caso antigo de Smart Switch deve continuar com pacote **fora** dos bancos; os novos casos de clonagem devem usar o conjunto de clonadores preenchido para provar que a condição do banco é indispensável. Não remover o caso antigo nem trocar sua expectativa para vazio.

O teste de integração que comprovará o mapeamento real de `KnownBanks.apps` e a montagem Hilt fica na **SPEC-018**, junto ao código desse provedor. Os testes desta spec usam um banco fictício para isolar o contrato do coletor. Não duplicar os testes do motor da SPEC-017.

## Atualizações de documentação planejadas

**Atualizar o ADR-017, sem criar um ADR novo.** Registrar uma emenda identificada como SPEC-019, preservando o contexto histórico da SPEC-012. A emenda substitui a definição vigente de S1, descreve os três parâmetros e suas cópias, registra a lista e suas fontes e corrige a antiga afirmação de que lojas vazias fazem todos receber S1. “Apps de clonagem fora das lojas” continua verdadeiro; a exceção estreita é separada. Trocar a pendência “SPEC-017 precisa definir como entra no score” pela decisão (B), com referência à #73 e ao ADR-022.

No **ADR-020**, corrigir a remissão antiga à SPEC-017 no parágrafo sobre clonagem: a fonte é `KnownBanks.apps`, os pacotes são derivados pela SPEC-018 no Hilt e a regra está no S1 da SPEC-019. A lista de bancos e o comportamento de S9 não mudam.

Na **§5 de `docs/especificacao.md`**, planejar estas alterações textuais concretas:

- Definição da linha S1, mantendo peso 30: **“Instalado fora de loja confiável, salvo quando o pacote está na lista de bancos conhecidos e o instalador está na lista de apps de clonagem conhecidos.”** Acrescentar que instalador nulo gera S1.
- Em “Listas de apoio”, acrescentar a lista dos sete clonadores com os pacotes da tabela deste plano e referência ao ADR-017; explicitar as exclusões de ShareMe, SHAREit, SHAREit Lite e MIUI Backup.
- Explicitar que `KnownBanks.apps`, além de servir ao S9, fornece os pacotes para a exceção de S1. Clonadores continuam fora de `TrustedStores`.
- Junto à regra 3, acrescentar a explicação: **“O motor aplica esta regra pela ausência de S1; isso inclui o banco conhecido copiado por clonador conhecido. As exceções S7/S9 e a exigência de S1 para vermelho continuam valendo.”** Preservar as quatro regras, os pesos e os cinco exemplos obrigatórios.

Sincronizar README do módulo e documentos de contexto relacionados a S1, inclusive a tabela de coleta na §8 se ela ainda sugerir comparação somente com lojas. A KDoc do coletor deve cobrir os novos parâmetros, os vazios e as limitações. Não reescrever os artefatos históricos da SPEC-012/015/017 como se já tivessem entregue esta mudança.

## Limitações preservadas

A análise da brecha é a aprovada na SPEC-017: comparar apenas nomes não autentica apps. Um pacote de banco emprestado, já presente no aparelho antigo e transferido por clonador conhecido, pode usufruir da exceção. Um instalador com pacote emprestado de clonador ausente também pode enganar a comparação. Isso não cria capacidade adicional frente à possibilidade já registrada de emprestar o pacote de uma loja ausente, que dispensa S1 para qualquer app; **não significa ausência de risco residual**.

As defesas de instalador app do sistema e de certificado ficam nas #57 e #65. Não ampliar a regra para “qualquer banco” nem adicionar clonadores a lojas: banco instalado à mão deve conservar S1. A SPEC-022 verificará qual `installingPackageName` cada ferramenta atribui e eventuais reinstalações pela Play; este plano não inventa esse resultado.

## Estimativa e tarefas

| Parte | Linhas estimadas adicionadas/alteradas |
| --- | ---: |
| Lista, coletor e KDocs relacionados | 82 |
| Testes da lista, tabela dos sete clonadores, negativos, cópias dos três conjuntos e adaptação dos testes existentes | 298 |
| Margem exclusiva para testes (~23% sobre 298) | 70 |
| **`feature.estimate-lines`** | **450** |

Estimativa de código e testes, excluindo documentação externa ao código e os artefatos SDD da própria feature. É superior à projeção inicial de ~260 da SPEC-017 porque inclui explicitamente os três conjuntos, mutações de entrada, vazios independentes, exclusões e regressões. Um PR, sem batches e sem `single-pr-reason`, abaixo do limite de 500. Se o contrato revelar aumento acima do limite, reestimar e aplicar as regras de decomposição antes de implementar.

Branch proposta para a implementação: `spec-019-apps-clonagem`; PR com SPEC-019 no título e `Closes #73`. Nenhuma branch ou PR foi criado neste planejamento.

1. [x] Architect revisar os artefatos, fixar nomes e organização dos testes e produzir o contrato; não reabrir a decisão (B).
2. [x] Entregar lista pública com fontes e seus testes de conteúdo e imutabilidade.
3. [x] Alterar o coletor, adaptar as seis chamadas e entregar os testes da exceção, negativos, vazios e cópias no mesmo passo, mantendo a suíte compilável.
4. [x] Atualizar ADR-017, remissão do ADR-020, §5, KDocs, README e documentos de contexto; registrar o contrato de integração para a SPEC-018.
5. [x] Executar a validação SDD e os checks de implementação; revisar o diff e o orçamento antes do PR.

## Aceite e validação

Critérios completos em `.agent/specs/acceptance/spec-019-apps-clonagem.acceptance.toon`; feature em `.agent/specs/features/spec-019-apps-clonagem.spec.toon`.

- [x] Sete pacotes exatos em lista pública não modificável, com fonte por entrada e teste independente.
- [x] Três conjuntos obrigatórios, sem padrão, copiados individualmente; consumidor controla a configuração.
- [x] Loja dispensa S1; exceção de clonagem exige simultaneamente banco e clonador.
- [x] Banco copiado não recebe S1; app qualquer copiado e banco instalado à mão recebem S1.
- [x] ShareMe e SHAREit geram S1 inclusive para banco; SHAREit Lite e MIUI Backup permanecem excluídos.
- [x] Casos de nulo, caixa, conjuntos vazios, cópias e regressões cobertos sem mudar o motor.
- [x] ADR-017, ADR-020 e §5 coerentes com a regra; Hilt explicitamente destinado à SPEC-018.
- [x] ktlint, detekt e testes passam; nenhuma dependência ou permissão nova.

Na implementação: `./gradlew :core-detection:build` como verificação do módulo e `./gradlew build` como gate do PR (no Windows, equivalente `./gradlew.bat`). Registrar os testes que só rodarem no CI, conforme a convenção do projeto. A mudança não cria meta Kover em `core-detection`; o gate existente do domínio continua no build completo. Conferência manual em aparelho fica na SPEC-022.

Neste planejamento, validar os dois TOON com `.agent/scripts/validate-spec.sh`, conferir contagens e rastreabilidade de regras/aceites e revisar a consistência com este plano. Não executar Gradle para afirmar uma implementação que ainda não existe. O banco de dados e as migrações não têm alterações.

## Conferência do planejamento — 2026-11-26

- `.agent/scripts/validate-spec.sh` executado sobre a feature e o aceite: **2 arquivos válidos, sem erros**.
- Conferência adicional: colunas das tabelas TOON consistentes; **12 regras cobertas por 18 critérios**; `feature-ref: FT-019` e estimativa 450 coerentes entre os artefatos.
- Confirmadas as seis chamadas atuais do construtor nas três classes de testes; dependências #8 e #11 fechadas; #73 em “Em andamento” no Project 2.
- Nenhum código de produção ou teste alterado. Build e testes do aplicativo não executados nesta etapa de planejamento. Alterações de ADRs e da especificação do produto permanecem tarefas da implementação.

## Execução após aprovação — concluída em 2026-11-27

- Arquitetura aprovada sem mudar o comportamento; nomes e organização em `spec-019-apps-clonagem-architecture.md` e contrato FT-019 v1.0. As observações anteriores descrevem o estado na etapa de planejamento.
- Implementação e testes concluídos na branch `spec-019-apps-clonagem`, sem push. Main e origin/main permanecem em `fc74fb6252e151288efcfa1b14788494a275a249`.
- Diff de código e testes: 333 adições + 15 remoções = 348 linhas; estimativa 450 mantida. Sem batches e sem single-pr-reason.
- 71 testes focados passam, incluindo 44 novos. Build do módulo e build completo passam; core-detection com 406 testes e domínio com 97,5% de cobertura.
- Sete mutações locais detectadas por asserções, restauradas sem commit; resultados e testes ignorados no Windows detalhados no test-plan.
- Emenda SPEC-019 no ADR-017, remissão e limitação no ADR-020, §5 e §8, README e documentos de contexto atualizados.
- Validação final: feature, aceite e contrato válidos no validate-spec.sh; colunas e rastreabilidade conferidas (12 regras e 18 critérios).
