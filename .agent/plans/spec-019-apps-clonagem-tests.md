# Test Plan: SPEC-019

JUnit 4 JVM, runTest, anInstalledApp; sem mocks ou Robolectric. Segue o contrato FT-019 v1.0. Plano registrado antes de escrever os testes.

| Classe | Cenários | Aceite |
| --- | --- | --- |
| KnownCloneAppsTest + ExpectedKnownCloneApps | Conteúdo com sete literais; cast recusado e conteúdo preservado | AC-001–003 |
| InstallSourceCollectorCloneAppTest | Sete instaladores; banco vazio e não banco S1 | AC-007–008 |
| InstallSourceCollectorRejectedCloneTest | Banco com nulo, desconhecido, shell, três instaladores do sistema e quatro apps excluídos | AC-009–010 |
| InstallSourceCollectorConfigurationTest | Caixa do banco/clonador, rótulo, vazios independentes, lojas, conjunto próprio ampliado e reduzido | AC-006, AC-011–013 |
| InstallSourceCollectorCopiedSetsTest | Adição e remoção após construir em cada um dos três conjuntos | AC-005 |
| Três classes antigas do S1 | Todos os casos preservados; seis chamadas adaptadas; lojas com banco e não banco | AC-004, AC-006, AC-015–016 |

Construtor obrigatório, fontes, KDocs, limites de módulo e documentação verificados por inspeção (AC-001, AC-004, AC-014–015, AC-017). AC-018: build do módulo e completo com lint/testes; SDD validado.

Dados: banco fictício com.example.bank e app comum com.example.other; os testes de configuração/cópia usam loja e clonador fictícios. A lista esperada não deriva da produção. Hilt e o mapeamento real de KnownBanks.apps ficam na SPEC-018, plataforma na SPEC-022 e motor sem novos testes.

## Conferência local planejada

Depois de uma execução verde, alterar individualmente a produção: E por OU; retirar cada cópia dos conjuntos; comparar bancos e clonadores sem distinguir caixa; incluir clonadores em TrustedStores. Cada alteração deve derrubar testes. Restaurar bytes originais em finally, sem commit de mutantes, e executar a suíte final restaurada.

## Resultados

Lista: 2 testes passaram; ktlint e detekt do módulo passaram. Demais grupos pendentes.
