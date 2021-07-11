package me.shika.js.hir

import me.shika.js.SourceOffset

class HirErrorReporter {
    private val errors = mutableListOf<HirError>()

    fun reportError(message: String, location: SourceOffset) {
        errors += HirError(message, location)
    }

    fun getErrors(): List<HirError> =
        errors
}

data class HirError(
    val text: String,
    val location: SourceOffset
)
