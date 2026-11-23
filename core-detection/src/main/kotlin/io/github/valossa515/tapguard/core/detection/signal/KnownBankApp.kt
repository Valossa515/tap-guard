package io.github.valossa515.tapguard.core.detection.signal

/**
 * Um app de banco conhecido, ou um apelido dele, para o sinal S9 (ADR-020).
 *
 * [displayName] é o nome da marca que o S9 compara com o rótulo do app, escrito à mão a partir do
 * título da Play ou do nome do desenvolvedor. Não é o rótulo do aparelho, que fica para conferir no
 * teste ponta a ponta (SPEC-022). [packageName] é o pacote oficial, comparado exato, com maiúsculas e
 * minúsculas.
 *
 * Um pacote pode aparecer em mais de uma entrada, uma por nome: o app do BB é "BB" e também "Banco
 * do Brasil". Um banco com vários apps tem uma entrada por app.
 *
 * Nome ou pacote em branco lança [IllegalArgumentException].
 */
data class KnownBankApp(
    val displayName: String,
    val packageName: String,
) {
    init {
        require(displayName.isNotBlank()) { "O nome de exibição não pode ficar em branco" }
        require(packageName.isNotBlank()) { "O nome do pacote não pode ficar em branco" }
    }
}
