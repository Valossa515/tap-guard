# Runbook

> Sem código ainda. Comandos dependem do build Gradle que a SPEC-000 a SPEC-004 (Etapa 0 — Fundação) vão criar.

## Prerequisites
- Android SDK com API 26+ e o SDK alvo exigido pela Play Store na data da entrega.
- JDK / versão do Gradle / Android Gradle Plugin: TODO: no code yet.
- `adb`, para instalar o `decoy-app` (simula origem fora da loja).
- Emulador Android (CI e testes ponta a ponta).
- Aparelho físico com NFC e um cartão Visa ou Mastercard por aproximação, para o teste manual do F2.

## How to Run
```bash
# instalar o app em debug
TODO: no code yet

# instalar o app isca (uma das variantes: nfcOnly, hce, full, lookalike)
TODO: no code yet  # a especificação só define que a instalação é por `adb install`
```

## How to Test
```bash
# unitários (core-domain, JUnit 5)
TODO: no code yet

# Robolectric (módulos Android, JUnit 4)
TODO: no code yet

# instrumentados / Compose UI / ponta a ponta
TODO: no code yet

# lint
TODO: no code yet  # ktlint + detekt
```

Verificações automáticas de privacidade **(planned)**:
- Teste que falha se o manifesto final declarar `INTERNET`.
- Teste que procura o número completo do cartão sintético em banco, preferências e logs depois de uma leitura.

## Build and Release
```bash
# build debug (artefato publicado pelo CI — objetivo da Etapa 0)
TODO: no code yet

# release 1.0 (Etapa 7)
TODO: no code yet
```
- Migrações Room: schema exportado em `core-data/schemas` e versionado. Cada mudança de versão traz uma `Migration` e um teste de migração. `fallbackToDestructiveMigration()` é proibido em release. Comando para rodar os testes de migração: TODO: no code yet.
- Distribuição: Play Store (exige declaração de `QUERY_ALL_PACKAGES` como app de segurança) e GitHub enquanto isso. Publicar na Play Store já na 1.0 é decisão em aberto.

## Troubleshooting
Problemas previstos na especificação (seção 13):
- **Vigia atrasa ou não roda** -> fabricantes matam tarefas em segundo plano; a varredura também roda ao abrir o app; orientar o usuário sobre economia de bateria nas configurações.
- **S7 / RF-303 falham em versão nova do Android** -> API de pagamento padrão mudou (Android 15+: papel de carteira); coletor isolado deve degradar sem erro; testar em Android 8, 12, 14 e na versão mais recente.
- **Receptor de instalação não dispara** -> receptor no manifesto não é confiável desde o Android 8; a detecção confiável é a varredura periódica.
- Erros de build/ambiente: TODO: no code yet.
