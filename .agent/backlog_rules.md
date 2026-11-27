# Backlog Rules

> Fonte: `docs/especificacao.md`. Regras de negócio e de processo já definidas; todas **(planned)** até serem implementadas.

## Work Items
- Cada spec é uma issue com o ID no título (`SPEC-0xx`), entregável, critérios de aceite em checklist e dependências.
- As etapas são as milestones do repositório (8 etapas, 38 specs, issues #1 a #38).
- Specs referenciam os IDs de requisito (`RF-xxx`, `RNF-xx`), de sinal (`S1`–`S9`) e de tela (`T1`–`T9`).

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

**Ordem:** caminho crítico Etapa 0 → 1 → 3 → 5 → 7. Etapa 2 em paralelo com a 1 a partir da SPEC-001. Etapa 4 só depende do domínio (paralela às 3 e 5). SPEC-060 a qualquer momento depois da SPEC-003. SPEC-000 antes de qualquer outra (skills de Android no SDD).

**Primeiro marco demonstrável:** ao fim da SPEC-032, o app instala, varre o aparelho e mostra a isca em vermelho com a explicação.

## User Story Format
- Não usamos formato de história de usuário. Cada spec é descrita por entregável e checklist de aceite, como nas issues.

## Acceptance Criteria
- Em checklist na issue de cada spec.
- Critérios de aceite por funcionalidade já definidos:
  - **F1:** isca instalada por APK aparece em vermelho com os sinais corretos; carteiras e apps de banco da loja oficial aparecem em verde; varredura de 300 apps em até 5 s em aparelho intermediário.
  - **F2:** leitura em até 3 s com Visa ou Mastercard; nenhuma ocorrência do número completo em banco local, preferências, logcat ou captura de tela.
  - **F3:** instalar a isca com o Tap Guard fechado gera notificação no próximo ciclo; nenhuma notificação repetida para o mesmo app na mesma versão.

## Business Rules

### Score de risco (§5)
- Score = soma dos pesos dos sinais presentes, limitada a 100, depois ajustada.
- Pesos: S1 30, S2 15, S3 20, S4 25, S5 10, S6 10, S7 30, S8 10, S9 20 — em um único arquivo de configuração.
- Os pesos são um ponto de partida e podem ser calibrados. Qualquer mudança exige atualizar os cinco exemplos abaixo e os testes correspondentes na mesma spec.
- Faixas: verde 0–29; amarelo 30–59; vermelho 60+.
- Regras de ajuste, **nesta ordem**:
  1. Apps do sistema são ignorados.
  2. App marcado como confiável pelo usuário fica sempre verde.
  3. App instalado por loja confiável tem score limitado a 29, a não ser que tenha S7 ou S9.
  4. Vermelho exige S1 junto com pelo menos um sinal de NFC (S2, S3 ou S7). Sem isso, o teto é amarelo (59).
- S1 (SPEC-019): Sem S1 quando o instalador está nas lojas OU (pacote está nos bancos E instalador está nos clonadores). Instalador nulo e todos os demais casos geram S1. Comparação exata; três conjuntos obrigatórios e copiados. A SPEC-018 monta os três conjuntos no Hilt do `app`: `TrustedStores.packageNames`, `KnownBanks.apps.map { it.packageName }.toSet()` e `KnownCloneApps.packageNames`. O mapeamento deduplica as 29 entradas em 26 pacotes; a cópia defensiva continua sendo feita pelo coletor. Bancos vazios ou clonadores vazios desligam somente a exceção; lojas vazias ainda permitem banco com clonador; todos vazios geram S1 para qualquer app.
- Combinação (ADR-022): as regras 3 e 4 são tetos e comutam (vence 29); app sem S1 nunca fica vermelho; a regra 2 só troca o nível, e o score do app confiável passa pelas regras 3 e 4.
- Arquivo de configuração: `RiskScoreConfig` (`core-domain`, `internal`), com pesos, tetos e faixas; motor: `WeightedRiskScorer`. A tabela de combinações dos testes (`WeightedRiskScorerTableTest`) também muda quando um peso muda.
- Exemplos obrigatórios como testes unitários:
  - APK de fora da loja com NFC e HCE: 30 + 15 + 20 = 65, vermelho.
  - Mesmo app, instalado ontem e com acessibilidade ativa: 100, vermelho.
  - App de banco da loja oficial com NFC e HCE: 35, limitado a 29, verde.
  - Jogo instalado por APK sem NFC: 30, amarelo.
  - App de fora da loja com acessibilidade e SMS, sem NFC: 65, limitado a 59, amarelo.
- Listas embutidas: lojas confiáveis (Google Play, Galaxy Store, Xiaomi GetApps, OPPO App Market, vivo V-Appstore, HONOR App Market, Palm Store, Huawei AppGallery, Amazon Appstore — `TrustedStores`, SPEC-012, ADR-017); carteiras conhecidas (Carteira do Google, Samsung Wallet, Nubank, Itaú, Banco do Brasil e Inter — `KnownWallets`, 6 pacotes, SPEC-015, ADR-020); bancos conhecidos para S9 (nome de exibição + pacote oficial de 29 apps das 14 instituições com mais clientes no ranking do Banco Central — `KnownBanks`, SPEC-015, ADR-020).

- Clonadores conhecidos: Samsung Smart Switch (`com.sec.android.easyMover`), Xiaomi Mi Mover (`com.miui.huanji`), OPPO Clone Phone (`com.coloros.backuprestore`), OnePlus Clone Phone (`com.oneplus.backuprestore`), Huawei Phone Clone (`com.hicloud.android.clone`), HONOR Device Clone (`com.hihonor.android.clone`) e vivo EasyShare (`com.vivo.easyshare`). Lista pública imutável `KnownCloneApps.packageNames`; separada de `TrustedStores`. ShareMe (`com.xiaomi.midrop`), SHAREit (`com.lenovo.anyshare.gps`), SHAREit Lite (`shareit.lite`) e MIUI Backup (`com.miui.backup`) ficam fora. Fontes no ADR-017.

### Varredura (F1)
- Lista apps que não são do sistema; roda ao abrir o app e por "Verificar agora".
- Resumo: pior nível encontrado e contagem por nível.
- Amarelos e vermelhos do maior score para o menor; verdes em lista recolhida.
- Cada sinal explicado em uma frase simples no detalhe.
- Ações: abrir desinstalação do sistema, abrir informações do app, marcar como confiável. O app nunca desinstala ou bloqueia sozinho.
- Confiável sai dos alertas, vai para lista própria e pode ser desfeito.

### Teste do cartão (F2)
- Explicar e pedir confirmação antes de começar.
- Mostrar bandeira ou nome da aplicação, 4 últimos dígitos e validade. Nunca o número completo.
- Mensagem educativa exata: "Foi assim, encostando, que um app conseguiu ler seu cartão. Nenhum banco pede isso."
- Nenhum dado do cartão em disco, log ou análise de falhas; só em memória, descartado ao sair da tela; tela segura.
- Mensagem própria para: sem NFC, NFC desligado (atalho para configurações), cartão não suportado, leitura interrompida.

### Vigia (F3)
- Varredura periódica (padrão 6 h; opções 1, 6, 12, 24) comparada com a anterior.
- Notifica app novo ou atualizado que ficar amarelo ou vermelho, e troca do app de pagamento padrão.
- Sem notificação repetida para o mesmo app na mesma versão (controle via `alert_event`).
- Tocar na notificação abre o detalhe do app (T4).
- Usuário pode desligar a vigia e mudar o intervalo; histórico local dos alertas.

### Emergência (F4)
- Botão "Acho que caí em um golpe" sempre visível na tela inicial.
- Passos marcáveis: desligar o NFC, desinstalar o app suspeito, bloquear o cartão pelo app oficial ou telefone do verso, contestar transações, registrar boletim de ocorrência.
- Atalhos para as telas do sistema de NFC e de apps instalados. Funciona sem internet.

### Geral
- Onboarding de três telas: o que é o golpe, o que o app faz, pedido de permissão de notificação.
- Configurações: vigia, intervalo, apps confiáveis, política de privacidade, versão.
- T2 sem varredura anterior: "Ainda não verificamos seu aparelho".
- Textos exatos dos níveis: verde "Nada suspeito encontrado."; amarelo "Encontramos apps que merecem atenção."; vermelho "Encontramos um app com perfil de golpe. Veja o que fazer."
- Textos exatos das notificações: app de risco "Um app instalado há pouco tem perfil de golpe. Toque para ver."; pagamento padrão "O app de pagamento por aproximação do seu celular mudou. Foi você?"
- Retenção: 10 varreduras mais recentes pela ordem de gravação (`id`), na transação da gravação (SPEC-018, ADR-023); alertas por 90 dias (SPEC-053).

## Definition of Ready
- Issue com ID da spec no título, entregável, critérios de aceite em checklist e dependências mapeadas.
- Todas as issues listadas em "Depende de" na issue da spec estão fechadas.
- Demais itens: ver [skills/shared/dor/SKILL.md](skills/shared/dor/SKILL.md).

## Definition of Done
- Entrega código, testes e atualização da documentação do módulo.
- Passa em ktlint, detekt e na suíte de testes.
- Uma branch e um PR por spec, com o ID da spec no título. Quando a spec tiver batches (acima de 500 linhas de código e testes), um PR por batch, com o ID da spec e o `BATCH-NN` no título; só o PR do último batch leva `Closes #N`.
- Nada de lógica de score fora do `core-domain`; nada de API do Android dentro do `core-domain`.
- Manifesto final sem `INTERNET`; nenhum dado de cartão persistido ou logado.
- Demais itens: ver [skills/shared/dod/SKILL.md](skills/shared/dod/SKILL.md).
