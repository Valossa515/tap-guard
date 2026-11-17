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

O repositório usa o [SDD](https://github.com/valossa515/sdd) com a stack `android`. Usar um agente é opcional. Código gerado por agente passa pelas mesmas regras, pelo mesmo CI e pela mesma revisão que qualquer outro, e quem abre o pull request responde pelo conteúdo.

### O que já está no repositório

| Caminho | Conteúdo |
| --- | --- |
| `.agent/SKILLS.md` | Ponto de entrada. Lista as skills ativas e, em "Project Overrides", as regras do projeto que têm precedência sobre elas |
| `.agent/context.md`, `architecture.md`, `conventions.md`, `decisions.md`, `backlog_rules.md`, `glossary.md`, `runbook.md`, `project-reference.md` | Contexto do projeto, escrito a partir de `docs/especificacao.md` |
| `.agent/skills/android/` | Convenções de arquitetura e de testes para Android |
| `.agent/agents/` | Os papéis do SDD: planner, architect, builder, tester, reviewer e conductor |
| `.agent/plans/` | Um plano por spec |
| `.agent/specs/features/` e `.agent/specs/acceptance/` | A spec e os critérios de aceite de cada spec, em formato `.toon` |

Você não precisa instalar nada: a pasta `.agent/` já está versionada.

### Fluxo de uma spec

1. **Prepare a branch.** A partir da `main` atualizada, crie `spec-NNN-descricao-curta`.
2. **Planeje** com `@planning`. O agente escreve o plano e os dois arquivos `.toon`, sem código.
3. **Revise o plano.** Confira versões, decisões e o que ficou em aberto. Responda às perguntas do agente antes de liberar a implementação.
4. **Implemente** com `@implementation`, um commit por tarefa do plano.
5. **Teste** com `@test-plan` e `@test`, quando a spec tiver comportamento para testar.
6. **Revise** com `@review` antes de abrir o pull request.
7. **Abra o pull request** seguindo o template, com `Closes #N`.

Em specs só de estrutura, como a SPEC-001, o builder também escreve os testes de fumaça e a etapa do architect pode ser pulada, porque não há contrato de API.

### Exemplos de pedido

Planejar uma spec:

```
@planning
Planeje a SPEC-010 (issue #6): modelos e contratos do domínio.

Critérios de aceite: compila em Kotlin puro; modelos imutáveis.

Leia .agent/SKILLS.md e siga as skills listadas. Se não tiver certeza de uma versão ou de uma decisão que a especificação não cobre, pergunte em vez de inventar. Não escreva código ainda. Me mostre o plano.
```

Implementar depois do plano aprovado:

```
@implementation
Plano da SPEC-010 aprovado. Implemente um commit por tarefa, sem push. Se um passo falhar, pare e me mostre o erro, sem contornar. Ao fim, me mostre a saída de "./gradlew build".
```

Abrir o pull request:

```
Rode "git fetch origin" e "git merge origin/main". Se houver conflito, pare e me mostre.
Faça o push da branch e abra o pull request seguindo .github/pull_request_template.md, com SPEC-010 e "Closes #6".
Marque no checklist só o que foi verificado, e diga como. Não faça o merge.
```

Para um agente que não reconhece os comandos com `@`, peça o papel pelo arquivo:

```
Leia .agent/SKILLS.md e siga as skills listadas. Depois aja como o planner definido em .agent/agents/planner.md e planeje a SPEC-010 (issue #6).
```

### Combinados que valem para qualquer agente

- **Perguntar em vez de inventar.** Versão de biblioteca, nome de pacote, permissão ou regra que a especificação não define é pergunta, não suposição.
- **Parar quando falha.** Se uma ferramenta não funciona com as versões do projeto, o agente para e avisa. Ele não troca versão, não desliga verificação e não cria código só para um teste passar.
- **Um commit por tarefa, sem push** até a revisão de quem está conduzindo.
- **Nada fora da spec.** Permissão, dependência ou arquivo que a spec não nomeia fica de fora. O que aparecer no caminho vira anotação em "Fora do escopo ou pendente" no pull request.
- **Checklist honesto.** Item que não se aplica fica desmarcado com "não se aplica". Item marcado diz como foi verificado.
- **O merge é de uma pessoa.** O agente abre o pull request e informa o resultado do CI. Quem faz o merge é o mantenedor.

### Particularidades deste projeto

- **Specs sem HTTP.** O schema do SDD foi pensado para APIs. Aqui, o campo `status` das specs guarda o código de saída do comando (`0` ou `1`), e a checagem de código HTTP do validador não se aplica. Está registrado em "Project Overrides" no `SKILLS.md`.
- **AGP 9.** O Kotlin já vem embutido no plugin do Android, então o plugin `kotlin-android` que a skill mostra não é aplicado.
- **Regras do projeto vencem as skills.** Quando `conventions.md` ou "Project Overrides" dizem uma coisa e um exemplo da skill diz outra, vale a regra do projeto.

### Atualizar o SDD

Quando sair uma versão nova do SDD, rode o instalador de novo com a stack `android`, com tudo já commitado, em um pull request só para isso. A reinstalação substitui as skills, os scripts e os agentes, e preserva o `SKILLS.md` e os documentos de contexto. Confira o `git diff` antes do commit: só devem mudar arquivos em `.agent/skills/`, `.agent/scripts/` e `.agent/agents/`.

## Ambiente

### Pré-requisitos

- **JDK 21 instalado (obrigatório).** O arquivo `gradle/gradle-daemon-jvm.properties` fixa o JDK do daemon do Gradle em 21 (`toolchainVersion=21`). Com ele, o Gradle procura um JDK 21 instalado e roda nele mesmo que o `JAVA_HOME` aponte para outra versão. Sem JDK 21 na máquina, o Gradle não acha um JVM compatível e para. O motivo: o detekt 1.23.8 roda dentro do processo do Gradle e não reconhece JVMs mais novos (falha no JDK 25).
- **Android SDK** com a plataforma 37 (`platforms;android-37.0`, o compileSdk) e o Build Tools correspondente. O targetSdk é 36. Se a plataforma 37 faltar e as licenças do SDK estiverem aceitas, o AGP baixa sozinho no primeiro build; senão, instale pelo SDK Manager do Android Studio.
- **`local.properties` na raiz** com o caminho do Android SDK. O Android Studio cria o arquivo ao sincronizar. Fora dele, crie à mão:

  ```properties
  # Windows (barras invertidas escapadas)
  sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
  # Linux / macOS
  sdk.dir=/home/<usuario>/Android/Sdk
  ```

  O `local.properties` é pessoal e está no `.gitignore`: nunca faça commit dele. A variável de ambiente `ANDROID_HOME` também serve.

### Comandos

Na raiz do repositório (no Windows, `gradlew.bat` ou `.\gradlew`):

```bash
./gradlew build          # compila os seis módulos e roda lint, ktlint, detekt, testes e a cobertura do core-domain (é o que o CI roda)
./gradlew test           # só os testes: JUnit 5 no core-domain, JUnit 4 nos módulos Android (Robolectric no app)
./gradlew ktlintCheck    # estilo (ktlint 1.8.0)
./gradlew ktlintFormat   # corrige o que o ktlint consegue corrigir sozinho
./gradlew detekt         # análise estática (detekt 1.23.8)
./gradlew :app:assembleDebug   # APK de debug do app
./gradlew :app:installDebug    # instala no aparelho ou emulador conectado; abra pelo ícone "Tap Guard"
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```

Para um módulo só, use o prefixo do módulo, por exemplo `./gradlew :core-domain:test`.

Todas as versões (Kotlin, AGP, SDKs, JDK, ktlint, detekt, Kover, JUnit) ficam só em `gradle/libs.versions.toml`.

### Cobertura

O `core-domain` exige no mínimo 80% de cobertura de linhas (Kover). Abaixo disso, o `./gradlew build` falha no `:core-domain:koverVerify`. Enquanto o módulo não tem código de produção, a regra passa sem ter o que medir; ela começa a valer na primeira classe com código executável (interfaces não contam, `data class` conta). Os outros módulos não têm meta de cobertura.

### CI

Todo pull request para `main` roda dois jobs, ambos obrigatórios para o merge:

- **Regras do projeto:** nenhum manifesto declara `INTERNET`; scripts shell com final de linha LF.
- **Build e testes:** valida o `gradle-wrapper.jar` contra os checksums oficiais do Gradle, roda `./gradlew build` no JDK 21 e publica o APK de debug do `app` como artefato `app-debug`, que pode ser baixado na página do run em Actions.
