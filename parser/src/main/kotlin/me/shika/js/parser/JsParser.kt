package me.shika.js.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import me.shika.js.elements.JsElementType.Companion.ARGUMENT
import me.shika.js.elements.JsElementType.Companion.BLOCK
import me.shika.js.elements.JsElementType.Companion.FILE
import me.shika.js.elements.JsElementType.Companion.FUNCTION
import me.shika.js.elements.JsElementType.Companion.PARAMETER
import me.shika.js.elements.JsElementType.Companion.PARAMETER_LIST
import me.shika.js.elements.JsElementType.Companion.VARIABLE
import me.shika.js.lexer.JsToken
import me.shika.js.lexer.JsToken.Companion.COMMA
import me.shika.js.lexer.JsToken.Companion.EQ
import me.shika.js.lexer.JsToken.Companion.FUNCTION_KEYWORD
import me.shika.js.lexer.JsToken.Companion.IDENTIFIER
import me.shika.js.lexer.JsToken.Companion.LBRACE
import me.shika.js.lexer.JsToken.Companion.LPAR
import me.shika.js.lexer.JsToken.Companion.RBRACE
import me.shika.js.lexer.JsToken.Companion.RPAR
import me.shika.js.lexer.JsToken.Companion.SEMICOLON
import me.shika.js.lexer.JsToken.Companion.VAR_KEYWORD
import me.shika.js.lexer.JsToken.Companion.WHITESPACE

class JsParser : PsiParser {
    override fun parse(elementType: IElementType, builder: PsiBuilder): ASTNode =
        JsParsing(builder).parseFile()
}

private val DEFAULT_RECOVERY_SET = TokenSet.create(RPAR, RBRACE, SEMICOLON)

class JsParsing(override val psiBuilder: PsiBuilder) : ParserBase() {
    private val expressionParser = JsExpressionParser(psiBuilder)

    init {
        psiBuilder.setDebugMode(true)
    }

    fun parseFile(): ASTNode {
        val fileMarker = psiBuilder.mark()

        while (!eof()) {
            parseStatement()
        }

        fileMarker.done(FILE)

        return psiBuilder.treeBuilt
    }

    private fun parseStatement() {
        skipSpace()

        when {
            parseDeclaration() -> {
                // no-op
            }
            expressionParser.parseExpression() -> {
                expect(SEMICOLON, "Expected semicolon after the statement", advance = true)
            }
            else -> {
                error("Failed to parse statement", recovery = TokenSet.create(WHITESPACE, SEMICOLON))
            }
        }

        skipSpace()
    }

    private fun parseIdentifier(message: String, recovery: TokenSet = TokenSet.EMPTY): Boolean {
        return expect(IDENTIFIER, message, recovery = recovery, advance = true)
    }

    private fun parseDeclaration(): Boolean {
        skipSpace()

        if (at(FUNCTION_KEYWORD)) {
            parseFunction()
            return true
        }

        if (at(VAR_KEYWORD)) {
            parseVariable()
            expect(SEMICOLON, "Expected semicolon variable declaration", advance = true)
            return true
        }

        return false
    }

    private fun parseFunction() {
        val functionMark = startPsiElement(FUNCTION_KEYWORD)

        parseIdentifier("Expected function name", recovery = TokenSet.create(LPAR))

        expect(LPAR, "Expected \"(\" after function name")

        parseParameterList()

        expect(LBRACE, "Expected function body")

        parseBlock()

        functionMark.done(FUNCTION)
    }

    private fun parseVariable() {
        val variableMark = startPsiElement(VAR_KEYWORD)

        parseIdentifier("Expected variable name", recovery = TokenSet.create(EQ))

        if (at(EQ)) {
            advance()
            val argumentMark = psiBuilder.mark()
            val result = expressionParser.parseExpression()
            if (!result) {
                argumentMark.error("Expected variable value")
            } else {
                argumentMark.done(ARGUMENT)
            }
        }

        variableMark.done(VARIABLE)
    }

    private fun parseBlock() {
        val blockMark = startPsiElement(LBRACE)

        while (!at(RBRACE) && !eof()) {
            parseStatement()

            skipSpace()
        }

        expect(RBRACE, "Expected \"}\"", advance = true)
        blockMark.done(BLOCK)
    }

    private fun parseParameterList() {
        val valueParameterListMark = startPsiElement(LPAR)

        while (at(IDENTIFIER)) {
            val valueParameterMark = psiBuilder.mark()
            advance()
            valueParameterMark.done(PARAMETER)

            if (at(COMMA)) {
                advance()
            }
        }

        expect(RPAR, "Expected \")\" after parameter list", advance = true)
        valueParameterListMark.done(PARAMETER_LIST)
    }
}

abstract class ParserBase {
    protected abstract val psiBuilder: PsiBuilder

    protected fun singleToken(type: IElementType) {
        val mark = psiBuilder.mark()
        next()
        mark.done(type)
    }

    protected fun startPsiElement(requiredToken: JsToken): PsiBuilder.Marker {
        require(at(requiredToken)) { "Expected $requiredToken, but got ${current()}"}
        val mark = psiBuilder.mark()
        advance()
        return mark
    }

    protected fun expect(token: JsToken, message: String, advance: Boolean = false, recovery: TokenSet = DEFAULT_RECOVERY_SET): Boolean {
        if (at(token)) {
            if (advance) {
                advance()
            }
            return true
        }

        error(message, recovery)

        return false
    }

    protected fun error(message: String, recovery: TokenSet = DEFAULT_RECOVERY_SET)  {
        if (recovery.contains(current())) {
            psiBuilder.error(message)
        } else {
            psiBuilder.error(message)
            advance()
        }
    }

    protected fun eof() = psiBuilder.eof()

    protected fun current(): IElementType? = psiBuilder.tokenType
    protected fun at(type: IElementType): Boolean = current() == type
    protected fun advance() {
        next()
        skipSpace()
    }

    protected fun skipSpace() {
        while (at(WHITESPACE)) next()
    }

    protected fun next() {
        psiBuilder.advanceLexer()
    }
}
