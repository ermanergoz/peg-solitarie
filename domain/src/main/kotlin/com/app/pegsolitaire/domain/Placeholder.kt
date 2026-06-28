package com.app.pegsolitaire.domain

/**
 * Trivial placeholder so the pure-JVM `:domain` module compiles and has a
 * non-empty test source set from commit 0 (keeps `:domain:test` meaningful while
 * the gates are established). Replaced by the real immutable board/rules model
 * (`Board`, `BoardSpec`, `Move`, `GameState`, `RulesEngine`) in Plan 02.
 */
data class Placeholder(
    val name: String,
)
