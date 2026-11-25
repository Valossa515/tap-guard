# Test Plan: SPEC-016 Pagamento padrão (S7)

> Fonte: `test-strategy` de `.agent/specs/contracts/spec-016-pagamento-padrao.contract.toon` (versão 1.1) e critérios de `.agent/specs/acceptance/spec-016-pagamento-padrao.acceptance.toon` (versão 1.1) · 2026-11-24
> Quatro alvos: coletor (7 cenários), modelo (3 testes estendidos), inventário (9 cenários novos e 1 estendido, nas cinco APIs) e os shadows de apoio. Execuções novas: 7 do coletor e 45 do inventário (9 × 5) no `core-detection`; o `core-domain` continua com 32.

## Scope
- **Coletor de S7:** `DefaultPaymentAppCollector` gera S7 quando o app é o padrão e o pacote está fora da lista; não gera para a carteira da lista nem para o app que não é o padrão; comparação exata; conjunto copiado; lista vazia; nunca devolve outro sinal (AC-001 a AC-006).
- **Modelo:** `InstalledApp.defaultPaymentServices` é lido de volta, copiado e não pode ser alterado nem por cast (AC-016).
- **Inventário:** a etapa do pagamento padrão no Robolectric, nas APIs 26, 28, 30, 33 e 36 (AC-007 a AC-015):
  - marca só o serviço que o sistema diz ser o padrão, só em apps de usuário;
  - papel de carteira: todos os serviços do dono;
  - só serviços ligados (estado atual) e exportados são consultados; ligado em tempo de execução conta;
  - sem NFC, NFC sem HCE e serviço NFC morto deixam o campo vazio sem erro; sem a permissão `NFC`, a `SecurityException` propaga.
- **Fora (contrato, `excluded`):** registro real do serviço no sistema NFC, papel de carteira real e permissão cobrada (SPEC-022); diferença entre as APIs (é do serviço NFC); formato e troca pelo pacote (SPEC-052); desempenho; ausência de `android.*` no coletor (inspeção, AC-017); conteúdo de `KnownWallets` (SPEC-015).

## Padrões seguidos (pattern analysis)
- **`InstallSourceCollectorTest` e `BankLookalikeCollectorTest`:** `runTest`, JUnit 4, `anInstalledApp`, constantes num `private companion object`, "later change to given set", "yields only sN for app with other risk traits".
- **`PackageManagerAppInventoryTest` (SPEC-011, 013 e 014):** `install`, `installService`, `aPackageInfo`, `aService`; `associate`/`associateBy` por pacote; literal no lugar da constante de produção (`enabled_accessibility_services`, aqui `payment`).
- **`InstalledAppTest` (SPEC-013 e 014):** o campo novo entra nos três testes existentes (lido de volta, cópia, cast).
- `skills/android/testing.md` › Adapter Tests — Robolectric (cinco APIs).

## Unit tests

### Coletor de S7 — `core-detection/src/test/.../signal/DefaultPaymentAppCollectorTest.kt` (JUnit 4)
Lista escrita à mão: `com.wallet.one`, `com.wallet.two`.

| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | default-outside-list-yields-s7 | `com.scam.app` padrão → `{UNKNOWN_DEFAULT_PAYMENT_APP}` | AC-001 |
| 2 | default-in-list-yields-nothing | `com.wallet.one` padrão → vazio | AC-002 |
| 3 | app-that-is-not-default-yields-nothing | HCE sem ser o padrão → vazio | AC-003 |
| 4 | package-differing-by-case-yields-s7 | `COM.WALLET.ONE` e `com.Wallet.one` padrão → S7 | AC-004 |
| 5 | later-change-to-given-set-does-not-affect-collector | `com.scam.app` posto no conjunto original depois → ainda S7 | AC-005 |
| 6 | empty-list-yields-s7-for-every-default-app | lista vazia, `com.wallet.one` padrão → S7 | AC-005 |
| 7 | yields-only-s7-for-app-with-other-risk-traits | NFC, sobreposição, SMS, HCE, acessibilidade, instalador nulo e padrão → exatamente S7 | AC-006 |

