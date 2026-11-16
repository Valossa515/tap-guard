# Política de segurança

O Tap Guard é um app de segurança. Uma falha nele pode dar ao usuário uma confiança que ele não deveria ter, por isso relatos são bem-vindos e tratados com prioridade.

## Como relatar uma vulnerabilidade

Não abra uma issue pública. Use o relato privado do GitHub: na aba **Security** do repositório, clique em **Report a vulnerability**.

Inclua, se possível:

- o que acontece e o que deveria acontecer;
- os passos para reproduzir;
- a versão do app e a versão do Android;
- o impacto que você enxerga.

## O que conta como vulnerabilidade

- O app gravar, logar ou expor dados de cartão.
- O app enviar qualquer dado para fora do aparelho.
- Um jeito de fazer o app classificar como seguro um app com perfil de golpe por causa de um defeito, e não de um limite conhecido da detecção.
- Um jeito de outro app manipular o resultado da varredura, as notificações ou a lista de apps confiáveis.

## O que não conta

- Limites já documentados na especificação: o Android não permite observar ou bloquear o uso de NFC por outro app, e a detecção é por perfil do app instalado.
- Problemas que exigem aparelho com root ou já comprometido.

Para esses casos, uma issue comum é o caminho certo.

## Amostras e testes

Não envie malware real nem dados reais de cartão, nem em relato privado. Descreva o comportamento ou use o `decoy-app` do repositório para demonstrar.

## Versões com suporte

Enquanto não houver uma versão 1.0, só a branch `main` recebe correções.
