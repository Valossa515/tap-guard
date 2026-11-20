# Test Plan: SPEC-014 Sinais de controle (S4, S5, S6, S8)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-014-sinais-controle.contract.toon` (versão 1.0) e critérios de `.agent/specs/acceptance/spec-014-sinais-controle.acceptance.toon` (versão 1.0) · 2026-11-20
> **BATCH-01** (esta seção): 4 alvos e 22 cenários (5 + 7 + 1 + 9), um método de teste por cenário. O parametrizado das permissões de SMS roda 5 casos. Execuções novas: 26 no `core-detection`, todas na JVM.
> **BATCH-02:** a seção do S4 (coletor, `InstalledAppTest` e inventário no Robolectric) entra no PR do BATCH-02.

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
