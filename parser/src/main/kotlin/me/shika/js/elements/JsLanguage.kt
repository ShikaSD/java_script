package me.shika.js.elements

import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.application.ApplicationManager

object JsLanguage : Language("JS") {
    override fun getDisplayName(): String =
        "JS"

    override fun isCaseSensitive(): Boolean =
        true

    override fun toString(): String =
        "JS"
}
