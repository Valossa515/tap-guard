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

### Pré-requisitos

- **JDK 21 instalado (obrigatório).** O arquivo `gradle/gradle-daemon-jvm.properties` fixa o JDK do daemon do Gradle em 21 (`toolchainVersion=21`). Com ele, o Gradle procura um JDK 21 instalado e roda nele mesmo que o `JAVA_HOME` aponte para outra versão. Sem JDK 21 na máquina, o Gradle não acha um JVM compatível e para. O motivo: o detekt 1.23.8 roda dentro do processo do Gradle e não reconhece JVMs mais novos (falha no JDK 25).
- **Android SDK** com a plataforma 36 (compileSdk e targetSdk) e o Build Tools correspondente.
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
./gradlew test           # só os testes: JUnit 5 no core-domain, JUnit 4 nos módulos Android
./gradlew ktlintCheck    # estilo (ktlint 1.8.0)
./gradlew ktlintFormat   # corrige o que o ktlint consegue corrigir sozinho
./gradlew detekt         # análise estática (detekt 1.23.8)
./gradlew :app:assembleDebug   # APK de debug do app
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
