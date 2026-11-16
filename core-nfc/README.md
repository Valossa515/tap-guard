# core-nfc

Biblioteca Android com a leitura EMV por aproximação (modo leitor NFC) e o parser TLV. Implementa `CardReader` do `core-domain`.

> **Estado (SPEC-001):** só a configuração do módulo e um teste de fumaça. Nenhuma funcionalidade ainda.

## Dependências

- **Módulos:** `core-domain`.
- **Plugins:** `com.android.library` (Kotlin embutido do AGP 9), ktlint e detekt.
- **Testes:** JUnit 4.
- **Namespace:** `io.github.valossa515.tapguard.core.nfc` · minSdk 26 · compileSdk 36.

## Regras

- O número do cartão é mascarado aqui dentro: quem usa o módulo só recebe os 4 últimos dígitos.
- Dados de cartão ficam só em memória. Nunca são gravados, logados ou exportados.
- Testes usam apenas vetores TLV sintéticos, nunca dados reais de cartão.

## Comandos

```bash
./gradlew :core-nfc:build
./gradlew :core-nfc:test
```
