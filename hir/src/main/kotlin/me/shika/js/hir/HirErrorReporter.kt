package me.shika.js.hir

import me.shika.js.hir.elements.HirSource

class HirErrorReporter {
    private val errors = mutableListOf<Error>()

    fun reportError(message: String, location: HirSource) {
        errors += Error(message, location)
    }

    fun getErrors(): List<Error> =
        errors
}

class Error(
    val text: String,
    val location: HirSource
)
