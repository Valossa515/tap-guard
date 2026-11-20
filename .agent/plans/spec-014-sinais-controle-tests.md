# Test Plan: SPEC-014 Sinais de controle (S4, S5, S6, S8)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-014-sinais-controle.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-014-sinais-controle.acceptance.toon` (versão 1.0) · 2026-11-20
> **BATCH-01** (esta seção): 4 alvos e 22 cenários (5 + 7 + 1 + 9), um método de teste por cenário. O parametrizado das permissões de SMS roda 5 casos. Execuções novas: 26 no `core-detection`, todas na JVM.
> **BATCH-02:** a seção do S4 (coletor, `InstalledAppTest` e inventário no Robolectric) está no fim deste arquivo.

## Scope (BATCH-01)
- **Coletor de S5:** `OverlayPermissionCollector` gera S5 só com `android.permission.SYSTEM_ALERT_WINDOW`, com comparação exata, e nunca devolve outro sinal (AC-001 a AC-003, AC-013).
- **Coletor de S8:** `SmsPermissionCollector` gera um só S8 com `READ_SMS`, `RECEIVE_SMS` ou as duas; as permissões de SMS que não contam não geram; comparação exata; nunca devolve outro sinal (AC-004 a AC-007, AC-013).
- **Coletor de S6:** `RecentInstallOrUpdateCollector` com `Clock.fixed`:
  - instalação ou atualização dentro dos 7 dias gera S6; as duas antigas não geram;
  - o limite é inclusivo (168 h exatas contam; 7 dias e 1 ms não);
  - horário no futuro conta, na instalação e só na atualização;
  - o mesmo app contra dois relógios mostra que o agora vem do `Clock`;
  - nunca devolve outro sinal (AC-008 a AC-013).
- **Onde roda:** JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`, dentro do `./gradlew build`.
- **Fora (contrato, `excluded`):**
  - ausência de `import android.*`, de `Instant.now` e de Hilt nos coletores (inspeção no review, AC-012 e AC-020);
  - relógio de aparelho real (o coletor só vê o `Clock`).
  - AC-021 e AC-022 são conferidos pelo build e pelo `git status`.

## Padrões seguidos (pattern analysis)
- **`NfcPermissionCollectorTest` e `CardEmulationServiceCollectorTest` (SPEC-013):** `runTest`, asserções do JUnit 4, constantes num `private companion object`, nomes em frase com crases, laço com mensagem para os casos de maiúsculas.
- **`NfcPermissionCollectorRelatedPermissionTest` (SPEC-013):** parametrizado com o parâmetro no construtor, `@JvmStatic @Parameterized.Parameters(name = "{0}")` e lista literal.
- **`InstallSourceCollectorTest` (SPEC-012):** fixture `anInstalledApp` do `testFixtures` do `core-domain`.
- `skills/android/testing.md`: "inject a `Clock`" e "❌ Reading `Instant.now()` in code under test".

## Unit tests

### Coletor de S5 — `core-detection/src/test/.../signal/OverlayPermissionCollectorTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | overlay-permission-yields-s5 | `SYSTEM_ALERT_WINDOW` com `RECEIVE_SMS` → `{OVERLAY_PERMISSION}` | AC-001 |
| 2 | no-permissions-yields-nothing | conjunto vazio | AC-002 |
| 3 | unrelated-permission-yields-nothing | só `RECEIVE_SMS` → conjunto vazio | AC-002 |
| 4 | overlay-permission-in-other-case-yields-nothing | `ANDROID.PERMISSION.SYSTEM_ALERT_WINDOW` e `android.permission.system_alert_window` → conjunto vazio | AC-003 |
| 5 | yields-only-s5-for-app-with-all-batch-signals | `SYSTEM_ALERT_WINDOW`, `READ_SMS`, `NFC`, HCE e instalador nulo → exatamente `{OVERLAY_PERMISSION}` | AC-013 |

### Coletor de S8 — `core-detection/src/test/.../signal/SmsPermissionCollectorTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | read-sms-yields-s8 | `{SMS_PERMISSION}` | AC-004 |
| 2 | receive-sms-yields-s8 | `{SMS_PERMISSION}` | AC-004 |
| 3 | both-sms-permissions-yield-one-s8 | `{SMS_PERMISSION}` | AC-004 |
| 4 | no-permissions-yields-nothing | conjunto vazio | AC-005 |
| 5 | unrelated-permission-yields-nothing | só `NFC` → conjunto vazio | AC-005 |
| 6 | sms-permission-in-other-case-yields-nothing | `ANDROID.PERMISSION.READ_SMS` e `android.permission.receive_sms` → conjunto vazio | AC-007 |
| 7 | yields-only-s8-for-app-with-all-batch-signals | exatamente `{SMS_PERMISSION}` | AC-013 |

### Permissões de SMS que não contam — `core-detection/src/test/.../signal/SmsPermissionCollectorUnrelatedPermissionTest.kt` (parametrizado, 5 casos)
| # | Scenario | Parameters | Expected result | AC |
|---|----------|------------|-----------------|----|
| 1 | unrelated-sms-permission-yields-nothing | `SEND_SMS`, `RECEIVE_MMS`, `RECEIVE_WAP_PUSH`, `READ_CELL_BROADCASTS`, `BROADCAST_SMS` (prefixo `android.permission.`) | conjunto vazio | AC-006 |

