package io.github.valossa515.tapguard.core.detection.signal

import java.text.Normalizer

/**
 * Compara o rótulo de um app com os nomes de exibição dos apps de banco conhecidos, para o sinal S9
 * (ADR-020). Não conhece pacote nem sinal: quem tira o app oficial é o coletor de S9.
 *
 * O rótulo casa quando alguma variante do esqueleto dele ([bankNameSkeleton]) fica a uma distância
 * [editDistance] de algum esqueleto da lista dentro do limite desse nome:
 * - variantes: o esqueleto; sem um prefixo (`banco`, `meu`, `app`); e sem sufixos, repetidamente
 *   (`token`, `empresas`, `empresa`, `cartoes`, `cartao`, `pj`, `app`, `mobile`, `oficial`,
 *   `digital`, `seguranca`). Funciona sobre o esqueleto colado ("ItauToken"); um prefixo ou sufixo só
 *   sai se sobrar algo;
 * - limite pelo tamanho do esqueleto do nome da lista: até 5 letras, 0 (só igual, porque uma letra a
 *   mais ou a menos já forma outra palavra: "Baixa" não casa com "Caixa"); de 6 a 9, até 1; 10 ou
 *   mais, até 2.
 *
 * Esqueleto vazio nunca casa, e um nome da lista com esqueleto vazio é ignorado.
 *
 * Comportamentos aceitos (decisão "nome inteiro", sem "contém"):
 * - nome curto igual de outro desenvolvedor casa ("Inter", "NEXT");
 * - o corte no separador também casa um app legítimo cujo nome começa com o de um banco ("Next -
 *   Launcher", "Caixa – Organizador");
 * - palavras fora da lista fechada escapam ("Atualização Nubank", "Nubank Wallpapers", "Inter&Co",
 *   "Neon Pay").
 */
internal class BankNameMatcher(
    displayNames: Collection<String>,
) {
    private val bankSkeletons: List<String> =
        displayNames.map(::bankNameSkeleton).filter { it.isNotEmpty() }

    fun matches(label: String): Boolean {
        val skeleton = bankNameSkeleton(label)
        if (skeleton.isEmpty()) return false
        return variants(skeleton).any { variant ->
            bankSkeletons.any { bankSkeleton -> editDistance(variant, bankSkeleton) <= tolerance(bankSkeleton) }
        }
    }
}

/**
 * Esqueleto de um nome, o mesmo para o rótulo do app e para os nomes da lista:
 * 1. corta no primeiro separador (`:`, `|`, ` - `, ` – `, ` — `), se houver algo antes dele;
 * 2. NFKC (largura cheia, ligaduras) e minúsculas invariantes;
 * 3. NFD e remoção das marcas combinantes (acentos), pelo tipo do caractere, sem regex;
 * 4. letras parecidas trocadas: `0→o`; `1`, `!` e `l` por `i`; `3→e`; `4` e `@` por `a`; `5` e `$`
 *    por `s`; `7→t`; cirílico e grego que imitam letras latinas; `rn→m`; `vv→w`;
 * 5. fica só com `a-z` e `0-9` (espaços, pontuação, emojis e caracteres invisíveis saem).
 */
internal fun bankNameSkeleton(label: String): String {
    val folded = Normalizer.normalize(cutAtFirstSeparator(label), Normalizer.Form.NFKC).lowercase()
    val unaccented =
        Normalizer
            .normalize(folded, Normalizer.Form.NFD)
            .filterNot { it.category == CharCategory.NON_SPACING_MARK }
    val latin = buildString { unaccented.forEach { append(LOOKALIKE_LETTERS[it] ?: it) } }
    return latin
        .replace("rn", "m")
        .replace("vv", "w")
        .filter { it in 'a'..'z' || it in '0'..'9' }
}

/**
 * Distância de Damerau-Levenshtein na versão OSA (alinhamento ótimo de cadeias): inserção, remoção,
 * troca e transposição de letras vizinhas valem 1 cada.
 */
internal fun editDistance(
    a: String,
    b: String,
): Int {
    val distances = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) distances[i][0] = i
    for (j in 0..b.length) distances[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            distances[i][j] =
                minOf(distances[i - 1][j] + 1, distances[i][j - 1] + 1, distances[i - 1][j - 1] + cost)
            val transposed = i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
            if (transposed) {
                distances[i][j] = minOf(distances[i][j], distances[i - 2][j - 2] + 1)
            }
        }
    }
    return distances[a.length][b.length]
}

private const val SHORT_NAME_MAX_LENGTH = 5
private const val MEDIUM_NAME_MAX_LENGTH = 9

private val SEPARATORS = listOf(":", "|", " - ", " – ", " — ")

private val PREFIXES = listOf("banco", "meu", "app")

private val SUFFIXES =
    listOf(
        "token",
        "empresas",
        "empresa",
        "cartoes",
        "cartao",
        "pj",
        "app",
        "mobile",
        "oficial",
        "digital",
        "seguranca",
    )

/** Cada caractere da esquerda vira o da direita, na mesma posição. Depois das minúsculas. */
private val LOOKALIKE_LETTERS: Map<Char, Char> =
    listOf(
        // Dígitos e símbolos, e o l minúsculo, igual ao I maiúsculo em muitas fontes.
        "01!l34@5\$7" to "oiiieaasst",
        // Cirílico com forma de minúscula latina: а е о р с у х і ӏ ѕ ј.
        "аеорсухіӏѕј" to "aeopcyxiisj",
        // Cirílico que, em maiúscula, imita a latina (К М Н Т В): к м н т в.
        "кмнтв" to "kmhtb",
        // Grego (α ο ρ ι κ ν τ); ν e τ são as minúsculas de Ν e Τ, que imitam N e T.
        "αορικντ" to "aopiknt",
    ).flatMap { (from, to) -> from.zip(to) }.toMap()

private fun cutAtFirstSeparator(label: String): String {
    val cut = SEPARATORS.map { label.indexOf(it) }.filter { it > 0 }.minOrNull()
    return if (cut == null) label else label.substring(0, cut)
}

private fun variants(skeleton: String): Set<String> {
    val found = linkedSetOf<String>()
    val pending = ArrayDeque(listOf(skeleton) + PREFIXES.mapNotNull { skeleton.withoutPrefix(it) })
    while (pending.isNotEmpty()) {
        val variant = pending.removeFirst()
        if (found.add(variant)) SUFFIXES.mapNotNullTo(pending) { variant.withoutSuffix(it) }
    }
    return found
}

private fun String.withoutPrefix(prefix: String): String? =
    if (startsWith(prefix) && length > prefix.length) substring(prefix.length) else null

private fun String.withoutSuffix(suffix: String): String? =
    if (endsWith(suffix) && length > suffix.length) substring(0, length - suffix.length) else null

private fun tolerance(bankSkeleton: String): Int =
    when {
        bankSkeleton.length <= SHORT_NAME_MAX_LENGTH -> 0
        bankSkeleton.length <= MEDIUM_NAME_MAX_LENGTH -> 1
        else -> 2
    }
