# Como contribuir

Obrigado pelo interesse no Tap Guard. Este guia explica como o trabalho é organizado e o que um pull request precisa ter para ser aceito.

## Como o trabalho é organizado

- O projeto é desenvolvido por specs. Cada spec é uma issue com o ID no título (`SPEC-0xx`), um entregável, critérios de aceite em checklist e a lista de specs de que depende.
- As etapas são as milestones do repositório. O quadro do projeto mostra o que está em Backlog, Pronta, Em andamento, Em revisão e Concluída.
- Uma spec só pode ser iniciada quando todas as issues listadas em "Depende de" estão fechadas.
- O contexto completo está em [docs/especificacao.md](docs/especificacao.md).

## Antes de começar

1. Escolha uma issue em "Pronta" e comente que vai trabalhar nela.
2. Para uma ideia nova ou um problema que não tem issue, abra uma issue antes de escrever código.
3. Falhas de segurança não vão em issue pública. Veja [SECURITY.md](SECURITY.md).

## Fluxo de trabalho

1. Crie uma branch a partir de `main` com o nome `spec-NNN-descricao-curta`, por exemplo `spec-011-inventario-de-apps`.
2. Implemente a spec inteira: código, testes e documentação do módulo.
3. Abra um pull request com o ID da spec no título e `Closes #N` na descrição.
4. Espere o CI passar e a revisão.

Um pull request por spec. Mudanças que não pertencem à spec vão em outro pull request.

## Regras que não têm exceção

- **Sem internet.** Nenhum manifesto do app pode declarar `android.permission.INTERNET`. O CI falha se ela aparecer.
- **Dados de cartão.** Nunca gravar, logar ou exportar. O número completo não sai do módulo `core-nfc`.
- **Domínio puro.** Nada de API do Android dentro do `core-domain`. Nada de lógica de score fora dele.
- **Permissões e dependências.** Só entram as que a spec nomeia.
- **Testes.** Nunca usar malware real nem dados reais de cartão. O alvo de teste é o `decoy-app`.

## Convenções

- Código e nomes técnicos em inglês. Textos de interface e documentação em português do Brasil.
- Textos de interface em linguagem simples, sem termos técnicos, sempre em arquivo de recursos.
- Mensagens de commit no formato `tipo: descrição`, com os tipos `feat`, `fix`, `docs`, `test`, `refactor` e `chore`.
- Convenções de código: [.agent/conventions.md](.agent/conventions.md) e a skill de Android em `.agent/skills/android/`.

## Definição de pronto

- Todos os critérios de aceite da issue estão marcados.
- ktlint, detekt e a suíte de testes passam.
- A documentação do módulo foi atualizada.
- O manifesto final não declara `INTERNET` e nenhum dado de cartão é persistido ou logado.

## Desenvolvimento com agentes de IA

O repositório usa o [SDD](https://github.com/valossa515/sdd). O contexto do projeto fica em `.agent/`. Quem usa um agente deve começar cada tarefa pedindo que ele leia `.agent/SKILLS.md` e siga as skills listadas. Código gerado por agente passa pelas mesmas regras e pela mesma revisão que qualquer outro.

## Ambiente

Os comandos de build e de teste serão documentados aqui quando a SPEC-001 criar o projeto Gradle.
