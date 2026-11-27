# Architecture: SPEC-019 — Apps de clonagem no S1

Revisão em 2026-11-26, como architect. **Aprovada sem alteração de comportamento.** O usuário autorizou continuar o fluxo quando a revisão preservasse o comportamento; não há nova aprovação pendente.

## Contrato

- Nomes confirmados: `KnownCloneApps.packageNames` e `InstallSourceCollector(trustedInstallers: Set<String>, knownBankPackages: Set<String>, knownCloneApps: Set<String>)`.
- Todos obrigatórios, sem padrão; três cópias independentes com `Collections.unmodifiableSet(LinkedHashSet(...))`. Sem overload antigo.
- Predicado: instalador em lojas OU (pacote em bancos E instalador em clonadores). Comparação exata; nulo gera S1.
- Lista pública em arquivo próprio; fontes por entrada; sete pacotes aprovados. Sem incluir clonadores em TrustedStores.
- SPEC-018 compõe no Hilt do app: TrustedStores.packageNames; KnownBanks.apps.map { it.packageName }.toSet(); KnownCloneApps.packageNames. O coletor não conhece Hilt nem consulta listas implicitamente.
- Nenhuma mudança de score, domínio, inventário, dependência, permissão ou banco.

## Padrões analisados

TrustedStores, KnownWallets e KnownBanks fixam a forma das listas. InstallSourceCollector, DefaultPaymentAppCollector e BankLookalikeCollector fixam a cópia de conjuntos e o retorno de sinais. KnownWalletsTest, InstallSourceCollectorTrustedStoreTest e InstallSourceCollectorUntrustedInstallerTest fixam JUnit 4 e tabelas esperadas independentes.

## Arquivos e estimativa

| Arquivo/grupo | Linhas adicionadas/alteradas estimadas |
| --- | ---: |
| KnownCloneApps.kt | 45 |
| InstallSourceCollector.kt e KDocs de KnownBanks/TrustedStores | 37 |
| ExpectedKnownCloneApps.kt e KnownCloneAppsTest.kt | 42 |
| InstallSourceCollectorCloneAppTest.kt | 42 |
| InstallSourceCollectorRejectedCloneTest.kt | 42 |
| InstallSourceCollectorConfigurationTest.kt | 75 |
| InstallSourceCollectorCopiedSetsTest.kt | 70 |
| Adaptação das três classes atuais | 27 |
| Subtotal | 380 |
| Margem para testes | 70 |
| Total mantido em feature.estimate-lines | **450** |

Contar linhas adicionadas e removidas no diff de código/testes para a conferência final conservadora. Não contam documentação Markdown nem artefatos SDD. A distribuição refinada substitui a divisão aproximada de 70 + 290 + 90, preservando o total aprovado. Não há necessidade de batches agora. Se ultrapassar 500, dividir obrigatoriamente em BATCH-01 (lista e testes dela) e BATCH-02 (coletor e testes dele); não usar single-pr-reason.

## Organização dos testes

- KnownCloneAppsTest: conteúdo exato e cast recusado com conteúdo preservado. ExpectedKnownCloneApps: sete literais independentes.
- InstallSourceCollectorCloneAppTest: parâmetro instalador, sete linhas escritas à mão; dois testes por linha (banco dispensado e app comum com S1).
- InstallSourceCollectorRejectedCloneTest: parâmetro instalador nullable; banco com instalador nulo, desconhecido, shell, instaladores do sistema, ShareMe, SHAREit, Lite e MIUI Backup recebe S1.
- InstallSourceCollectorConfigurationTest: tabela de configurações com cinco parâmetros (três conjuntos, InstalledApp, resultado); casos de caixa isolada, rótulo, vazios, loja prevalecendo e configuração própria ampliada/reduzida.
- InstallSourceCollectorCopiedSetsTest: parâmetro que identifica qual dos três conjuntos foi alterado; remoção de entrada aceita e adição de entrada recusada após construir, mantendo as outras condições verdadeiras.
- Três classes antigas: manter todos os testes e atualizar seis chamadas; teste de lojas cobre banco e não banco. Caso antigo Smart Switch continua rejeitando app fora dos bancos.

JUnit 4, runTest e anInstalledApp; sem mocks/Robolectric. O construtor e comentários/fontes são revisados por inspeção. Mapeamento real e Hilt são testes da SPEC-018; plataforma em aparelho na SPEC-022. Nenhum teste novo do score.

## Execução e commits

1. Artefatos aprovados + arquitetura/contrato.
2. Lista pública e testes dela (builder escreve produção; tester prepara o test-plan e escreve testes antes do commit).
3. Coletor, atualização de chamadas e testes do coletor, mantendo o commit compilável.
4. Documentação e resultados da conferência/revisão.

A ordem segue as tarefas do plano; os papéis são executados sequencialmente na mesma sessão. Checks por tarefa, build final, mutações locais revertidas em finally sem commit, validação SDD e revisão. Sem push ou PR remoto nesta sessão.

## Conferência final — 2026-11-27

Diff de código e testes: 333 linhas adicionadas e 15 removidas, total conservador de 348. A estimativa 450 inclui margem e foi respeitada; sem batches. A implementação manteve todos os nomes e decisões de comportamento. Não há mudança fora dos arquivos do contrato e dos artefatos SDD próprios.