### Modelo — `core-domain/src/test/.../inventory/InstalledAppTest.kt` (JUnit 5)
| # | Scenario | Expected result | AC |
|---|----------|-----------------|----|
| 1 | fields are read back including default payment services (estendido) | `defaultPaymentServices` lido de volta | AC-016 |
| 2 | mutating source collections does not change the model (estendido) | limpar o `MutableSet` original não muda o campo | AC-016 |
| 3 | exposed collections reject add remove and clear through mutable set cast (estendido) | `UnsupportedOperationException` no campo novo | AC-016 |

## Integration tests (Robolectric)

### Inventário — `core-detection/src/test/.../inventory/PackageManagerAppInventoryTest.kt`
`@Config(sdk = [26, 28, 30, 33, 36])` da classe; `@Config(shadows = [...])` no método dos cenários com shadow próprio.

Ajudantes novos:
- `enableCardEmulation(hostCardEmulation = true)`: features de NFC e HCE e `ShadowNfcAdapter.setNfcHardwareExists(true)`;
- `installHceService(pacote, classe, enabled, exported)`: serviço com `BIND_NFC_SERVICE` e o filtro de HCE;
- `setDefaultPaymentService(pacote, classe)`: `ShadowCardEmulation.setDefaultServiceForCategory` com `"payment"`.

Shadows (`CardEmulationShadows.kt`): `DeadNfcServiceCardEmulationShadow` (`RemoteException(...).rethrowAsRuntimeException()`), `MissingNfcPermissionCardEmulationShadow` (`SecurityException`), `WalletRoleCardEmulationShadow` (`true` para todo serviço do `HOLDER_PACKAGE` em `payment`).

| # | Scenario | Setup | Expected result | AC |
|---|----------|-------|-----------------|----|
| 1 | marks-default-payment-service-of-one-user-app | dois apps com HCE; o serviço de um é o padrão | só ele tem o campo; `cardEmulationServices` igual | AC-007 |
| 2 | default-outside-queried-services-marks-no-app | padrão num app do sistema; depois numa classe com `BIND_NFC_SERVICE` sem o filtro | campo vazio; o app do sistema não aparece | AC-008 |
| 3 | wallet-role-marks-every-service-of-the-holder | `WalletRoleCardEmulationShadow`; dono com dois serviços | as duas classes no dono; o outro app vazio | AC-009 |
| 4 | disabled-not-exported-or-disabled-app-service-is-not-asked | desligado em tempo de execução, desligado no manifesto, não exportado e app desligado, cada um definido por vez como padrão | campo vazio em todos; `cardEmulationServices` com os quatro e o do app desligado | AC-010 |
| 5 | service-enabled-at-runtime-is-asked | desligado no manifesto e ligado com `setComponentEnabledSetting`; padrão | o campo tem a classe | AC-011 |
| 6 | without-nfc-every-app-has-no-default-payment-service | sem features; padrão definido | campo vazio nos dois; `cardEmulationServices` preenchido | AC-012 |
| 7 | nfc-without-hce-every-app-has-no-default-payment-service | só `FEATURE_NFC` | campo vazio, sem exceção | AC-013 |
| 8 | dead-nfc-service-leaves-default-payment-empty-and-keeps-other-fields | `DeadNfcServiceCardEmulationShadow` | campo vazio nos dois; HCE e permissões preenchidos | AC-014 |
| 9 | missing-nfc-permission-propagates-security-exception | `MissingNfcPermissionCardEmulationShadow` | `SecurityException` | AC-015 |
| 10 | returns-empty-sets-without-permissions-or-services (estendido) | app sem nada | `defaultPaymentServices` vazio | AC-012 |

## Resultado (2026-11-24, local, Windows)
- [x] `DefaultPaymentAppCollectorTest`: 7 de 7.
- [x] `InstalledAppTest`: 3 de 3 (estendidos).
- [x] `PackageManagerAppInventoryTest`: 175 de 175 (35 cenários × 5 APIs; eram 130).
- [x] Detekt e ktlint do `core-detection` e do `core-domain` sem achados; o `LargeClass` não acusou, e os testes do S7 ficaram na classe atual (o plano B da aprovação não foi preciso).
