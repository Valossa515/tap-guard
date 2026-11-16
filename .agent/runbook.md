# Runbook

> Build Gradle criado na SPEC-001: seis módulos, sem funcionalidade ainda. Comandos de instalação e de testes instrumentados chegam nas specs seguintes.

## Prerequisites
- **JDK 21 instalado (obrigatório).** `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) faz o daemon do Gradle rodar num JDK 21 instalado mesmo com outro `JAVA_HOME`. Sem JDK 21, o Gradle para.
- Android SDK com a plataforma 36 e o Build Tools correspondente (minSdk 26, targetSdk 36, compileSdk 36).
- `local.properties` na raiz com `sdk.dir=<caminho do Android SDK>` (o Android Studio cria ao sincronizar; nunca vai para o Git). `ANDROID_HOME` também serve.
- Versões (todas em `gradle/libs.versions.toml`): Gradle 9.7.0 (wrapper), AGP 9.3.3, Kotlin 2.4.21, ktlint-gradle 14.2.0 com motor ktlint 1.8.0, detekt 1.23.8, Kover 0.9.11, JUnit 5.14.4 e JUnit 4.13.2.
- `adb`, para instalar o `decoy-app` (simula origem fora da loja).
- Emulador Android (CI e testes ponta a ponta).
- Aparelho físico com NFC e um cartão Visa ou Mastercard por aproximação, para o teste manual do F2.

## How to Run
```bash
# APK de debug do app (ainda sem Activity: instala, mas não abre nada)
./gradlew :app:assembleDebug
# instalar no aparelho/emulador conectado: TODO (a partir da spec que criar a primeira tela)

# instalar o app isca (uma das variantes: nfcOnly, hce, full, lookalike)
TODO: no code yet  # a especificação só define que a instalação é por `adb install`
```

## How to Test
```bash
# todos os testes unitários (JUnit 5 no core-domain, JUnit 4 nos módulos Android)
./gradlew test

# só o domínio
./gradlew :core-domain:test

# Robolectric (módulos Android, JUnit 4): entra com os coletores
TODO: no code yet

# instrumentados / Compose UI / ponta a ponta
TODO: no code yet

# lint
./gradlew ktlintCheck detekt
./gradlew ktlintFormat   # corrige o estilo automaticamente

# cobertura do core-domain (Kover, mínimo de 80% de linhas; o koverVerify também roda no build)
./gradlew :core-domain:koverVerify       # confere o mínimo de 80% de cobertura de linhas (também roda no build)
./gradlew :core-domain:koverHtmlReport   # relatório em core-domain/build/reports/kover/html/index.html
./gradlew :core-domain:koverXmlReport    # relatório em core-domain/build/reports/kover/report.xml
```

Verificações automáticas de privacidade **(planned)**:
- Teste que falha se o manifesto final declarar `INTERNET`.
- Teste que procura o número completo do cartão sintético em banco, preferências e logs depois de uma leitura.

## Build and Release
```bash
# build completo: compila, roda lint do Android, ktlint, detekt, testes e a cobertura do core-domain (é o que o CI roda)
./gradlew build

# release 1.0 (Etapa 7)
TODO: no code yet
```
- **CI (`.github/workflows/ci.yml`):** job "Regras do projeto" (sem `INTERNET` nos manifestos; scripts com LF) e job "Build e testes" (valida o `gradle-wrapper.jar` com `gradle/actions/wrapper-validation` v6.4.1, roda `./gradlew build` no JDK 21 e publica `app/build/outputs/apk/debug/app-debug.apk` no artefato `app-debug` com `actions/upload-artifact` v7.0.2). Os dois são checks obrigatórios da ruleset da `main`. As actions novas são referenciadas pelo SHA do commit, com a versão em comentário.
- **Baixar o APK de debug:** na aba Actions, abrir o run do PR ou da `main` e baixar o artefato `app-debug` (zip com o `app-debug.apk`). Retenção: o padrão do repositório.
- Migrações Room: schema exportado em `core-data/schemas` e versionado. Cada mudança de versão traz uma `Migration` e um teste de migração. `fallbackToDestructiveMigration()` é proibido em release. Comando para rodar os testes de migração: TODO: no code yet.
- Distribuição: Play Store (exige declaração de `QUERY_ALL_PACKAGES` como app de segurança) e GitHub enquanto isso. Publicar na Play Store já na 1.0 é decisão em aberto.

## Troubleshooting
Problemas previstos na especificação (seção 13):
- **Vigia atrasa ou não roda** -> fabricantes matam tarefas em segundo plano; a varredura também roda ao abrir o app; orientar o usuário sobre economia de bateria nas configurações.
- **S7 / RF-303 falham em versão nova do Android** -> API de pagamento padrão mudou (Android 15+: papel de carteira); coletor isolado deve degradar sem erro; testar em Android 8, 12, 14 e na versão mais recente.
- **Receptor de instalação não dispara** -> receptor no manifesto não é confiável desde o Android 8; a detecção confiável é a varredura periódica.
- **`IllegalArgumentException: 25` no detekt** (ou outra versão nova em `JavaVersion.current()`) -> o Gradle está rodando num JDK mais novo que o 21. Confira se `gradle/gradle-daemon-jvm.properties` existe e se há um JDK 21 instalado; rode `./gradlew --stop` e tente de novo.
- **Gradle não acha um JVM compatível** -> não há JDK 21 instalado. Instale um (por exemplo, Temurin 21).
- **`SDK location not found`** -> falta o `local.properties` com `sdk.dir` (ou a variável `ANDROID_HOME`).
- **`Rule violated: lines covered percentage is …, but expected minimum is 80`** -> a cobertura do `core-domain` caiu abaixo de 80%. Rode `./gradlew :core-domain:koverHtmlReport` e veja as linhas descobertas em `core-domain/build/reports/kover/html/index.html`.
- **"Valida o wrapper do Gradle" falha no CI** -> o `gradle-wrapper.jar` não bate com um checksum oficial. Regere o wrapper com `./gradlew wrapper --gradle-version <versão>` a partir de uma distribuição oficial; nunca faça commit de um JAR de outra origem.
- **ktlint reclama de `chain-method-continuation` nos `build.gradle.kts`** -> quebrar a linha antes de cada `.` em expressões como `libs.versions.minSdk.get().toInt()`, ou rodar `./gradlew ktlintFormat`.
