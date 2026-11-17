# Runbook

> Build Gradle criado na SPEC-001. Desde a SPEC-003 o `app` abre: tema, componente de nível e as nove telas vazias ligadas pela navegação provisória. Testes instrumentados chegam nas specs seguintes.

## Prerequisites
- **JDK 21 instalado (obrigatório).** `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) faz o daemon do Gradle rodar num JDK 21 instalado mesmo com outro `JAVA_HOME`. Sem JDK 21, o Gradle para.
- Android SDK com a plataforma 37 (`platforms;android-37.0`) e o Build Tools correspondente (minSdk 26, targetSdk 36, compileSdk 37; ADR-013).
- `local.properties` na raiz com `sdk.dir=<caminho do Android SDK>` (o Android Studio cria ao sincronizar; nunca vai para o Git). `ANDROID_HOME` também serve.
- Versões (todas em `gradle/libs.versions.toml`): Gradle 9.7.0 (wrapper), AGP 9.3.3, Kotlin 2.4.21, ktlint-gradle 14.2.0 com motor ktlint 1.8.0, detekt 1.23.8, Kover 0.9.11, JUnit 5.14.4 e JUnit 4.13.2; no `app`, Compose BOM 2026.09.00, `activity-compose` 1.13.0, `navigation-compose` 2.10.2, Robolectric 4.18 e `androidx.test:core` 1.7.0; no `core-data` (e Hilt no `app`), KSP 2.3.14, Hilt 2.60.1, Room 2.8.5, DataStore Preferences 1.3.0, `room-testing`, `hilt-android-testing` e `kotlinx-coroutines-test` 1.11.0.
- `adb`, para instalar o `decoy-app` (simula origem fora da loja).
- Emulador Android (CI e testes ponta a ponta).
- Aparelho físico com NFC e um cartão Visa ou Mastercard por aproximação, para o teste manual do F2.

## How to Run
```bash
# APK de debug do app
./gradlew :app:assembleDebug
# instalar no aparelho/emulador conectado e abrir (ou tocar no ícone "Tap Guard")
./gradlew :app:installDebug
adb shell am start -n io.github.valossa515.tapguard/.MainActivity

# instalar o app isca (uma das variantes: nfcOnly, hce, full, lookalike)
TODO: no code yet  # a especificação só define que a instalação é por `adb install`
```

## How to Test
```bash
# todos os testes unitários (JUnit 5 no core-domain, JUnit 4 nos módulos Android)
./gradlew test

# só o domínio
./gradlew :core-domain:test

# Robolectric no app (JUnit 4, sem aparelho): navegação, níveis, tema, cores e manifesto mesclado
./gradlew :app:testDebugUnitTest

# Robolectric no core-data: DAOs em memória, migração inicial, preferências e grafo do Hilt
# (no Windows, as duas escritas do SettingsDataSourceTest aparecem como ignoradas; rodam no CI)
./gradlew :core-data:testDebugUnitTest

# Robolectric nos coletores: entra com a SPEC-011 em diante
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
- **CI (`.github/workflows/ci.yml`):** job "Regras do projeto" (sem `INTERNET` nos manifestos; regras de backup no manifesto do `app`; scripts com LF) e job "Build e testes" (valida o `gradle-wrapper.jar` com `gradle/actions/wrapper-validation` v6.4.1, roda `./gradlew build` no JDK 21, falha se o build regerar `core-data/schemas` com diferença do commitado, e publica `app/build/outputs/apk/debug/app-debug.apk` no artefato `app-debug` com `actions/upload-artifact` v7.0.2). Os dois são checks obrigatórios da ruleset da `main`. As actions novas são referenciadas pelo SHA do commit, com a versão em comentário.
- **Baixar o APK de debug:** na aba Actions, abrir o run do PR ou da `main` e baixar o artefato `app-debug` (zip com o `app-debug.apk`). Retenção: o padrão do repositório.
- Migrações Room: schema exportado em `core-data/schemas` e versionado (versão 1 desde a SPEC-004). Até a primeira versão pública, a versão 1 pode mudar com o schema regerado e commitado junto; depois, cada mudança sobe a versão e traz uma `Migration` e um teste de migração (ADR-014). `fallbackToDestructiveMigration()` é proibido em release. Teste de migração: `./gradlew :core-data:testDebugUnitTest --tests '*TapGuardDatabaseMigrationTest'`.
- Distribuição: Play Store (exige declaração de `QUERY_ALL_PACKAGES` como app de segurança) e GitHub enquanto isso. Publicar na Play Store já na 1.0 é decisão em aberto.

