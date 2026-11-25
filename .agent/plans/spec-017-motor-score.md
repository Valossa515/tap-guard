# Plan: SPEC-017 Motor de score

> Issue #13 · Etapa 1 (Núcleo de detecção) · Depende de SPEC-010 (#6, fechada) · Especificação RF-103, §5 (pesos, faixas, regras de ajuste, exemplos), §7 (`RiskScorer`), §11 (casos fixos do motor) · ADR-003, ADR-015, ADR-017 a ADR-021
> Spec: `.agent/specs/features/spec-017-motor-score.spec.toon` (FT-017 v1.0, `interface: library`, `estimate-lines: 380`, um PR) · Aceite: `.agent/specs/acceptance/spec-017-motor-score.acceptance.toon` (v1.0)
> Branch: `spec-017-motor-score` (a partir da `main` em `3e987dd`) · Planejado em 2026-11-25 · Um PR, com `Closes #13`
> Issue #13 movida para "Em andamento" no Project 2 em 2026-11-25.

## Summary
Implementação do `RiskScorer` no `core-domain`, em Kotlin puro:
- **Pesos e limites num único arquivo de configuração** da implementação (não no enum `Signal`): os nove pesos da §5, o teto da soma (100), os tetos das regras 3 (29) e 4 (59) e as faixas (verde até 29, amarelo até 59, vermelho a partir de 60).
- **Cálculo:** soma dos pesos dos sinais recebidos, limitada a 100, depois as regras 2 a 4 na ordem da §5. A regra 1 já é cumprida pelo `AppInventory` (SPEC-011): o motor nunca recebe app do sistema.
- **`ScoringContext` continua só com `trustedPackages`** (ADR-015). A regra 3 sai da ausência de S1.
- **Testes:** os cinco exemplos da §5 e uma tabela com todas as combinações que tocam as regras 2 a 4 e as faixas, inclusive os limites 29/30 e 59/60.

