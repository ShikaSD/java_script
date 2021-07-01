package me.shika.js.lexer

import com.intellij.psi.tree.IElementType
import me.shika.js.elements.JsLanguage

class JsToken(debugName: String) : IElementType(debugName, JsLanguage) {
    companion object {
        @JvmField
        val IDENTIFIER = JsToken("IDENTIFIER")

        // literals
        @JvmField
        val NUMBER_LITERAL = JsToken("NUMBER_LITERAL")
        @JvmField
        val STRING_LITERAL = JsToken("STRING_LITERAL")
        @JvmField
        val BOOLEAN_LITERAL = JsToken("BOOLEAN_LITERAL")

        // keywords
        @JvmField
        val FUNCTION_KEYWORD = JsToken("FUNCTION_KEYWORD")
        @JvmField
        val VAR_KEYWORD = JsToken("VAR_KEYWORD")
        @JvmField
        val IF_KEYWORD = JsToken("IF_KEYWORD")
        @JvmField
        val ELSE_KEYWORD = JsToken("ELSE_KEYWORD")
        @JvmField
        val RETURN_KEYWORD = JsToken("RETURN_KEYWORD")

        // symbols
        @JvmField
        val LPAR = JsToken("(")
        @JvmField
        val RPAR = JsToken(")")
        @JvmField
        val LBRACE = JsToken("{")
        @JvmField
        val RBRACE = JsToken("}")
        @JvmField
        val EQ = JsToken("=")
        @JvmField
        val SEMICOLON = JsToken(";")
        @JvmField
        val COMMA = JsToken(",")
        @JvmField
        val DOT = JsToken(".")
        
        // operators
        @JvmField
        val EQEQ = JsToken("==")

        // Other
        @JvmField
        val WHITESPACE = JsToken("WHITESPACE")
    }
}