## Troubleshooting
Problemas previstos na especificação (seção 13):
- **Vigia atrasa ou não roda** -> fabricantes matam tarefas em segundo plano; a varredura também roda ao abrir o app; orientar o usuário sobre economia de bateria nas configurações.
- **S7 / RF-303 falham em versão nova do Android** -> API de pagamento padrão mudou (Android 15+: papel de carteira); coletor isolado deve degradar sem erro; testar em Android 8, 12, 14 e na versão mais recente.
- **Receptor de instalação não dispara** -> receptor no manifesto não é confiável desde o Android 8; a detecção confiável é a varredura periódica.
- **`compileSdk` 37 não encontrado** (`Failed to find target with hash string 'android-37.0'` ou `checkDebugAarMetadata` exigindo 37) -> instale a `Android SDK Platform 37.0` pelo SDK Manager, ou aceite as licenças do SDK para o AGP baixar sozinho. O targetSdk continua 36.
- **"Schema do Room commitado" falha no CI** -> uma entidade mudou e o `core-data/schemas/.../N.json` regerado não foi commitado. Rode `./gradlew build` e commite o schema.
- **"Regras de backup no manifesto" falha no CI** -> falta `android:allowBackup="false"`, `android:dataExtractionRules` ou `android:fullBackupContent` no `app/src/main/AndroidManifest.xml`.
- **`Unable to rename …settings.preferences_pb.tmp` num teste local no Windows** -> a JVM do Windows não renomeia o temporário do DataStore por cima do arquivo existente. Testes que gravam mais de uma vez usam `Assume.assumeFalse(<Windows>)` e rodam no CI.
- **`MergedManifestTest` falha com "Permissões fora da lista"** -> uma dependência nova trouxe permissão, ou alguém declarou uma. Ela só entra com decisão registrada no `SKILLS.md` › Permissions e na lista do teste, na mesma spec.
- **`IllegalArgumentException: 25` no detekt** (ou outra versão nova em `JavaVersion.current()`) -> o Gradle está rodando num JDK mais novo que o 21. Confira se `gradle/gradle-daemon-jvm.properties` existe e se há um JDK 21 instalado; rode `./gradlew --stop` e tente de novo.
- **Gradle não acha um JVM compatível** -> não há JDK 21 instalado. Instale um (por exemplo, Temurin 21).
- **`SDK location not found`** -> falta o `local.properties` com `sdk.dir` (ou a variável `ANDROID_HOME`).
- **`Rule violated: lines covered percentage is …, but expected minimum is 80`** -> a cobertura do `core-domain` caiu abaixo de 80%. Rode `./gradlew :core-domain:koverHtmlReport` e veja as linhas descobertas em `core-domain/build/reports/kover/html/index.html`.
- **"Valida o wrapper do Gradle" falha no CI** -> o `gradle-wrapper.jar` não bate com um checksum oficial. Regere o wrapper com `./gradlew wrapper --gradle-version <versão>` a partir de uma distribuição oficial; nunca faça commit de um JAR de outra origem.
- **ktlint reclama de `chain-method-continuation` nos `build.gradle.kts`** -> quebrar a linha antes de cada `.` em expressões como `libs.versions.minSdk.get().toInt()`, ou rodar `./gradlew ktlintFormat`.
