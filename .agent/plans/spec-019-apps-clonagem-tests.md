# Test Plan: SPEC-019

JUnit 4 JVM, runTest, anInstalledApp; sem mocks ou Robolectric. Segue o contrato FT-019 v1.0. Plano registrado antes de escrever os testes.

| Classe | Cenários | Aceite |
| --- | --- | --- |
| KnownCloneAppsTest + ExpectedKnownCloneApps | Conteúdo com sete literais; cast recusado e conteúdo preservado | AC-001–003 |
| InstallSourceCollectorCloneAppTest | Sete instaladores; banco sem S1 e não banco com S1 | AC-007–008 |
| InstallSourceCollectorRejectedCloneTest | Banco com nulo, desconhecido, shell, três instaladores do sistema e quatro apps excluídos | AC-009–010 |
| InstallSourceCollectorConfigurationTest | Caixa do banco/clonador, rótulo, vazios independentes, lojas, conjunto próprio ampliado e reduzido | AC-006, AC-011–013 |
| InstallSourceCollectorCopiedSetsTest | Adição e remoção após construir em cada um dos três conjuntos | AC-005 |
| Três classes antigas do S1 | Todos os casos preservados; seis chamadas adaptadas; lojas com banco e não banco | AC-004, AC-006, AC-015–016 |

Construtor obrigatório, fontes, KDocs, limites de módulo e documentação verificados por inspeção (AC-001, AC-004, AC-014–015, AC-017). AC-018: build do módulo e completo com lint/testes; SDD validado.

Dados: banco fictício com.example.bank e app comum com.example.other; os testes de configuração/cópia usam loja e clonador fictícios. A lista esperada não deriva da produção. Hilt e o mapeamento real de KnownBanks.apps ficam na SPEC-018, plataforma na SPEC-022 e motor sem novos testes.

## Conferência local planejada

Depois de uma execução verde, alterar individualmente a produção: E por OU; retirar cada cópia dos conjuntos; comparar bancos e clonadores sem distinguir caixa; incluir clonadores em TrustedStores. Cada alteração deve derrubar testes. Restaurar bytes originais em finally, sem commit de mutantes, e executar a suíte final restaurada.

## Resultados

Lista: 2 testes passaram; ktlint e detekt do módulo passaram. Suíte focada completa: 71 testes passaram (44 novos e 27 existentes), sem falhas ou ignorados; ktlint e detekt passaram. Conferência por mutação e build completo concluídos; detalhes abaixo.

## Execução concluída — 2026-11-27

- Testes focados: 71, todos passaram, sendo 44 novos e 27 preservados. JUnit 4 na JVM; sem ignorados na SPEC-019.
- Build final restaurado: `./gradlew.bat :core-detection:build build --console=plain` passou em 2min29s, com 535 tarefas (91 executadas, 444 atualizadas). Inclui ktlint, detekt, Android lint, testes e Kover.
- Relatórios locais: core-domain 75; core-detection 406; core-data 17 (2 ignorados no Windows); core-nfc 1; app 32; decoy-app 1. Zero falhas. São 532 casos, 530 aprovados e 2 ignorados.
- Os dois casos existentes de SettingsDataSourceTest que rodam no CI são `written values are read back` e `writing null removes last default payment component`. Nenhum teste foi desabilitado por esta spec.
- Cobertura de linhas do domínio: 117 cobertas e 3 não cobertas = 97,5%; koverVerify passou. Não há meta Kover em core-detection.
- Não executado em aparelho, não executado Hilt da SPEC-018, não enviado ao CI remoto (sem push).

### Mutações locais, todas desfeitas sem commit

| Mutação isolada | Casos que falharam | Evidência |
| --- | ---: | --- |
| Trocar E por OU na exceção | 27 | Apps comuns nos sete clonadores, bancos instalados à mão, configurações e cópias |
| Retirar a cópia das lojas | 2 | Novo caso stores e regressão antiga da cópia |
| Retirar a cópia dos bancos | 1 | Caso banks |
| Retirar a cópia dos clonadores | 1 | Caso cloners |
| Ignorar caixa do pacote do banco | 1 | configuration 1 |
| Ignorar caixa do instalador de clonagem | 1 | configuration 2 |
| Acrescentar os sete clonadores a TrustedStores | 9 | Sete apps comuns copiados, conteúdo de TrustedStores e antigo Smart Switch |

Cada execução saiu com código 1 por falha de asserção; nenhuma foi considerada detectada apenas por erro de compilação. Os bytes originais dos dois arquivos foram restaurados em finally; diff de produção vazio em relação ao commit ee28ca2 antes do build final verde. Não se adicionou script de mutação ao repositório.

A primeira rodada de detekt apontou MaxLineLength em InstallSourceCollectorRejectedCloneTest; a chamada foi quebrada em linhas e o check seguinte passou. O build exibe o aviso geral de APIs Gradle obsoletas já observado na primeira execução; nenhum aviso novo de ktlint/detekt permaneceu.