### Coletor de S6 — `core-detection/src/test/.../signal/RecentInstallOrUpdateCollectorTest.kt` (JUnit 4, `NOW` = 2026-01-12T00:00:00Z)
| # | Scenario | Setup | Expected result | AC |
|---|----------|-------|-----------------|----|
| 1 | install-one-day-ago-yields-s6 | instalado e atualizado em `NOW − 1 dia` | `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-008 |
| 2 | install-and-update-thirty-days-ago-yield-nothing | as duas datas em `NOW − 30 dias` | conjunto vazio | AC-008 |
| 3 | old-install-with-update-two-days-ago-yields-s6 | instalado em `NOW − 400 dias`, atualizado em `NOW − 2 dias` | `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-009 |
| 4 | latest-exactly-seven-days-ago-yields-s6 | as duas datas em `NOW − 168 h` | `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-010 |
| 5 | latest-seven-days-and-one-milli-ago-yields-nothing | as duas datas em `NOW − 7 dias − 1 ms` | conjunto vazio | AC-010 |
| 6 | install-and-update-in-the-future-yield-s6 | as duas datas em `NOW + 1 h` | `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-011 |
| 7 | old-install-with-update-in-the-future-yields-s6 | instalado em `NOW − 30 dias`, atualizado em `NOW + 3 dias` | `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-011 |
| 8 | same-app-against-two-clocks | atualizado em 2026-01-10; relógios em 2026-01-12 e 2026-02-12 | S6 com o primeiro; conjunto vazio com o segundo | AC-012 |
| 9 | yields-only-s6-for-app-with-all-batch-signals | datas em `NOW`, `SYSTEM_ALERT_WINDOW`, `READ_SMS`, `NFC`, HCE, instalador nulo | exatamente `{RECENTLY_INSTALLED_OR_UPDATED}` | AC-013 |

## Edge cases
- Limite exato e 1 ms além (cenários 4 e 5 do S6).
- Atualização no futuro com instalação fora da janela (cenário 7 do S6): o `maxOf` escolhe a atualização.
- As duas permissões de SMS juntas geram um só S8 (cenário 3 do S8).

## Test data & fixtures
- **`anInstalledApp(...)`** ganha `firstInstallTime` e `lastUpdateTime`, com os valores de antes como padrão (2026-01-01 e 2026-01-02), então os testes que já existem não mudam.
- **No teste do S6:** `appInstalledAt(instant)` põe as duas datas iguais, como num app recém-instalado no aparelho; `collectorAt(now)` monta o coletor com `Clock.fixed(now, ZoneOffset.UTC)`.
- Permissões escritas à mão no teste, não lidas da produção: mudar a constante na produção derruba o teste.

## Infrastructure
- Nenhuma biblioteca nova: `junit4`, `kotlinx-coroutines-test` e `testFixtures(project(":core-domain"))` já estão no `core-detection`; `java.time` é do JDK.


---

# BATCH-02: S4 (serviço de acessibilidade ligado)

> Fonte: o mesmo contrato (versão 1.2) e os critérios AC-013 a AC-019 · 2026-11-20
> 3 alvos e 12 cenários (4 + 3 + 5), um método de teste por cenário.
> - Os 5 cenários do inventário rodam nos cinco níveis de API; 4 são novos e 1 (conjuntos vazios) ganha uma asserção.
> - Execuções novas ou alteradas: 4 no coletor (novas), 3 no `core-domain` (alteradas) e 25 no inventário (5 × 5; 20 novas).

## Scope (BATCH-02)
- **Modelo:** `InstalledApp.enabledAccessibilityServices` é lido de volta, copiado e recusa alteração por cast, como as outras coleções (AC-019).
- **Coletor de S4:** `EnabledAccessibilityServiceCollector` gera S4 quando o campo não está vazio; um serviço só declarado em `services` não gera; nunca devolve outro sinal (AC-013, AC-014).
- **Inventário:** leitura de `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` no Robolectric, nos cinco níveis de API (AC-015 a AC-018). Cobre:
  - formatos completo e curto, sem cruzar com `services`;
  - chave nula e vazia;
  - entradas malformadas;
  - isolamento por pacote, app do sistema, próprio pacote e pacote não instalado.
- **Onde roda:**
  - `core-domain`: JUnit 5 (Kover);
  - coletor: JUnit 4 na JVM, sem Robolectric e sem `@Config(sdk)`;
  - inventário: Robolectric com o `@Config(sdk = [26, 28, 30, 33, 36])` da classe.
  - Tudo roda no `./gradlew build`.
- **Fora (contrato, `excluded`):**
  - falha do provedor de configurações (o shadow não a simula; o framework devolve `null`; ADR-019);
  - leitura recusada pelo `@Readable` (o Robolectric não o aplica; fontes do AOSP e SPEC-022);
  - `SecurityException` propagada (sem como provocar; a leitura não tem `try`, revisão);
  - ausência de `import android.*` e de Hilt no coletor (inspeção, AC-020).

## Padrões seguidos (pattern analysis)
- **`CardEmulationServiceCollectorTest` (SPEC-013):** campo do `InstalledApp` não vazio gera o sinal; serviço comum em `services` não gera; "só o seu sinal" com todos os dados.
- **Cenários de HCE no `PackageManagerAppInventoryTest` (SPEC-013):** `install(...)`, `aPackageInfo(...)`, `aService(...)`, pacotes `com.example.a…`/`b…`, app do sistema com `FLAG_SYSTEM`, mapa de pacote para conjunto.
- **`InstalledAppTest` (SPEC-010 e SPEC-013):** JUnit 5, laço sobre as coleções expostas com cast para `MutableSet`.
- **Experimento do plano (2026-11-20):** `Settings.Secure.putString` nas cinco APIs, chave nula no começo de cada teste, `/OnlyClass` com pacote vazio.

## Unit tests

### Modelo — `core-domain/src/test/.../inventory/InstalledAppTest.kt` (JUnit 5, alterado)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | fields-are-read-back-including-enabled-accessibility-services | `enabledAccessibilityServices` igual ao passado | AC-019 |
| 2 | mutating-source-collections-does-not-change-the-model | limpar o `MutableSet` de origem não muda o campo | AC-019 |
| 3 | exposed-collections-reject-mutation-through-cast | `add`, `remove` e `clear` por cast lançam `UnsupportedOperationException` | AC-019 |

### Coletor de S4 — `core-detection/src/test/.../signal/EnabledAccessibilityServiceCollectorTest.kt` (JUnit 4)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | enabled-service-yields-s4 | `{ACCESSIBILITY_SERVICE_ENABLED}` | AC-014 |
| 2 | no-enabled-service-yields-nothing | conjunto vazio | AC-014 |
| 3 | declared-but-not-enabled-service-yields-nothing | `services` com o serviço e o campo vazio → conjunto vazio | AC-014 |
| 4 | yields-only-s4-for-app-with-all-spec-signals | `SYSTEM_ALERT_WINDOW`, `READ_SMS`, `NFC`, HCE, instalador nulo e serviço ligado → exatamente `{ACCESSIBILITY_SERVICE_ENABLED}` | AC-013 |

## Integration tests (Robolectric, APIs 26, 28, 30, 33 e 36)

### Inventário — `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt` (alterado)
| # | Scenario | Setup | Expected result | AC |
|---|----------|-------|-----------------|----|
| 1 | fills-enabled-accessibility-services-from-full-and-short-forms-and-keeps-services | app declara `A11yService`; chave com `pkg/pkg.A11yService` e `pkg/.SecondA11yService` (esta não declarada) | o campo tem os dois nomes completos; `services` só com o declarado | AC-015 |
| 2 | null-or-empty-key-leaves-every-app-without-enabled-services | dois apps; chave `null` e depois `""` | os dois apps com o campo vazio nos dois casos; sem exceção | AC-016 |
| 3 | ignores-malformed-entries-and-keeps-the-valid-one | `:garbage:/OnlyClass:com.pkg/:pkg/.A11yService:` | só o serviço válido; sem exceção | AC-017 |
| 4 | keeps-enabled-services-per-package-and-ignores-system-own-and-missing-packages | `USER_APP`, `OTHER_APP` e app do sistema; chave com serviços de `USER_APP`, do app do sistema, do próprio pacote e de `com.example.missing` | só `USER_APP` com o serviço; `OTHER_APP` vazio; app do sistema e próprio pacote ausentes; pacote não instalado não gera app | AC-018 |
| 5 | returns-empty-sets-without-permissions-or-services (existente) | app sem nada | também `enabledAccessibilityServices` vazio | AC-016 |

## Edge cases
- `/OnlyClass` não volta `null` em `unflattenFromString`: volta pacote vazio, que fica fora pelo cruzamento com a lista (cenário 3).
- Dois-pontos no fim e entrada vazia no começo (cenário 3).
- Serviço ligado que o app não declara (cenário 1, critério mais largo).

## Test data & fixtures
- **`anInstalledApp(...)`** ganha `enabledAccessibilityServices` com padrão vazio (a fixture já tem o `@Suppress` da emenda 1.2).
- **`enableAccessibilityServices(value: String?)`**, novo no `PackageManagerAppInventoryTest`: `Settings.Secure.putString(context.contentResolver, "enabled_accessibility_services", value)`. O KDoc explica que basta nas cinco APIs, que a chave volta a `null` em cada teste e que o Robolectric não aplica o `@Readable`.
- **Constantes:** a chave escrita à mão (`enabled_accessibility_services`), não lida da produção: trocar a chave na produção derruba os testes.

## Infrastructure
- Nenhuma biblioteca nova: `robolectric`, `androidx.test:core`, `junit4`, `kotlinx-coroutines-test` e `testFixtures(project(":core-domain"))` já estão no `core-detection`; JUnit 5 no `core-domain`.
