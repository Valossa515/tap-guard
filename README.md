# Tap Guard

App Android que detecta apps com perfil de golpe de retransmissão de cartão por NFC e ensina o usuário a se proteger.

> **Estado:** em desenvolvimento, Etapa 0 (fundação). Ainda não há versão instalável.

## O problema

Golpistas se passam por banco, convencem a vítima a instalar um app fora da loja oficial e pedem para ela encostar o cartão no celular e digitar a senha. O app lê o cartão por NFC e retransmite para um comparsa, que paga ou saca em outro lugar.

## O que o app faz

- **Varredura:** analisa os apps instalados e classifica cada um em verde, amarelo ou vermelho.
- **Teste do cartão:** mostra, com o cartão do próprio usuário, o que sai por aproximação.
- **Vigia:** avisa quando surge um app de risco ou quando o app de pagamento padrão muda.
- **Emergência:** passo a passo para quem acha que caiu no golpe.

## Privacidade

Tudo roda no aparelho. O app não tem servidor, não pede cadastro e não declara a permissão de internet, então nada sai do celular. Dados de cartão nunca são gravados.

## Limites

O Android não permite que um app observe ou bloqueie o uso de NFC por outro app. Por isso o Tap Guard trabalha com o perfil dos apps instalados e com educação, e não substitui antivírus nem o antifraude do banco.

## Documentação

- [Especificação do MVP](docs/especificacao.md): modelo de ameaça, requisitos, regras de score, arquitetura e plano.
- [Issues](../../issues): uma por spec, agrupadas em milestones por etapa.

## Como contribuir

Leia o [guia de contribuição](CONTRIBUTING.md) e o [código de conduta](CODE_OF_CONDUCT.md). Para relatar uma falha de segurança, siga a [política de segurança](SECURITY.md).

## Licença

[Apache License 2.0](LICENSE).
