package io.github.valossa515.tapguard.core.domain

import java.util.Collections

/**
 * Cópia que não muda quando a origem muda e que recusa alteração, inclusive por cast para
 * [MutableSet]. `Set.copyOf` não serve: só existe no Android a partir da API 30.
 */
internal fun <T> Set<T>.unmodifiableCopy(): Set<T> = Collections.unmodifiableSet(LinkedHashSet(this))

/**
 * Cópia que não muda quando a origem muda e que recusa alteração, inclusive por cast para
 * [MutableList]. `List.copyOf` não serve: só existe no Android a partir da API 30.
 */
internal fun <T> List<T>.unmodifiableCopy(): List<T> = Collections.unmodifiableList(ArrayList(this))
