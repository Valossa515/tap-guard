# Plan: SPEC-015 Listas de apoio e imitação (S9)

> Issue #11 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-011 (#7, fechada) · Especificação RF-102, §5 (S9 e listas de apoio), §8 (linha S9) e §11 (variante `lookalike`) · ADR-017 (formato da lista de lojas e limitações) · Issue #47 (Pix por aproximação)
> Spec: `.agent/specs/features/spec-015-listas-imitacao.spec.toon` (FT-015 v1.3, `interface: library`, `estimate-lines: 970`, três batches) · Aceite: `.agent/specs/acceptance/spec-015-listas-imitacao.acceptance.toon` (v1.1)
> Arquitetura: `.agent/plans/spec-015-listas-imitacao-architecture.md` · Contrato: `.agent/specs/contracts/spec-015-listas-imitacao.contract.toon` (v1.1)
> Branches: `spec-015-listas-imitacao` (BATCH-01), `spec-015-listas-imitacao-batch-02` e `spec-015-listas-imitacao-batch-03`, cada uma a partir da `main` depois do merge do batch anterior · um PR por batch; só o do BATCH-03 leva `Closes #11`
> Planejado em 2026-11-21. A #11 foi movida para "Em andamento" no Project 2 antes do planejamento. Plano aprovado pelo usuário em 2026-11-21, com os acréscimos da seção "Pedidos do usuário na aprovação".
> Nomes finais pela revisão do architect: `KnownWallets`, `KnownBankApp`, `KnownBanks.apps`, `BankNameMatcher` (`internal`) e `BankLookalikeCollector`. O texto abaixo já usa esses nomes.

## Summary
Três listas públicas e um coletor no `core-detection`, pacote `core.detection.signal`, no formato do `TrustedStores` (ADR-017):
- **`KnownWallets`:** pacotes de apps que podem ser o app de pagamento por aproximação padrão sem gerar S7. Inclui a Carteira do Google, a Samsung Wallet e os apps de banco que fazem Pix por aproximação no próprio app (Nubank, Itaú, BB e Inter), como pedem a #11 e a #47. O consumidor é o coletor de S7 (SPEC-016).
- **`KnownBanks`:** nome de exibição e pacote oficial dos apps dos 14 maiores bancos e fintechs do Brasil por número de clientes (BCB, dez/2025), com os apps irmãos oficiais (Empresas, Cartões, Personnalité, CAIXA Tem etc.).
- **Coletor de S9 (`BankLookalikeCollector`):** compara o rótulo normalizado do app com os nomes da lista de bancos. Devolve `BANK_LOOKALIKE` quando o nome casa e o pacote do app não é nenhum dos pacotes oficiais da lista.

O BATCH-01 entrega as listas, o BATCH-02 a comparação de nomes (`BankNameMatcher`) e o BATCH-03 o coletor de S9.

## Decisões do usuário (2026-11-21)