A **regra dos apps de clonagem** (segundo comentário da #13) fica **fora desta spec**: forma (B), no coletor de S1, na **SPEC-019 (#73)** (aprovado pelo usuário em 2026-11-25). Comparação e lista abaixo.

## Scope
- **Módulos afetados:** `core-domain` (código e testes) e documentação. `core-detection`, `core-data` e `app` não mudam.
- **Novos arquivos** (nomes finais com o architect):
  - arquivo de configuração dos pesos e limites, em `core.domain.risk`;
  - implementação do `RiskScorer`, em `core.domain.risk`;
  - testes JUnit 5 do motor (exemplos da §5 e tabela de combinações), em `core-domain/src/test/.../risk/`.
- **Arquivos modificados:** documentação: `core-domain/README.md`, `docs/especificacao.md` (§5: score do app confiável), `.agent/backlog_rules.md`, `.agent/decisions.md` (ADR-022, consequência no ADR-003), `.agent/SKILLS.md` (Domain models), `.agent/project-reference.md`, `.agent/glossary.md`, `.agent/context.md`, `.agent/architecture.md`; KDoc de `RiskResult` e `ScoringContext` se o texto mudar (só comentário).
- **Fora do escopo:**
  - regra dos apps de clonagem (SPEC-019, #73);
  - montar o `ScoringContext` a partir de `user_decision` e chamar o motor no `ScanDevice` (SPEC-018, #14);
  - ligação no Hilt (SPEC-018);
  - textos de cada sinal e nível na interface (SPEC-030 em diante);
  - #57 e #65 (fora do MVP).

## Decisões

| Item | Decisão |
|------|---------|
| `ScoringContext` | Só `trustedPackages` (primeiro comentário da #13, ADR-015). Nenhum campo novo |
| Regra 1 | Cumprida pelo inventário (SPEC-011). O critério "100% das regras de ajuste" cobre as regras 2 a 4 |
| Pesos | Na implementação, num único arquivo de configuração. Não no enum `Signal` |
| Score do app confiável (regra 2) | **Decisão do usuário, 2026-11-25:** o score passa sempre pelas regras 3 e 4; a regra 2 só troca o nível para verde. Desfazer "confiável" não muda o número. A KDoc do `RiskResult` ("verde com qualquer score") já permite isso |
| Regra dos apps de clonagem | **Aprovado pelo usuário, 2026-11-25:** forma (B), no coletor de S1, na SPEC-019 (#73), que fecha antes da SPEC-018. Lista de sete apps, com ShareMe, SHAREit e MIUI Backup fora |

## Gap analysis

| # | Categoria | Lacuna | Classificação | Resolução |
|---|-----------|--------|---------------|-----------|
| 1 | Regra | Score de um app confiável: a §5 só fala do nível | 🔴 Bloqueante | Decidido: regras 3 e 4 sempre; regra 2 só no nível |
| 2 | Regra | Ordem das regras 3 e 4 e o que acontece quando as duas valem | 🟡 Não bloqueante | A §5 define ("nesta ordem"). As duas são tetos (mínimo), e o mínimo comuta: a ordem não muda o resultado (seção "Ordem das regras") |
| 3 | Regra | Regra 2 com S7 ou S9 | 🟡 Não bloqueante | A §5 diz "sempre verde": vale com qualquer sinal |
| 4 | Regra | Regra dos apps de clonagem | 🔴 Bloqueante para a SPEC-019, não para esta | Decidido: (B) na SPEC-019 (#73) |
| 5 | Dados | Comparação do pacote com `trustedPackages` | 🟡 Não bloqueante | Exata, com maiúsculas e minúsculas, como em todos os coletores (assumption) |
| 6 | Dados | `RiskResult.signals` | 🟡 Não bloqueante | Os sinais recebidos, sem mudança: a SPEC-018 grava a máscara e o RF-106 explica cada sinal, inclusive num app limitado pelas regras (assumption) |
| 7 | Técnico | Forma e visibilidade do arquivo de pesos; pesos injetáveis ou fixos | 🟡 Não bloqueante | Para o architect. Proposta: `internal`, fixo, e a implementação pública sem parâmetros (a §5 calibra "com testes", não pelo SDK; nada pede pesos por quem usa o SDK) |
| 8 | Testes | 29 e 59 não saem de nenhuma soma (os pesos são múltiplos de 5) | 🟡 Não bloqueante | Os limites 29 e 59 saem dos tetos das regras 3 e 4; 30 e 60 saem de somas exatas. Se o architect separar a função de faixa, ela ganha um teste direto com 0, 29, 30, 59, 60 e 100 |
| 9 | Técnico | Implementação usa `app` só pelo `packageName` | 🟡 Não bloqueante | A assinatura da §7 não muda; o motor lê só `app.packageName` (regra 2) |

## Algoritmo e ordem das regras

1. `soma = min(Σ peso(s) para s em signals, 100)`.
2. **Regra 2:** `confiável = app.packageName in ctx.trustedPackages`. Não muda o score (decisão do usuário).
3. **Regra 3:** se `S1 ∉ signals` e `S7 ∉ signals` e `S9 ∉ signals`, `score = min(score, 29)`.
4. **Regra 4:** se não (`S1 ∈ signals` e algum de `S2`, `S3`, `S7` ∈ `signals`), `score = min(score, 59)`.
5. **Nível:** `GREEN` se confiável; senão pela faixa do score final (0–29 verde, 30–59 amarelo, 60+ vermelho).

**Quando mais de uma regra vale:**
- **Regras 3 e 4 juntas** (sem S1, sem S7 e sem S9): a regra 4 sempre vale quando falta S1. `min(min(s, 29), 59) = min(s, 29)`: vence o teto de 29. O mínimo comuta, então aplicar 4 antes de 3 dá o mesmo resultado.
- **Exceção da regra 3 (S7 ou S9) sem S1:** a regra 3 não vale, mas a regra 4 vale (falta S1). O teto é 59: **um app sem S1 nunca fica vermelho**, nem com S7. Isso confirma o ADR-020 ("pode ficar amarelo, mas não vermelho").
- **Com S1:** a regra 3 nunca vale. A regra 4 vale só sem sinal de NFC (S2, S3 ou S7).
- **Regra 2 com qualquer outra:** as regras 3 e 4 continuam calculando o score; a regra 2 só decide o nível. Como ela não mexe no score, também comuta com as outras. A ordem da §5 continua respeitada: a regra 2 vence o nível que as regras 3 e 4 dariam.

## Tabela de combinações (vira teste)

Pesos: S1 30, S2 15, S3 20, S4 25, S5 10, S6 10, S7 30, S8 10, S9 20. "R3" e "R4" dizem se a regra muda o score. Pacote do app: `com.example.app`; `trustedPackages` vazio, salvo nas linhas T.

**Um sinal por vez (confere cada peso)**

| # | Sinais | Soma | R3 | R4 | Score | Nível |
|---|--------|------|----|----|-------|-------|
| P0 | nenhum | 0 | — | — | 0 | verde |
| P1 | S1 | 30 | não vale (S1) | vale, sem efeito | 30 | amarelo |
| P2 | S2 | 15 | vale, sem efeito | vale, sem efeito | 15 | verde |
| P3 | S3 | 20 | idem | idem | 20 | verde |
| P4 | S4 | 25 | idem | idem | 25 | verde |
| P5 | S5 | 10 | idem | idem | 10 | verde |
| P6 | S6 | 10 | idem | idem | 10 | verde |
| P7 | S7 | 30 | exceção (S7) | vale, sem efeito | 30 | amarelo |
| P8 | S8 | 10 | vale, sem efeito | vale, sem efeito | 10 | verde |
| P9 | S9 | 20 | exceção (S9) | vale, sem efeito | 20 | verde |

**Os cinco exemplos da §5 (testes com nome próprio)**

| # | Exemplo | Sinais | Soma | Score | Nível |
|---|---------|--------|------|-------|-------|
| E1 | APK de fora da loja com NFC e HCE | S1 S2 S3 | 65 | 65 | vermelho |
| E2 | Mesmo app, instalado ontem e com acessibilidade ativa | S1 S2 S3 S4 S6 | 100 | 100 | vermelho |
| E3 | App de banco da loja oficial com NFC e HCE | S2 S3 | 35 | 29 (R3) | verde |
| E4 | Jogo instalado por APK sem NFC | S1 | 30 | 30 | amarelo |
| E5 | App de fora da loja com acessibilidade e SMS, sem NFC | S1 S4 S8 | 65 | 59 (R4) | amarelo |

**Regra 3 e as exceções S7 e S9**

| # | Sinais | Soma | Score | Nível | O que confere |
|---|--------|------|-------|-------|---------------|
| C01 | S2 S3 S4 | 60 | 29 (R3) | verde | soma 60 sem S1 vira 29 |
| C02 | S2 S3 S4 S5 S6 S8 | 90 | 29 (R3) | verde | maior soma sem S1, S7 e S9 |
| C03 | S4 S5 | 35 | 29 (R3) | verde | R3 sem sinal de NFC |
| C04 | S7 S2 S3 | 65 | 59 (R4) | amarelo | exceção S7, teto amarelo sem S1 |
| C05 | S7 S4 | 55 | 55 | amarelo | exceção S7 abaixo de 59 |
| C06 | S9 S6 | 30 | 30 | amarelo | exceção S9, limite 30 (ADR-019) |
| C07 | S9 S2 S3 S4 | 80 | 59 (R4) | amarelo | exceção S9, teto amarelo |
| C08 | S7 S9 | 50 | 50 | amarelo | as duas exceções juntas |
| C09 | todos menos S1 | 100 (140) | 59 (R4) | amarelo | app de loja nunca fica vermelho |

**Regra 4 (com S1)**

| # | Sinais | Soma | Score | Nível | O que confere |
|---|--------|------|-------|-------|---------------|
| C10 | S1 S2 | 45 | 45 | amarelo | NFC abaixo de 60 |
| C11 | S1 S3 | 50 | 50 | amarelo | idem com S3 |
| C12 | S1 S3 S6 | 60 | 60 | vermelho | limite 60 com NFC (ADR-019) |
| C13 | S1 S7 | 60 | 60 | vermelho | S7 conta como sinal de NFC |
| C14 | S1 S2 S5 S8 | 65 | 65 | vermelho | S2 sozinho basta |
| C15 | S1 S5 S6 S8 | 60 | 59 (R4) | amarelo | soma 60 sem NFC vira 59 |
| C16 | S1 S4 S5 S6 | 75 | 59 (R4) | amarelo | R4 bem acima do teto |
| C17 | S1 S4 S5 S6 S8 S9 | 100 (105) | 59 (R4) | amarelo | teto de 100 e depois 59 |
| C18 | S1 S9 S2 | 65 | 65 | vermelho | S9 com S1 e NFC |
| C19 | todos os nove | 100 (170) | 100 | vermelho | teto de 100 |

**Regra 2 (app confiável)**

| # | `trustedPackages` | Sinais | Score | Nível | O que confere |
|---|-------------------|--------|-------|-------|---------------|
| T1 | `com.example.app` | S1 S2 S3 | 65 | verde | vermelho vira verde; score mantido |
| T2 | `com.example.app` | S1 S4 S8 | 59 (R4) | verde | R4 continua no score |
| T3 | `com.example.app` | S2 S3 | 29 (R3) | verde | R3 continua no score |
| T4 | `com.example.app` | S7 S2 S3 | 59 (R4) | verde | exceção S7 e R4 continuam |
| T5 | `com.example.app` | todos os nove | 100 | verde | score máximo, verde |
| T6 | `com.example.app` | nenhum | 0 | verde | sem sinais |
| T7 | `com.other.app` | S1 S2 S3 | 65 | vermelho | outro pacote não conta |
| T8 | `COM.EXAMPLE.APP` | S1 S2 S3 | 65 | vermelho | comparação exata |

Faixas cobertas: 0, 10, 15, 20, 25 e **29** (verde); **30**, 45, 50, 55 e **59** (amarelo); **60**, 65 e 100 (vermelho). Em toda linha, `RiskResult.signals` é igual aos sinais recebidos.

São 42 linhas (P1 e E4 repetem o mesmo caso de propósito: um confere o peso, o outro o exemplo). Os testes leem a tabela escrita à mão no teste, nunca o arquivo de pesos: mudar um peso derruba os casos, e o `backlog_rules.md` já exige atualizar os exemplos na mesma spec.

## Regra dos apps de clonagem: (A) no score ou (B) no coletor de S1

**O problema** (segundo comentário da #13, ADR-017): um app de banco copiado para um celular novo por Smart Switch, Mi Mover ou Clone Phone tem o app de clonagem como instalador, recebe S1 e, com S2 e S3, fica vermelho (65). As duas formas abaixo são a forma **estreita**: só vale com pacote de banco **e** instalador de clonagem conhecido. A forma larga já foi descartada no comentário (reabre a brecha do S1).

| | **(A) Regra no score** | **(B) No coletor de S1** |
|---|---|---|
| Onde fica | `RiskScorer`, `core-domain` | `InstallSourceCollector`, `core-detection` (SPEC-012) |
| Como funciona | Com `app.packageName` num conjunto de pacotes de banco e `app.installerPackageName` num conjunto de apps de clonagem, o score recebe o teto da regra 3 (29, salvo S7 ou S9) mesmo com S1 | O coletor não devolve S1 quando o pacote está no conjunto de bancos **e** o instalador está no conjunto de apps de clonagem. O score não muda; a regra 3 já limita a 29 pela ausência de S1 |
| `core-domain` | `ScoringContext` ganha dois `Set<String>` (bancos e apps de clonagem): desfaz a decisão do ADR-015 e do primeiro comentário. O `InstalledApp` já traz o instalador, então não precisa de campo novo | Nenhuma mudança |
| §5 | Regra de ajuste nova (ou exceção na regra 3), com ordem a definir em relação à regra 4 | Muda a definição do S1: "fora de loja confiável, salvo app de banco conhecido copiado por app de clonagem conhecido". As quatro regras não mudam |
| API pública do SDK | Muda o `ScoringContext` (tipo do `core-domain`, que todo usuário do SDK monta). Mais a lista nova no `core-detection` | Muda o construtor do `InstallSourceCollector` (dois parâmetros obrigatórios, sem padrão, copiados, como o S9). Mais a lista nova no `core-detection` (15 classes públicas) |
| Quem monta os conjuntos | `ScanDevice` (SPEC-018) monta três conjuntos para o contexto, a cada varredura | `ScanDevice` constrói o coletor uma vez, com `KnownBanks` e a lista nova, como já faz com `TrustedStores` |
| Sinais na interface | O app fica com S1 na lista de sinais e verde. O detalhe (RF-106) explicaria "instalado fora de uma loja confiável" num banco verde | Sem S1: o banco copiado aparece igual ao banco da loja |
| Interação com as outras regras | Precisa decidir: a exceção S7/S9 vale? E a regra 4, que olha o S1? | Herda tudo do caso "veio da loja": S7 ou S9 tiram o teto e a regra 4 limita a 59 |
| Testes | JUnit 5 no `core-domain`, contam no Kover. A tabela de combinações ganha a dimensão banco × instalador de clonagem × S1 × S7/S9 | JUnit 4 sem Robolectric no `core-detection`, fora do Kover. Lista testada contra uma lista escrita à mão; coletor parametrizado sobre ela. Seis chamadas existentes do construtor mudam nos testes do S1 |
| Brecha do pacote emprestado (#57, #65) | Igual à (B) hoje: só confere nomes. Mas as defesas futuras (instalador app do sistema, certificado de assinatura) são dados de detecção: na (A) elas teriam de chegar ao `core-domain` por mais campos, ou a regra mudaria de módulo | Igual hoje. As defesas futuras ficam no mesmo coletor que já decide o S1 para as lojas: a #57 ("instalador precisa ser app do sistema") cobre lojas e apps de clonagem com a mesma regra, e a #65 compara o certificado do pacote de banco no mesmo lugar |

**Brecha nova, nas duas formas:** nenhuma além da que já existe.
- Para usar a exceção, o golpe precisa do pacote de um banco ausente no aparelho **e** de um instalador registrado como app de clonagem.
- Quem consegue registrar um instalador com nome emprestado (um app comum com o pacote de um app de clonagem ausente) já consegue registrar uma **loja** com nome emprestado (`com.huawei.appmarket` num Samsung, ADR-017), o que tira o S1 de **qualquer** pacote, não só de banco. A exceção não dá capacidade nova.
- Resta o caso do comentário: o golpe com pacote de banco emprestado já estava no celular antigo e é copiado junto. No celular antigo ele tinha S1 (e, com NFC e HCE, ficava vermelho).

**Recomendação: (B).** Não muda o score, o `ScoringContext` nem as regras da §5; o banco copiado se comporta em todas as regras como o banco da loja; as defesas da #57 e da #65 ficam no mesmo lugar. O custo é mudar o construtor do `InstallSourceCollector` antes de o SDK ser publicado.

### Lista proposta de apps de clonagem (exigida pela (B))
Formato das listas da SPEC-015 (ADR-020): `object` público num arquivo próprio em `core.detection.signal`, conjunto que não pode ser alterado nem por cast (`Collections.unmodifiableSet(linkedSetOf(...))`), um comentário por entrada com o nome e a fonte, ampliado ou reduzido por quem usa o SDK com `+` ou `-`. Nome do `object` com o architect (por exemplo, `PhoneCloneApps.packageNames`).

Fonte principal: a tabela "Cloners & Movers" do [Talsec, Installation Sources Cookbook](https://docs.talsec.app/appsec-articles/glossary/sideloading/installation-sources-cookbook), a mesma fonte usada no ADR-017. Ela diz que, na migração, "the cloner becomes their attributed installer" (o app de clonagem vira o instalador). Segunda fonte: a página da Play (`play.google.com/store/apps/details?id=<pacote>`), consultada em 2026-11-25. A busca automática só leu o título da página, não o desenvolvedor.

| Pacote | App | Talsec | Segunda fonte |
|--------|-----|--------|---------------|
| `com.sec.android.easyMover` | Samsung Smart Switch | "Smart Switch (Samsung)" | Play: "Samsung Smart Switch Mobile" (200); [bayton](https://sysapps.bayton.org/packages/com.sec.android.easyMover); ADR-017 |
| `com.miui.huanji` | Xiaomi Mi Mover | "Mi Mover (Xiaomi)" | Play: "Mi Mover" (200); ADR-017 |
| `com.coloros.backuprestore` | OPPO Clone Phone | "Clone Phone (Oppo)" | Play: "OPPO Clone Phone" (200); [APKMirror](https://www.apkmirror.com/apk/coloros/oppo-clone-phone/); ADR-017 |
| `com.oneplus.backuprestore` | OnePlus Clone Phone | "Clone Phone (OnePlus)" | Play: "Clone Phone - OnePlus app" (200) |
| `com.hicloud.android.clone` | Huawei Phone Clone | "Phone Clone (Huawei)" | Play: 404 (Huawei fora da Play); [APKMirror](https://apkmirror.com/apk/huawei-internet-services/phone-clone/), desenvolvedor Huawei Internet Services |
| `com.hihonor.android.clone` | HONOR Device Clone | "Phone Clone (Honor)" | Play: "Device Clone" (200) |
| `com.vivo.easyshare` | vivo EasyShare | "EasyShare (Vivo)" | Play: "EasyShare" (200) |

**Ficam fora:**
- **ShareMe** (`com.xiaomi.midrop`), **SHAREit** (`com.lenovo.anyshare.gps`) e **SHAREit Lite** (`shareit.lite`), que o Talsec põe na mesma tabela: são apps de compartilhar arquivos entre quaisquer pessoas, não de migrar o próprio celular. Pô-los na lista deixaria qualquer um mandar um APK com pacote de banco emprestado sem S1. É a forma mais comum de instalar à mão, como o Xender (`cn.xender`), que o Talsec põe entre os instaladores de APK à mão.
- **MIUI Backup** (`com.miui.backup`): restaura um backup, não migra entre celulares; Play 404, só a fonte do Talsec. Pode entrar depois, se a SPEC-022 mostrar o caso.
- **Não achados em fonte:** realme (não confirmado se usa o Clone Phone da OPPO com o mesmo pacote), Motorola, Tecno/Infinix (Transsion), LG Mobile Switch. A restauração do Google reinstala pela Play (instalador `com.android.vending`, sem S1); não conferido em fonte, conferir na SPEC-022.

**Conferir na SPEC-022 (#17):** que cada app de clonagem aparece como `installingPackageName` do app copiado (o Talsec afirma, mas não diz a API) e se algum deles reinstala pela Play quando o app existe lá.

### Entra nesta spec?
**Não. Spec própria: SPEC-019 (#73)**, na Etapa 1, antes da SPEC-018 (numeração do usuário: a faixa 07x é da Etapa 7):
- com a (B), a regra é do S1, no `core-detection`, com outro tipo de teste (JUnit 4) e fora do Kover; a #13 é do `core-domain` (critério "nenhuma dependência Android", cobertura);
- as duas juntas passariam de 500 linhas (380 + 260) e exigiriam dois PRs de qualquer jeito;
- deve fechar antes da SPEC-018, que constrói o `InstallSourceCollector` com os dois conjuntos novos. Depende da SPEC-012 e da SPEC-015 (fechadas), não desta.

Se a escolha for a (A), a regra entra nesta spec (muda o `ScoringContext` e a §5), e a lista vai para o `core-detection` na mesma spec, que passaria a ter dois batches.

### Tamanho de cada parte

| Parte | Código | Testes | Margem nos testes | Total |
|-------|--------|--------|-------------------|-------|
| SPEC-017 (esta) | ~90 (pesos ~35, motor ~55) | ~230 (42 casos da tabela, cinco exemplos com nome, sinais repassados) | +60 (~25%) | **380** |
| Regra de clonagem (B), SPEC-019 | ~55 (lista ~40, coletor ~15) | ~165 (lista contra lista à mão ~45; coletor parametrizado e casos negativos ~110; seis chamadas ajustadas ~10) | +40 | **~260** |
| (A), se escolhida, nesta spec | ~65 (contexto ~10, regra ~15, lista ~40) | ~200 (tabela com banco × clonagem × S1 × S7/S9, contexto, lista) | +50 | +~315 (total ~695, dois batches) |

A documentação e os artefatos do SDD não contam. A margem vem da SPEC-016 (435 estimadas, 521 reais).

## Approach
- Segue `skills/android/SKILL.md` para o domínio puro e as regras do projeto em `.agent/SKILLS.md` › Domain models: Kotlin puro, sem `android.*`, sem dependência nova, sem `@Suppress`.
- **Arquivo de configuração:** um só arquivo com os nove pesos e os limites (100, 29, 59 e as faixas). Mapa ou `when` exaustivo sobre `Signal` fica com o architect; com `when` exaustivo, um sinal novo sem peso não compila. Constantes nomeadas (`MagicNumber` do detekt vale na produção).
- **Implementação pública** do `RiskScorer` (o `ScanDevice` do `core-detection` e o SDK a usam); sem estado; não lança exceção: o score sai sempre entre 0 e 100, e o `RiskResult` confere isso.
- **Testes (`skills/android/testing.md`):** JUnit 5, sem mocks. Os cinco exemplos da §5 como testes com nome próprio (§11); a tabela como `@ParameterizedTest` com `@MethodSource` (o artefato `junit-jupiter` já traz o `junit-jupiter-params`; nenhuma dependência nova). Primeiro teste parametrizado do `core-domain`: o padrão de tabela escrita à mão vem dos testes do `core-detection` (JUnit 4 `Parameterized`).
- **Kover:** o motor entra na cobertura do `core-domain` (mínimo 80%; hoje 98,9%).

## Tasks
1. [ ] Arquivo de configuração dos pesos e limites, com KDoc que aponta a §5 e o `backlog_rules.md`.
2. [ ] Implementação do `RiskScorer`: soma limitada a 100, regras 2 a 4 na ordem da seção "Algoritmo", nível pela faixa ou verde quando confiável.
3. [ ] Testes: os cinco exemplos da §5 com nome próprio e a tabela de combinações (P, C, T), escrita à mão.
4. [ ] Documentação: ADR-022 (pesos num arquivo, algoritmo e ordem, score do confiável, recomendação da regra de clonagem); §5 (score do confiável, ordem e "app sem S1 nunca fica vermelho"); `backlog_rules.md`; `core-domain/README.md` (estado, pacote `risk`, cobertura); `SKILLS.md`, `project-reference.md`, `glossary.md`, `context.md`, `architecture.md`.

## Database changes
Nenhuma. O score e o nível vão para `app_snapshot.score` e `app_snapshot.level` pela SPEC-018; o `1.json` não muda.

## Risks & open questions
- **Regra de clonagem:** aprovada em 2026-11-25 e aberta como SPEC-019 (#73), em "Pronta" no Project 2; comentário na #13.
- O score de um app confiável pode passar de 29 com nível verde (decisão do usuário). Quem ordena listas por score (RF-105) precisa separar os confiáveis antes, como o RF-108 já pede.
- Pesos são ponto de partida (§5). Calibrar depois muda a tabela inteira; a regra do `backlog_rules.md` (atualizar exemplos e testes na mesma spec) continua.

## Acceptance criteria
- [ ] Os cinco exemplos da §5 passam como testes com nome próprio.
- [ ] Cada peso da §5 conferido por um caso de um sinal só.
- [ ] Todas as linhas da tabela de combinações passam (regras 2 a 4 e limites 29/30 e 59/60).
- [ ] App confiável: nível verde e score calculado pelas regras 3 e 4.
- [ ] `RiskResult.signals` igual aos sinais recebidos.
- [ ] Nenhuma dependência Android no `core-domain`; nenhuma dependência nova; sem `@Suppress` novo.
- [ ] `koverVerify` do `core-domain` passa (mínimo 80%).
- [ ] ktlint, detekt e testes passam (`./gradlew build`).
- [ ] Documentação do módulo, §5, `backlog_rules.md` e ADR-022 atualizados.