| Item | Decisão |
|------|---------|
| Corte da lista de bancos | Os 14 primeiros do ranking de clientes do BCB (dez/2025), todos acima de 25 milhões de clientes, mais os apps irmãos oficiais achados na Play. Pan, Sicoob, Sicredi, BV, Banrisul e Safra ficam para depois, com o pacote já conferido (abaixo) |
| Lista de carteiras | Só os apps com fonte de que fazem pagamento por aproximação: Carteira do Google, Samsung Wallet, Nubank, Itaú, BB e Inter (6 pacotes) |
| Alcance do S9 | Nome inteiro: igualdade depois de normalizar e de tirar prefixos e sufixos de uma lista fechada, com tolerância de letras. "Contém o nome do banco" fica fora |
| Nomes da lista | Nome da marca tirado do título da Play (parte antes de `:`) ou do nome do desenvolvedor, com a fonte no comentário. A conferência do rótulo real no aparelho fica para o teste ponta a ponta (SPEC-022, #17) |
| Batches | Dois no plano: listas (BATCH-01) e S9 (BATCH-02). Na revisão do architect, o BATCH-02 passou de 500 e foi dividido em normalização (BATCH-02) e coletor (BATCH-03), como o usuário pediu |

## Gap analysis
- **Bloqueantes (resolvidas pelas quatro decisões acima):** critério de corte dos bancos; quais apps são carteiras; alcance da comparação; fonte do nome de exibição.
- **Não bloqueantes (premissas, vão para `assumptions:` da spec):**
  - o serviço de pagamento de um app de banco fica no próprio pacote do app (o S7 compara o pacote do serviço padrão). A SPEC-016 confirma;
  - o `InstalledApp.label` é o rótulo do `loadLabel` (SPEC-011). Sem rótulo, o Android devolve o nome do pacote, que não casa com nenhum nome da lista;
  - a lista de prefixos e sufixos é fechada e está no plano. Os que você citou: "Token", "Empresas", "Cartões". Os demais são proposta minha (marcados abaixo).

## Pesquisa

### Pix por aproximação: quais apps de banco viram o pagamento por aproximação
| Instituição | Pelo próprio app | Só pela Carteira do Google | Fonte |
|---|---|---|---|
| Nubank | sim ("é possível definir o Nubank como carteira padrão do dispositivo") | também | TechTudo, 07/04/2026; TechTudo e BPMoney, 03/08/2026 |
| Itaú | sim ("lançou o Pix por aproximação dentro do seu aplicativo"; só Android) | também | Mobile Time, 27/01/2026; TechTudo, 03/08/2026 |
| Banco do Brasil | sim ("entrar nas configurações do celular, abrir 'NFC e pagamentos sem contato' e escolher o Serviço Pix por Aproximação") | também | TechTudo e BPMoney, 03/08/2026 |
| Inter | sim ("pelo Super App: basta ter a versão 26.1.1 ou mais recente") | também | TechTudo e BPMoney, 03/08/2026 |
| Bradesco, Caixa, Santander, C6, PicPay | não | sim | TechTudo e BPMoney, 03/08/2026 |
| Mercado Pago | não | sim ("só pode ser realizado pela Carteira do Google") | blog do Mercado Pago |

- O BB é o único caso em que a fonte descreve a escolha do serviço nas configurações de NFC, que é o pagamento padrão (`CATEGORY_PAYMENT`). Nubank fala em "carteira padrão" como opção. Itaú e Inter falam em pagar pelo app, sem dizer se ele vira o padrão ou só o preferido com o app aberto. Nos dois casos, pôr o pacote na lista não tem custo: se ele nunca for o padrão, a entrada não é usada.
- O PagBank "Tap On" é maquininha no celular: lê cartões, não emula cartão. Não é carteira.

### Ranking de clientes (BCB, dez/2025), base do corte
Caixa 158,0 mi · Nubank 112,0 mi · Bradesco 110,4 mi · Itaú 100,3 mi · Banco do Brasil 81,9 mi · Santander 70,6 mi · Mercado Pago 68,8 mi · PicPay 66,9 mi · Inter 41,9 mi · PagSeguro (PagBank) 33,9 mi · C6 Bank 33,4 mi · BTG 26,7 mi · 99Pay 26,1 mi · Neon 26,0 mi.
- Fontes: Melhores Cartões (24/01/2026, cita o ranking do BCB, dados de dez/2025) e Forbes Brasil (22/01/2026), que confirma os cinco primeiros.
- As posições 15 a 18 não estão nas fontes. O Will Bank (19º) teve a liquidação decretada pelo BCB e fica fora.

### Pacotes conferidos na Play
Todos os pacotes abaixo foram vistos numa página da Play (`play.google.com/store/apps/details?id=…`, `hl=pt_BR&gl=BR`), com título e, quando indicado, desenvolvedor. Cinco palpites voltaram 404 e **não** entram: `com.itau.iti`, `com.itau.personnalite`, `com.itaucard`, `com.bradesco.cartoes` e `com.santander.empresas`. Não foi achado app separado para iti, Itaucard, BB Empresas e Nubank Empresas.

## Formato das listas
Seguem o `TrustedStores` (ADR-017, comentário da #11):
- um `object` público por tipo, num arquivo próprio, no pacote `core.detection.signal`;
- a coleção pública não pode ser alterada, nem por cast (`Collections.unmodifiableSet(linkedSetOf(...))`);
- um comentário por entrada com o nome e a fonte (em `KnownBanks`, um por instituição, com o título de cada app);
- quem usa o SDK monta o próprio conjunto a partir da lista padrão (`+` ou `-`) e o passa ao coletor.

**Carteiras não precisam de tipo próprio.** O S7 compara o pacote do serviço padrão, então `KnownWallets.packageNames: Set<String>`, igual ao `TrustedStores`.

**Bancos precisam de tipo com nome de exibição e pacote**, porque o S9 compara nomes (nomes decididos na revisão do architect):
- `KnownBankApp(displayName: String, packageName: String)`: `data class` pública no `core-detection`, sem coleção dentro. O `core-domain` não conhece o tipo;
- `KnownBanks.apps: Set<KnownBankApp>`;
- **uma entrada por par (nome, pacote).** Um pacote pode aparecer em mais de uma entrada (apelido): BB tem "BB" e "Banco do Brasil", e o app do Santander tem "Santander Brasil" (título da Play) e "Santander" (marca). Um banco com vários apps tem uma entrada por app (Itaú, Itaú Empresas, Personnalité). Assim o S9 não precisa de tipo com lista de nomes, e o teste compara a lista inteira com uma escrita à mão;
- nome ou pacote em branco lança `IllegalArgumentException` (mantido pelo architect: falha cedo para quem usa o SDK).

## Lista de carteiras (`KnownWallets.packageNames`, 6 pacotes)

| App | Pacote | Fonte |
|---|---|---|
| Carteira do Google | `com.google.android.apps.walletnfcrel` | Play ("Carteira do Google"); especificação §5 |
| Samsung Wallet | `com.samsung.android.spay` | Play ("Samsung Wallet (Samsung Pay)"); especificação §5; citada para Pix por aproximação em Galaxy (TechTudo e BPMoney, 03/08/2026) |
| Nubank | `com.nu.production` | Play; TechTudo, 07/04/2026 (carteira padrão) |
| Itaú | `com.itau` | Play; Mobile Time, 27/01/2026 |
| Banco do Brasil | `br.com.bb.android` | Play; TechTudo e BPMoney, 03/08/2026 (serviço escolhido em NFC) |
| Inter | `br.com.intermedium` | Play; TechTudo e BPMoney, 03/08/2026 (Super App 26.1.1+) |

Ficam fora, por ora: Mercado Pago, PicPay, Bradesco, Caixa, Santander e C6, que fazem Pix por aproximação pela Carteira do Google. Se um deles lançar no próprio app, o S7 acusa o app até a lista ser atualizada (risco registrado abaixo).

## Lista de bancos (`KnownBanks.apps`, 29 entradas, 26 pacotes)

Critério de corte: os 14 primeiros do ranking de clientes do BCB (dez/2025), mais os apps oficiais dessas instituições com o nome da marca no título, achados na busca da Play com o desenvolvedor do banco. O nome é o do título da Play antes de `:` (ou do desenvolvedor, quando indicado), escrito sem os prefixos e sufixos que o algoritmo já tira.

| # | Instituição | Nome de exibição | Pacote | Fonte (Play: título · desenvolvedor) |
|---|---|---|---|---|
| 1 | Caixa | CAIXA | `br.com.gabba.Caixa` | "CAIXA" |
| 2 | Caixa | CAIXA Tem | `br.gov.caixa.tem` | "CAIXA Tem" |
| 3 | Caixa | Cartões CAIXA | `br.gov.caixa.cartoes` | "Cartões CAIXA" · Caixa Econômica Federal |
| 4 | Nubank | Nubank | `com.nu.production` | "Nubank: conta, cartão e mais" |
| 5 | Bradesco | Bradesco | `com.bradesco` | "Bradesco: Conta, Cartão e Pix!" |
| 6 | Bradesco | Bradesco Cartões | `br.com.bradesco.cartoes` | "Bradesco Cartões: Faturas e +" · Banco Bradesco SA |
| 7 | Bradesco | Bradesco Empresas | `br.com.bradesco.pdpjapp` | "Bradesco Empresas: Conta PJ" · Banco Bradesco SA |
| 8 | Bradesco | Bradesco Net Empresa | `br.com.bradesco.netempresa` | "Bradesco Net Empresa: Conta PJ" · Banco Bradesco SA |
| 9 | Bradesco | next | `br.com.bradesco.next` | "next: Conta Digital e Cartão" |
| 10 | Itaú | Itaú | `com.itau` | "Banco Itaú: Conta, Cartão e +" · Itaú Unibanco S. A. |
| 11 | Itaú | Itaú Empresas | `com.itau.empresas` | "Itaú Empresas: Conta PJ" |
| 12 | Itaú | Personnalité | `com.itau.pers` | "Personnalité: banco e cartão" · Itaú Unibanco S. A. |
| 13 | Banco do Brasil | BB | `br.com.bb.android` | "BB: Banco, Conta, Pix, Cartão" |
| 14 | Banco do Brasil | Banco do Brasil | `br.com.bb.android` | desenvolvedor Banco do Brasil SA (apelido) |
| 15 | Santander | Santander Brasil | `com.santander.app` | "Santander Brasil" · Banco Santander (Brasil) S.A. |
| 16 | Santander | Santander | `com.santander.app` | desenvolvedor (apelido) |
| 17 | Santander | Santander Empresas | `com.santandermovelempresarial.app` | "Santander Empresas" · Banco Santander (Brasil) S.A. |
| 18 | Mercado Pago | Mercado Pago | `com.mercadopago.wallet` | "Mercado Pago: Banco Digital" · Mercado Libre |
| 19 | PicPay | PicPay | `com.picpay` | "Banco PicPay: Cartão, Pix e +" |
| 20 | Inter | Inter | `br.com.intermedium` | "Inter: Conta, Cartão e Pix" |
| 21 | Inter | Inter Empresas | `br.com.Inter.CDPro` | "Inter Empresas: Conta PJ" · Banco Inter SA (pacote com `I` maiúsculo) |
| 22 | PagBank | PagBank | `br.com.uol.ps.myaccount` | "Banco PagBank: Conta, Tap On +" · PagBank |
| 23 | C6 Bank | C6 Bank | `com.c6bank.app` | "C6 Bank: Cartão, Conta e Mais!" |
| 24 | BTG Pactual | BTG Pactual Banking | `com.btg.pactual.banking` | "BTG Pactual Banking" · Banco BTG Pactual S.A. |
| 25 | BTG Pactual | BTG Pactual | `com.btg.pactual.banking` | desenvolvedor (apelido) |
| 26 | BTG Pactual | BTG Pactual Empresas | `com.btg.business` | "BTG Pactual Empresas: Conta PJ" · Banco BTG Pactual S.A. |
| 27 | BTG Pactual | BTG Pactual Investimentos | `com.btg.pactual.digital.mobile` | "BTG Pactual Investimentos" · Banco BTG Pactual S.A. |
| 28 | 99Pay | 99Pay | `com.pay99.wallet` | "99Pay" · 99 Tecnologia LTDA |
| 29 | Neon | Neon | `br.com.neon` | "Neon cartão de crédito e conta" |

**Ficam para depois, com pacote já conferido na Play:** Banco Pan `br.com.bancopan.cartoes`, Sicoob `br.com.sicoobnet`, Sicredi `br.com.sicredimobi.smart` (e "Sicredi X" `br.com.sicredi.app`), Banrisul `br.com.banrisul`, Safra `br.livetouch.safra.net`, BV `com.votorantim.bvpd`. Sem fonte: Banco Original (não achado na busca).

## S9: algoritmo de comparação

O coletor recebe a lista inteira na construção (parâmetro obrigatório, sem valor padrão, conjunto copiado, como o S1). Na construção, ele calcula o esqueleto de cada nome da lista e o conjunto de todos os pacotes da lista. Para cada app:

1. **Pacote na lista → sem S9.** Se `app.packageName` é um dos pacotes da lista (qualquer banco), o coletor devolve vazio. Comparação exata, com maiúsculas, como no S1. Usar a união dos pacotes, e não só o pacote do banco que casou, evita marcar um app oficial cujo nome lembre outro app do mesmo banco ("Itaú Empresas" casa com "Itaú" depois de tirar o sufixo). O protótipo mostrou que, na lista acima, só nomes do mesmo banco casam entre si.
2. **Esqueleto do rótulo** (o mesmo para o rótulo do app e para os nomes da lista):
   1. corta no primeiro separador: `:`, `|`, ` - `, ` – `, ` — ` (o imitador que copia o título da Play, "Nubank: conta, cartão e mais", vira "Nubank");
   2. NFKC (letras de largura cheia, ligaduras) e minúsculas com `Locale.ROOT`;
   3. NFD e remoção das marcas combinantes (acentos): `Itáú` → `itau`. Proposta: filtrar pelo tipo do caractere (`Character.NON_SPACING_MARK`) em vez de regex, para não depender da diferença entre o regex da JVM e o do Android (ICU);
   4. troca de letras parecidas por uma letra só:
      - dígitos e símbolos: `0→o`, `1→i`, `3→e`, `4→a`, `5→s`, `7→t`, `@→a`, `$→s`, `!→i`;
      - `l→i` (o `l` minúsculo e o `I` maiúsculo são iguais em muitas fontes; depois das minúsculas, `I` já virou `i`);
      - cirílico e grego que imitam letras latinas (`а е о р с у х і ӏ ѕ ј к м н т в`, `α ο ρ ι κ ν τ`);
      - pares: `rn→m`, `vv→w`;
   5. fica só com `[a-z0-9]`: espaços, pontuação, `&`, emojis e caracteres invisíveis (espaço de largura zero) saem.
3. **Variantes do esqueleto:** o próprio esqueleto; sem um prefixo da lista; e sem sufixos da lista, repetidamente ("Itaú Cartões Empresas" → `itau`). Como trabalha sobre o esqueleto colado, cobre também "ItauToken". Um prefixo ou sufixo só sai se sobrar algo.
   - **Prefixos:** `banco`, `meu`, `app` (proposta minha).
   - **Sufixos:** `token`, `empresas`, `cartoes` (os seus); `empresa`, `cartao`, `pj`, `app`, `mobile`, `oficial`, `digital`, `seguranca` (proposta minha; "segurança" aparece em nomes de golpe como "Itaú Segurança").
4. **Casa** se alguma variante fica a uma distância de Damerau-Levenshtein (versão OSA: inserção, remoção, troca e transposição de letras vizinhas) dentro do limite do nome da lista:
   - esqueleto com até 5 letras: 0 (só igual): `itau`, `caixa`, `bb`, `inter`, `neon`, `next`, `99pay`;
   - de 6 a 9 letras: até 1: `nubank`, `bradesco`, `santander`, `picpay`;
   - 10 letras ou mais: até 2: `bancodobrasii`, `mercadopago`.
   - Nomes curtos não têm tolerância porque uma letra a mais ou a menos já forma outra palavra: "Baixa" não pode casar com "Caixa".
5. Esqueleto vazio (rótulo só com emoji ou pontuação) nunca casa.
6. Devolve `{BANK_LOOKALIKE}` ou vazio, nunca outro sinal. Não chama o Android: `java.text.Normalizer` é do JDK e existe no Android desde a API 1. Nada de `android.*`.

**Custo:** cerca de 30 esqueletos calculados uma vez; por app, até uns 10 variantes contra 29 nomes curtos. Muito abaixo do limite de 5 s para 300 apps (RNF-03).

### Tabela de exemplos (resultado do protótipo)
Rodado com a lista acima, no protótipo descrito em "Experimento". "Pacote falso" é qualquer pacote fora da lista.

| Rótulo do app | Pacote | Esperado | Por quê |
|---|---|---|---|
| **Apps verdadeiros** | | | |
| Nubank | `com.nu.production` | sem S9 | pacote na lista |
| Itaú Empresas | `com.itau.empresas` | sem S9 | pacote na lista (o nome casaria com "Itaú") |
| Caixa | `br.com.gabba.Caixa` | sem S9 | pacote na lista |
| Santander Empresas | `com.santandermovelempresarial.app` | sem S9 | pacote na lista |
| **Imitadores óbvios** | | | |
| Nubank | pacote falso | S9 | igual |
| NUBANK␠ / Nu Bank | pacote falso | S9 | maiúsculas e espaços saem |
| Nubank: conta, cartão e mais | pacote falso | S9 | corte no `:` |
| Nubаnk (`а` cirílico) | pacote falso | S9 | letra parecida |
| Ｎｕｂａｎｋ (largura cheia) | pacote falso | S9 | NFKC |
| Nubnak | pacote falso | S9 | transposição, distância 1 |
| Nubamk | pacote falso | S9 | troca de letra, distância 1 |
| Nubank | `com.nu.production.fake` | S9 | o pacote é comparado exato |
| Itau / Itáú / 1taú / Ltaú | pacote falso | S9 | acento e letras parecidas |
| Itaú Token / ItauToken / Itaú \| Token | pacote falso | S9 | sufixo, colado ou depois de separador |
| Banco Itaú / Itaú Cartões Empresas | pacote falso | S9 | prefixo; dois sufixos |
| Itaú + espaço de largura zero | pacote falso | S9 | caractere invisível sai |
| Bradesc0 / Brandesco / Bradesco App / Meu Bradesco | pacote falso | S9 | `0→o`; distância 1; sufixo; prefixo |
| Calxa | pacote falso | S9 | `l→i` |
| Santandr | pacote falso | S9 | distância 1 |
| PicPay Segurança / Pic Pay / PicPey | pacote falso | S9 | sufixo proposto; espaço; distância 1 |
| Mercad0 Pag0 | pacote falso | S9 | `0→o` |
| Banco Brasil | pacote falso | S9 | distância 2 de `bancodobrasii` (13 letras) |
| Banco PagBank: Conta, Tap On + | pacote falso | S9 | corte no `:` e prefixo |
| C6 Bank: Cartão, Conta e Mais! | pacote falso | S9 | corte no `:` |
| **Apps legítimos com nome parecido** | | | |
| Baixa | outro | sem S9 | `caixa` tem 5 letras: só igual |
| Caixinha | outro | sem S9 | não é igual nem está a distância 0 |
| Caixa de Ferramentas | outro | sem S9 | nome inteiro, sem "contém" |
| Next Launcher | outro | sem S9 | nome inteiro |
| Neon Light | outro | sem S9 | nome inteiro |
| Itaú Shop | outro | sem S9 | "shop" não é sufixo da lista |
| Mercado Livre | outro | sem S9 | distância 5 de `mercadopago` |
| Bradesco Seguros | outro | sem S9 | "seguros" não é sufixo da lista |
| C6 | outro | sem S9 | `c6` não é `c6bank` |
| Banco Pan | qualquer | sem S9 | Pan está fora da lista pelo corte |
| **Falsos positivos aceitos** | | | |
| Inter (app de outro desenvolvedor, por exemplo de um clube) | outro | S9 | nome curto igual ao do banco |
| NEXT (loja de roupas, pacote ilustrativo) | outro | S9 | nome curto igual ao do banco |
| Next - Launcher (outro desenvolvedor) | outro | S9 | o corte no ` - ` deixa "Next" |
| Caixa – Organizador (outro desenvolvedor) | outro | S9 | o corte no ` – ` deixa "Caixa" |
| **Imitações que escapam (limitação)** | | | |
| Atualização Nubank / Nubank Wallpapers | pacote falso | sem S9 | palavras fora da lista fechada (decisão "nome inteiro") |
| Inter&Co / Neon Pay | pacote falso | sem S9 | "co" e "pay" não são sufixos da lista |
| Nubank | `com.nu.production` emprestado, sem o Nubank no aparelho | sem S9 | empréstimo do nome de pacote (abaixo) |

O corte no separador existe para pegar o imitador que copia o título da Play ("Nubank: conta, cartão e mais"). O custo é o falso positivo de um app legítimo cujo nome começa com o nome de um banco seguido de separador. O mesmo vale para "Neon | Wallpapers" e "Inter: Notícias do clube" (conferido no protótipo). Os dois casos da tabela viram testes que documentam o comportamento e entram no ADR-020.

Um falso positivo do S9 custa pouco: um app de loja confiável com S9 perde o teto de 29 (regra 3) e pode ficar amarelo, mas não vermelho, porque o vermelho exige S1 (regra 4). A pessoa pode marcá-lo como confiável (regra 2).

### Limitação: nome de pacote emprestado (ADR-017)
O nome de pacote é único no aparelho, não no mundo. Num aparelho sem o Nubank, um app de golpe pode usar `com.nu.production` e o nome "Nubank" e não recebe S9 (passo 1). Análise:
- instalado à mão, ele recebe S1; com NFC e HCE (S2 + S3), fica vermelho (65) sem o S9;
- o S9 deixa de ajudar só quando o app de golpe vem de uma loja confiável, que já é o limite da regra 3;
- conferir o certificado de assinatura do app (SHA-256 por pacote) fecharia a brecha, mas exige ler as assinaturas no inventário e ter uma fonte para o certificado de cada banco. Fica fora desta spec, na issue pós-MVP #65 ("Conferir o certificado de assinatura dos apps de banco e lojas"), que cobre também o S1 (#57).
- O ADR novo registra a limitação junto com a do S1.

## Apps de clonagem: o que esta spec entrega e o que fica para a SPEC-017
O problema (comentário da #11 e ADR-017): um app de banco copiado para um celular novo pelo Smart Switch (`com.sec.android.easyMover`), Mi Mover (`com.miui.huanji`) ou Clone Phone (`com.coloros.backuprestore`) tem o app de clonagem como instalador. Ele recebe S1, e com S2 e S3 fica vermelho (65).

- **Esta spec entrega:** `KnownBanks.apps`, com o pacote oficial de cada app, público e sem alteração possível. Dele sai o conjunto de pacotes (`KnownBanks.apps.map { it.packageName }.toSet()`) que uma regra de ajuste pode usar.
- **Registrado na #13 (comentário de 2026-11-21):** as duas formas abaixo, com a recomendação da estreita, e a lista pública de apps de clonagem que ela exige.
- **Fica para a SPEC-017:**
  - a regra que tira o banco copiado do vermelho e onde ela entra na ordem das regras da §5;
  - se o conjunto de pacotes de banco vai para o `ScoringContext` (como `Set<String>`, para o `KnownBankApp` não entrar no `core-domain`), montado pelo `ScanDevice` (SPEC-018);
  - quanto confiar só no pacote, dada a limitação acima. Duas formas que a SPEC-017 pode avaliar:
    - **larga:** pacote na lista de bancos tem o teto da regra 3, como se viesse de loja. Um app de golpe com pacote emprestado, instalado à mão, ficaria verde: abre a brecha que o S1 fecha hoje;
    - **estreita:** o teto só vale quando o pacote está na lista de bancos **e** o instalador é um app de clonagem conhecido. A brecha passa a exigir que o golpe já estivesse no celular antigo.
    - As duas precisam de uma lista de apps de clonagem, que não existe hoje. Os três pacotes estão no ADR-017, sem uma lista pública. Esta spec não a cria, porque a §5 não pede.
  - a conferência em aparelho real (SPEC-022).

## Onde mora cada coisa
| O quê | Onde | Visibilidade |
|---|---|---|
| `KnownWallets` (pacotes) | `core-detection`, `core.detection.signal` | pública |
| `KnownBankApp` (nome, pacote) e `KnownBanks` | `core-detection`, `core.detection.signal` | pública |
| Coletor de S9 (`BankLookalikeCollector`) | `core-detection`, `core.detection.signal`, sem `android.*`, como os outros (ADR-017) | pública |
| Normalização (`BankNameMatcher.kt`) | `core-detection`, junto do coletor | `internal`: `BankNameMatcher`, `bankNameSkeleton` e `editDistance`; o resto `private` |
| Parâmetro `label` na `anInstalledApp` | `core-domain/src/testFixtures` | fixture de teste |

- **`ScoringContext`:** nenhuma lista entra nesta spec. `KnownWallets` vai para o construtor do coletor de S7 (SPEC-016), não para o score. Os pacotes de banco podem entrar na SPEC-017 (acima).
- **`InstalledApp`:** não muda. O S9 usa `label` e `packageName`, que já existem.
- **Fixture:** `anInstalledApp` ganha `label` (hoje fixo em `"Example"`) e passa a 9 parâmetros. É a exceção do ADR-015 estendida pelo ADR-019 (fixture que repassa à fábrica de uma data class, com valores padrão), sem `@Suppress` novo.
- **App isca `lookalike` (SPEC-021):** a §11 pede um banco fictício "da lista de testes". O parâmetro obrigatório do coletor permite passar `KnownBanks.apps + KnownBankApp("Banco Fictício", …)`. Como o app de verdade usa a lista padrão, a SPEC-021 ou a SPEC-022 decide como o teste ponta a ponta injeta o banco fictício.

## Experimento (projeto descartável no scratchpad, 2026-11-21, JDK 21)
O incerto aqui não era a API do Android (o S9 não usa nenhuma), mas o comportamento do algoritmo. Um protótipo em Java (`S9Proto.java`) implementou os passos acima com a lista de 29 entradas e rodou 80 rótulos (76 no planejamento e 4 de corte no separador na aprovação):
1. **Todos os casos da tabela deram o resultado esperado.** A tabela acima é a saída do protótipo, não uma conta de cabeça.
2. **Nomes da lista não casam entre bancos diferentes.** Os únicos pares que casam são do mesmo banco (Itaú Empresas → Itaú, Bradesco Cartões e Bradesco Empresas → Bradesco, Santander Empresas → Santander, Inter Empresas → Inter, BTG Pactual Empresas → BTG Pactual), e os pacotes deles estão na lista. Vira teste: nenhum nome da lista casa com o de outra instituição.
3. **Achados que mudaram o plano:**
   - o título da Play do Santander é "Santander Brasil"; sem o apelido "Santander", o imitador "Santander" escaparia (distância 6). Por isso, uma entrada por par (nome, pacote), com apelidos;
   - o mesmo para "BTG Pactual Banking" (apelido "BTG Pactual");
   - "Inter&Co" escapa (`interco`), e "NEXT" de uma loja de roupas casa: os dois foram para a tabela como limitação e falso positivo aceito.
4. `Normalizer` (NFKC, NFD) resolveu acentos, largura cheia e ligaduras; os caracteres cirílicos e gregos precisaram do mapa explícito (o NFKC não os converte).

## Approach
- Seguir:
  - o formato do `TrustedStores` e do `InstallSourceCollector` (ADR-017): `object` com conjunto público sem alteração, comentário de fonte por entrada; coletor `class` pública com parâmetro obrigatório e cópia do conjunto;
  - `skills/android/SKILL.md` e `SKILLS.md` › Signal collectors: um coletor por sinal, só o seu sinal ou nada, nada de `android.*`;
  - `skills/android/testing.md` e `SKILLS.md` › Tests of a collector with no Android API: JUnit 4 na JVM, sem Robolectric; casos parametrizados (`Parameterized`, uma classe por grupo) leem listas escritas à mão no teste, nunca a lista de produção;
  - `skills/shared/anti-invention`: nenhum pacote sem fonte; os palpites que deram 404 ficaram fora.
- **Listas:** o teste compara a lista de produção com uma escrita à mão (`ExpectedKnownWallets`, `ExpectedKnownBanks`), como `ExpectedTrustedStores`: tirar ou pôr uma entrada derruba um teste.
- **S9:** os casos da tabela viram testes parametrizados (imitadores, legítimos, falsos positivos aceitos e imitações que escapam), com uma lista de bancos escrita à mão no teste e um banco fictício para mostrar a extensão pelo SDK.

## Pedidos do usuário na aprovação (2026-11-21)
1. **Falso positivo do corte no separador:** "Next - Launcher" e "Caixa – Organizador", de outro desenvolvedor, recebem S9. Estão na tabela de exemplos, no `BankNameMatcherMatchTest` (casos que documentam o comportamento) e no ADR-020.
2. **Issue pós-MVP #65**, "Conferir o certificado de assinatura dos apps de banco e lojas" (labels `spec` e `core-detection`, sem milestone): ler as assinaturas no inventário e comparar com o SHA-256 conhecido de cada pacote, fechando a brecha do pacote emprestado no S1 (#57) e no S9. Cita o ADR-017 e esta spec.
3. **Comentário na #13 (SPEC-017):** as duas formas da regra dos apps de clonagem, com a recomendação da estreita (teto só com pacote de banco e instalador de clonagem conhecido, sem reabrir a brecha do S1), e a lista pública de apps de clonagem que ela exige, no formato das listas desta spec.

## Tasks

### BATCH-01: listas (PR "SPEC-015 BATCH-01 …", `Refs #11`)
1. [x] **Listas** (`feat:`): `KnownWallets` (6 pacotes), `KnownBankApp` (com `require` de nome e pacote não em branco) e `KnownBanks` (29 entradas), cada entrada com nome e fonte em comentário, KDoc com o critério de corte, o que fica fora e como ampliar pelo SDK. `./gradlew build` verde.
2. [x] **Testes das listas** (`test:`, papel tester):
   - listas escritas à mão (`ExpectedKnownWallets`, `ExpectedKnownBankApps`); conteúdo exato das duas listas;
   - cast para `MutableSet` lança `UnsupportedOperationException`;
   - `KnownBankApp` com nome ou pacote vazio ou só com espaços lança `IllegalArgumentException`; preenchidos criam sem erro.
3. [x] **Conferência dos critérios**, local e desfeita sem commit: tirar uma carteira e um app de banco da produção derruba os testes de conteúdo; `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
   - 2026-11-21, local (Windows), cada alteração desfeita sem commit (árvore limpa depois de cada uma), rodando só a classe de teste afetada:
     - `com.itau` tirado de `KnownWallets` → 1 falha: `KnownWalletsTest > default list has exactly the expected wallets` (AC-001).
     - `KnownBankApp("Neon", "br.com.neon")` tirado de `KnownBanks` → 1 falha: `KnownBanksTest > default list has exactly the expected bank apps` (AC-002).
     - `KnownBankApp("Banco PAN", "br.com.bancopan.cartoes")` posto em `KnownBanks` → 1 falha: o mesmo teste (AC-002).
     - `isNotBlank()` trocado por `isNotEmpty()` no `KnownBankApp` → 2 falhas: `blank display name is rejected` e `blank package name is rejected` (AC-004).
     - `./gradlew clean build` verde em 2 min 58 s, sem aviso `w:` do Kotlin e sem diff em `core-data/schemas` (AC-021, AC-022).
       - Testes: `app` 32, `core-data` 17 (2 ignorados no Windows; só rodam no CI), `core-detection` 210 (201 de antes + 9 novos: 2 + 2 + 5), `core-domain` 32, `core-nfc` 1, `decoy-app` 1. Total: 293.
       - `LibraryManifestTest` e `MergedManifestTest` passam (sem permissão nova).
       - `koverVerify` do `core-domain` passa (o módulo não mudou).
       - Lint sem achados em `app`, `core-data`, `core-detection` e `core-nfc`; o único aviso é o `MissingApplicationIcon` do `decoy-app`, que já existia.
   - **Desvios do contrato no BATCH-01:** nenhum nos arquivos. Na tarefa 1, seis comentários de fonte passavam de 120 caracteres (limite do ktlint e do detekt) e foram quebrados em duas linhas antes do primeiro build.
4. [x] **Documentação** (`docs:`):
   - `core-detection/README.md` (listas e como ampliar);
   - §5 de `docs/especificacao.md` (listas de carteiras e bancos com pacotes, critério de corte);
   - `SKILLS.md` (API pública, listas), `project-reference.md`, `context.md` (decisão em aberto "lista inicial de bancos e carteiras" resolvida), `glossary.md` (carteira conhecida, banco conhecido), `backlog_rules.md`;
   - **ADR-020** em `decisions.md`: listas, critério de corte, fontes, o que fica para depois, formato (`KnownBankApp`, apelidos, validação), algoritmo do S9, comportamentos aceitos (inclusive o corte no separador), limitação do pacote emprestado (#65) e regra dos apps de clonagem na SPEC-017 (#13); normalização e coletor marcados para os batches seguintes.

### BATCH-02: comparação de nomes (PR "SPEC-015 BATCH-02 …", `Refs #11`)
5. [x] **Normalização** (`feat:`): `BankNameMatcher.kt` com `internal class BankNameMatcher`, `internal fun bankNameSkeleton` e `internal fun editDistance`; separadores, mapa de letras parecidas, prefixos, sufixos, variantes e limites `private`, com os limites em `const val`. KDoc com o algoritmo e os comportamentos aceitos. `./gradlew build` verde.
   - 2026-11-23, `feat:` `f0ab741`. O protótipo rodado com as 29 entradas finais dá o mesmo resultado da tabela nos 80 rótulos e os mesmos 6 pares do mesmo banco.
   - **Defeito achado nos testes da tarefa 6 e corrigido no `fix:` `8549662`** (decisão do usuário, emenda 1.3 da spec e 1.1 do contrato): os sufixos `mobile`, `oficial` e `digital` nunca saíam, porque as variantes partem do esqueleto, que já trocou `l` por `i`. O protótipo tinha o mesmo defeito, e nenhum caso da tabela usava esses sufixos. Prefixos e sufixos passam pelo `bankNameSkeleton` ao carregar o arquivo, sem vazios nem repetidos, e viram `internal` (`BANK_NAME_PREFIXES`, `BANK_NAME_SUFFIXES`) para o teste que confere cada entrada. A ordem não importa (as variantes tentam todos), então não há ordenação.
   - **Desvio do protótipo, mantido:** ele também trocava `|` por `i`; o BR-006 não pede, e a troca só mudaria um rótulo que começa com `|` (ADR-020).
6. [x] **Testes** (`test:`, papel tester):
   - esqueleto parametrizado: maiúsculas, acentos, espaços, cada separador, separador no início, NFKC, cirílico e grego, dígitos e símbolos, `l`, `rn`/`vv`, caractere invisível, rótulo vazio;
   - distância: igual, troca, inserção, remoção, transposição, vazia, duas edições;
   - "casa" parametrizado: imitadores da tabela e os falsos positivos aceitos (Inter, NEXT, Next - Launcher, Caixa – Organizador);
   - "não casa" parametrizado: legítimos da tabela e as imitações que escapam;
   - limites por tamanho (5/6, 9/10 letras), prefixos e sufixos, esqueleto vazio, lista vazia, e nenhum nome de `KnownBanks.apps` casa com o de outra instituição (só os pares do mesmo banco escritos à mão).
   - 2026-11-23, `test:` `0fff511`: os cinco arquivos do contrato, com 84 execuções (esqueleto 20, distância 6, casa 35, não casa 13, `BankNameMatcherTest` 10). Mais o cenário `every loaded prefix and suffix is removed`, pedido pelo usuário com o `fix:`, que percorre as listas carregadas.
   - **Tamanho:** 499 linhas de código e testes (`BankNameMatcher.kt` 158; testes 57 + 39 + 86 + 39 + 120), contra 463 estimadas; abaixo de 500, então a `editDistance` ficou no batch.
7. [x] **Conferência dos critérios**, local e desfeita sem commit:
   - tirar o mapa de letras parecidas derruba os casos de cirílico e de `0→o`;
   - dar tolerância 1 aos nomes de até 5 letras derruba o caso "Baixa";
   - trocar a igualdade por "contém" derruba "Caixa de Ferramentas";
   - tirar o corte no separador derruba "Next - Launcher";
   - `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
   - 2026-11-23, local (Windows), cada alteração desfeita com `git checkout` (árvore limpa depois de cada uma), rodando as cinco classes do BATCH-02:
     - sem o mapa (`append(it)`) → 9 falhas: esqueleto de `Nubаnk`, das 16 letras cirílicas, das 7 gregas, de `01!34@5$7`, de `Bradesc0` e de `Calxa`; casa `1taú`, `Ltaú` e `Calxa` (AC-005). **Nuance:** os casos de casa `Nubаnk`, `Bradesc0` e `Mercad0 Pag0` continuam casando sem o mapa, porque a letra que sobra (ou some) conta como uma edição, dentro do limite de nomes com 6 letras ou mais. Quem pega o mapa nesses casos é o teste de esqueleto. Nos nomes de até 5 letras (`itau`, `caixa`), o mapa é o que faz casar.
     - tolerância 1 até 5 letras → 2 falhas: não casa `Baixa` e `five letter name needs equal skeleton` (AC-008).
     - "contém" (`|| bankSkeleton in variant`) → 12 falhas: não casa `Caixa de Ferramentas`, `Next Launcher`, `Neon Light`, `Itaú Shop`, `Bradesco Seguros`, `Atualização Nubank`, `Nubank Wallpapers`, `Inter&Co` e `Neon Pay`; e os cenários de prefixos e sufixos, de 5 letras e de pares entre instituições (AC-010).
     - sem o corte no separador → 10 falhas: casa `Next - Launcher`, `Caixa – Organizador` e os três títulos da Play copiados; esqueleto dos cinco casos com separador (AC-011).
     - (além do plano) prefixos e sufixos sem o esqueleto (`words.toList()`) → 2 falhas: `every loaded prefix and suffix is removed` e `prefix and suffixes are removed including glued and repeated`.
     - `./gradlew clean build` verde em 3 min 22 s, sem aviso `w:` do Kotlin e sem diff em `core-data/schemas` (AC-021, AC-022).
       - Testes: `app` 32, `core-data` 17 (2 ignorados no Windows; só rodam no CI), `core-detection` 294 (210 de antes + 84 novos), `core-domain` 32, `core-nfc` 1, `decoy-app` 1. Total: 377.
       - `LibraryManifestTest` e `MergedManifestTest` passam; `koverVerify` do `core-domain` passa.
       - Lint sem achados em `app`, `core-data`, `core-detection` e `core-nfc`; o único aviso é o `MissingApplicationIcon` do `decoy-app`, que já existia.
8. [x] **Documentação** (`docs:`): seção "Comparação de nomes do S9" no `core-detection/README.md` (coletor no BATCH-03); `SKILLS.md` (normalização `internal`); `glossary.md` (esqueleto do nome); ADR-020: normalização implementada.
   - 2026-11-23: os quatro documentos acima. No ADR-020, também a emenda 1.3 (prefixos e sufixos no formato do esqueleto) e a decisão de não trocar `|` por `i`. Emenda 1.3 da spec (BR-007), 1.1 do contrato (`BANK_NAME_PREFIXES` e `BANK_NAME_SUFFIXES` `internal`, cenário novo no `BankNameMatcherTest`) e nota na arquitetura.

### Correção depois do BATCH-02 (PR "SPEC-015 fix: letras parecidas fora do mapa", `Refs #11`)
Achada pelo usuário antes do merge do #68. Ficou num PR separado, a partir da `main` depois do merge, porque com ela o BATCH-02 passaria de 500 linhas (515). O BATCH-03 começa depois do merge deste PR.
- **Defeito:** uma letra parecida fora do mapa era apagada, e nos nomes de até 5 letras a imitação escapava ("ıtaú" virava `tau`, "cɑixɑ" virava `cix`, "ɪnter" virava `nter`). Além disso, `：` e `｜` de largura cheia não cortavam, porque o NFKC vinha depois do corte.
- **`fix:` `55139fa`:**
  - `ı ɑ ɪ ɩ օ` e as maiúsculas pequenas latinas entram no mapa, em escapes `\uXXXX`; o `ʟ` vira `i`, como o `l`;
  - NFKC antes do corte no separador;
  - 6 casos novos (4 de esqueleto, 2 de casa). O código ganha 10 linhas e perde 5, e os testes ganham 11.
- **`docs:`:** emenda 1.4 da spec (BR-006), 1.2 do contrato, ADR-020 (mapa parcial e ordem do NFKC), README do `core-detection`, `SKILLS.md` e plano de testes.
- **Issue #69:** gerar o mapa a partir do `confusables.txt` do UTS #39 (fora do MVP).
- `./gradlew build` verde, sem diff em `core-data/schemas`. `core-detection` com 300 testes (294 + 6).

### BATCH-03: coletor de S9 (PR "SPEC-015 BATCH-03 …", `Closes #11`)
9. [x] **Coletor** (`feat:`): `BankLookalikeCollector(knownBankApps)` com o conjunto de pacotes copiado e o `BankNameMatcher`; KDoc com o que conta, o que não conta e as limitações; parâmetro `label` na `anInstalledApp`. `./gradlew build` verde.
   - 2026-11-23, `feat:` `8149a08`, a partir da `main` em `0c8171c` (depois do merge do #70). Todas as chamadas da `anInstalledApp` usam argumentos nomeados, então o `label` em segundo lugar não desloca nenhuma.
10. [x] **Testes** (`test:`, papel tester): pacote oficial; pacote de outro app do mesmo banco; nome de banco com outro pacote; pacote que difere por sufixo ou maiúsculas; pacote emprestado (documenta a limitação); nome sem relação; lista ampliada com banco fictício; lista vazia; conjunto alterado depois; só S9.
   - 2026-11-23, `test:` `5c67012`: 10 cenários, lista pequena escrita à mão (Nubank, Itaú, Itaú Empresas) e um banco fictício.
   - **Tamanho:** 188 linhas de código e testes (coletor 49, fixture +2/−1, teste 137), contra 186 estimadas.
11. [x] **Conferência dos critérios**, local e desfeita sem commit: tirar o passo do pacote derruba o caso do pacote oficial; comparar o pacote sem maiúsculas derruba o caso de maiúsculas; `./gradlew build` verde sem aviso novo e sem diff em `core-data/schemas`.
   - 2026-11-23, local (Windows), cada alteração desfeita com `git checkout`:
     - sem o passo do pacote → 5 falhas: `official package yields nothing`, `package of another app of the same bank yields nothing`, `borrowed official package yields nothing`, `extended list flags fictitious bank imitator` e `later change to given set does not affect collector` (AC-015);
     - pacote comparado sem maiúsculas (`lowercase()` dos dois lados) → 1 falha: `package differing by suffix or case yields s9` (AC-016).
   - `./gradlew clean build` verde em 2 min 49 s, sem aviso `w:` do Kotlin e sem diff em `core-data/schemas` (AC-021, AC-022).
     - Testes: `app` 32, `core-data` 17 (2 ignorados no Windows; só rodam no CI), `core-detection` 310 (300 de antes + 10 novos), `core-domain` 32, `core-nfc` 1, `decoy-app` 1. Total: 393.
     - `LibraryManifestTest` e `MergedManifestTest` passam; `koverVerify` do `core-domain` passa (a fixture fica fora do Kover).
     - Lint sem achados em `app`, `core-data`, `core-detection` e `core-nfc`; o único aviso é o `MissingApplicationIcon` do `decoy-app`, que já existia.
12. [x] **Documentação** (`docs:`): READMEs do `core-detection` (coletor de S9, API pública com treze classes) e do `core-domain` (parâmetro da fixture); §8 de `docs/especificacao.md` (linha S9); `.agent/architecture.md` (linha S9); `SKILLS.md`, `project-reference.md`, `context.md`; ADR-020: S9 implementado.
   - 2026-11-23: os documentos acima. No `project-reference.md`, o `BankLookalikeCollector` na linha do `SignalCollector` e a API pública com treze classes; no `context.md`, o coletor de S9 (falta só o de S7, SPEC-016); plano de testes com a seção BATCH-03.

## Tamanho: cerca de 970 linhas, em três batches

| Batch | Conteúdo | Código | Testes | Total |
|---|---|---|---|---|
| **BATCH-01** | `KnownWallets`, `KnownBankApp`, `KnownBanks`, listas esperadas e testes | 158 | 161 | **319** |
| **BATCH-02** | `BankNameMatcher` e testes do esqueleto, da distância e da comparação | 140 | 323 | **463** |
| **BATCH-03** | `BankLookalikeCollector`, parâmetro da fixture e testes | 51 | 135 | **186** |

- O plano estimou 790 em dois batches (325 e 465). A conta do architect pelos arquivos do contrato deu 968, e o BATCH-02 do plano teria 649 linhas. Como você pediu, a normalização e os testes dela foram separados do coletor, sem `single-pr-reason`.
- A documentação não entra na conta (regra do SDD 1.8.0).

## Revisão do architect (2026-11-21, emenda 1.1 da spec)
- **Nomes:** `KnownWallets` fica. `KnownBank` → `KnownBankApp`: uma entrada é um app ou apelido, não um banco (o Itaú tem três entradas). `KnownBanks.entries` → `KnownBanks.apps`: `entries` é a propriedade dos `enum` do Kotlin. `displayName` fica (é o nome da marca, não o rótulo do aparelho). Coletor: `BankLookalikeCollector(knownBankApps: Set<KnownBankApp>)`, do `Signal.BANK_LOOKALIKE`.
- **`internal` e `private`:** `internal` a classe `BankNameMatcher` e as funções `bankNameSkeleton` e `editDistance`, testadas direto; o resto `private` (separadores, mapa, prefixos, sufixos, variantes, limites). Sem `@VisibleForTesting`.
- **Validação em branco:** mantida, com `require` e mensagens fixas, como o `LastFourDigits`. Não valida o formato do pacote nem o esqueleto do nome (o `BankNameMatcher` descarta esqueletos vazios).
- **Detekt:** limites 5 e 9 em `const val` (`MagicNumber`); `lowercase()` invariante; acentos por `CharCategory.NON_SPACING_MARK`, sem regex.
- **Estimativa:** 968 pelos arquivos do contrato; spec 1.1 com `estimate-lines: 970` e três batches (320, 465 e 185). Se o BATCH-02 passar de 500 na implementação, a distância vai para um batch próprio.
- **Branches:** como na SPEC-014, uma por batch, cada uma a partir da `main` depois do merge do anterior.
- Detalhes em `.agent/plans/spec-015-listas-imitacao-architecture.md`.

## Database changes
Nenhuma. O S9 vai no bit 8 de `app_snapshot.signals` pela SPEC-018; o `1.json` não muda (ADR-014).

## Risks & open questions
- **Rótulos não conferidos:** os nomes vêm do título da Play e do desenvolvedor, não do rótulo do aparelho. Se o rótulo real de um app oficial for outro ("Inter&Co", por exemplo), um imitador com esse rótulo escapa. Conferência na SPEC-022.
- **Nomes curtos sem tolerância:** "Inter", "next", "Neon", "BB" e "99Pay" só casam iguais. Um app legítimo com esse nome exato recebe S9 (falso positivo aceito, no máximo amarelo).
- **Corte no separador:** um app legítimo cujo nome começa com o nome de um banco seguido de separador ("Next - Launcher", "Caixa – Organizador") recebe S9. Falso positivo aceito, com teste e no ADR-020.
- **Imitação com palavra fora da lista** ("Atualização Nubank") escapa, por decisão. A lista de prefixos e sufixos pode crescer com amostras reais.
- **Empréstimo de nome de pacote:** ver a limitação acima; o S1 continua cobrindo o app instalado à mão. Defesa na issue #65.
- **BATCH-02 perto do limite (463 linhas):** se passar de 500, a distância vai para um batch próprio.
- **Listas desatualizadas:** um banco que lançar o Pix por aproximação no próprio app recebe S7 até a lista ser atualizada; um app oficial novo de um banco da lista pode receber S9 até entrar nela. As listas são embutidas (RNF-01, sem internet); a atualização é por versão do app ou pelo SDK.
- **Itaú e Inter na lista de carteiras:** as fontes dizem que pagam pelo app, mas não que viram o padrão. Entrada sem custo se nunca forem o padrão.
- **Banco fictício do app isca:** a SPEC-021 ou a SPEC-022 decide como o teste ponta a ponta injeta o banco fictício na lista.
- **Regra dos apps de clonagem:** fica para a SPEC-017, com as duas formas descritas acima (comentário na #13).

## Acceptance criteria
- [x] Listas em arquivo único por tipo, públicas, sem alteração possível nem por cast, com fonte por entrada (critério da #11; AC-001 a AC-003)
- [x] `KnownWallets` com as 6 carteiras e `KnownBanks` com as 29 entradas, conferidas por listas escritas à mão; `KnownBankApp` recusa nome ou pacote em branco (AC-001, AC-002, AC-004)
- [x] Nome igual ou quase igual com pacote diferente gera S9: maiúsculas, acentos, espaços, separadores, sufixos e prefixos da lista, letras parecidas e distância dentro do limite (critério da #11; AC-005 a AC-009, AC-016)
- [x] Pacote oficial não gera S9, inclusive o de outro app do mesmo banco (critério da #11; AC-015, AC-017)
- [x] Nomes curtos só casam iguais; legítimos da tabela não geram S9; falsos positivos aceitos e imitações que escapam documentados; nenhum nome da lista casa com o de outra instituição; esqueleto vazio nunca casa (AC-008, AC-010 a AC-014)
- [x] Lista obrigatória no coletor, copiada, ampliável pelo SDK; lista vazia nunca gera S9; o coletor só devolve S9 ou nada (AC-018, AC-019)
- [x] Sem `android.*` nas listas, na normalização e no coletor; sem permissão nova; schema do Room sem diff (AC-020, AC-021)
- [x] ktlint, detekt, lint e testes passam em cada batch (critério da #11; AC-022)
- [ ] Documentação de cada batch, com o ADR-020 e as §5 e §8, e CI de cada PR verde (critério da #11; AC-023)

## Fontes
- [TechTudo, 03/08/2026: 9 bancos que já permitem pagar por aproximação usando Pix](https://www.techtudo.com.br/listas/2026/08/9-bancos-que-ja-permitem-pagar-por-aproximacao-usando-pix-veja-quais-edapps.ghtml)
- [BPMoney, 03/08/2026: Pix por aproximação, 9 bancos já oferecem a função](https://bpmoney.com.br/mercado/pix-por-aproximacao-9-bancos-como-usar/)
- [Mobile Time, 27/01/2026: Itaú passa a ter Pix por aproximação no aplicativo](https://www.mobiletime.com.br/noticias/27/01/2026/itau-pix-aplicativo/)
- [TechTudo, 07/04/2026: como funciona o pagamento por aproximação no Nubank](https://www.techtudo.com.br/noticias/2026/04/como-funciona-o-pix-por-aproximacao-no-nubank-veja-a-novidade-edapps.ghtml)
- [Mercado Pago: Pix por aproximação, como obter e ativar](https://www.mercadopago.com.br/blog/como-obter-pix-por-aproximacao)
- [Melhores Cartões, 24/01/2026: bancos com mais clientes no Brasil (BCB, dez/2025)](https://www.melhorescartoes.com.br/bancos-com-mais-clientes-brasil.html)
- [Forbes Brasil, 22/01/2026: Nubank ultrapassa 112 milhões de clientes](https://forbes.com.br/forbes-money/2026/01/nubank-ultrapassa-112-milhoes-de-clientes-e-se-torna-a-segunda-maior-instituicao-financeira-do-brasil/)
- [Ranking do BCB (fonte primária, histórico)](https://www3.bcb.gov.br/ranking/historico.do)
- Google Play, páginas `play.google.com/store/apps/details?id=<pacote>&hl=pt_BR&gl=BR` de cada pacote das tabelas, e buscas `play.google.com/store/search?q=<banco>&c=apps&hl=pt_BR&gl=BR` para os apps irmãos (consultadas em 2026-11-21).
