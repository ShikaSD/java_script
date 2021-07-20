package me.shika.js.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsElementType.Companion.ARGUMENT
import me.shika.js.elements.JsElementType.Companion.ARGUMENT_LIST
import me.shika.js.elements.JsElementType.Companion.BLOCK
import me.shika.js.elements.JsElementType.Companion.BOOLEAN_CONSTANT
import me.shika.js.elements.JsElementType.Companion.FILE
import me.shika.js.elements.JsElementType.Companion.FUNCTION
import me.shika.js.elements.JsElementType.Companion.NUMBER_CONSTANT
import me.shika.js.elements.JsElementType.Companion.OBJECT
import me.shika.js.elements.JsElementType.Companion.OBJECT_CLAUSE
import me.shika.js.elements.JsElementType.Companion.OBJECT_KEY
import me.shika.js.elements.JsElementType.Companion.OBJECT_VALUE
import me.shika.js.elements.JsElementType.Companion.PARAMETER
import me.shika.js.elements.JsElementType.Companion.PARAMETER_LIST
import me.shika.js.elements.JsElementType.Companion.REFERENCE
import me.shika.js.elements.JsElementType.Companion.STRING_CONSTANT
import me.shika.js.elements.JsElementType.Companion.VARIABLE
import me.shika.js.lexer.JsToken
import me.shika.js.lexer.JsToken.Companion.BOOLEAN_LITERAL
import me.shika.js.lexer.JsToken.Companion.COLON
import me.shika.js.lexer.JsToken.Companion.COMMA
import me.shika.js.lexer.JsToken.Companion.EQ
import me.shika.js.lexer.JsToken.Companion.FUNCTION_KEYWORD
import me.shika.js.lexer.JsToken.Companion.IDENTIFIER
import me.shika.js.lexer.JsToken.Companion.LBRACE
import me.shika.js.lexer.JsToken.Companion.LPAR
import me.shika.js.lexer.JsToken.Companion.NUMBER_LITERAL
import me.shika.js.lexer.JsToken.Companion.RBRACE
import me.shika.js.lexer.JsToken.Companion.RPAR
import me.shika.js.lexer.JsToken.Companion.SEMICOLON
import me.shika.js.lexer.JsToken.Companion.STRING_LITERAL
import me.shika.js.lexer.JsToken.Companion.VAR_KEYWORD
import me.shika.js.lexer.JsToken.Companion.WHITESPACE

class JsParser : PsiParser {
    override fun parse(elementType: IElementType, builder: PsiBuilder): ASTNode =
        JsParsing(builder).parseFile()
}

private val DEFAULT_RECOVERY_SET = TokenSet.create(RPAR, RBRACE, SEMICOLON)

class JsParsing(private val psiBuilder: PsiBuilder) {
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

        if (parseExpression()) {
            // todo
        } else if (!parseDeclaration()) {
            error("Failed to parse statement", recovery = TokenSet.create(WHITESPACE, SEMICOLON))
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
            val result = parseAtomicExpression()
            if (!result) {
                argumentMark.error("Expected variable value")
            } else {
                argumentMark.done(ARGUMENT)
            }
        }

        if (at(SEMICOLON)) {
            advance()
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

    private fun parseAtomicExpression(): Boolean {
        if (parseReference()) {
            // TODO
        } else if (parseObject()) {
            // TODO
        } else if (!parseLiteral()) {
            return false
            //error("Unknown expression", TokenSet.create(EOL, SEMICOLON))
        }

        return true
    }

    private fun parseExpression(): Boolean {
        // Basically parses call now
        val callMark = psiBuilder.mark()

        val hasAtomic = parseAtomicExpression()
        if (!hasAtomic) {
            callMark.drop()
            return false
        }

        if (!at(LPAR)) {
            callMark.drop()
            return true
        }

        parseArgumentList()
        callMark.done(JsElementType.CALL)

        return true
    }

    private fun parseArgumentList() {
        val argumentListMark = startPsiElement(LPAR)

        while (!at(RPAR)) {
            val argumentMark = psiBuilder.mark()
            val result = parseAtomicExpression()

            if (result) {
                argumentMark.done(ARGUMENT)
            } else {
                argumentMark.error("Expected argument")
                advance()
            }

            if (at(COMMA)) {
                advance()
            }
        }

        expect(RPAR, "Expected \")\" after arguments", advance = true)
        argumentListMark.done(ARGUMENT_LIST)
    }

    private fun parseLiteral(): Boolean {
        when (current()) {
            STRING_LITERAL -> singleToken(STRING_CONSTANT)
            BOOLEAN_LITERAL -> singleToken(BOOLEAN_CONSTANT)
            NUMBER_LITERAL -> singleToken(NUMBER_CONSTANT)
            else -> return false
        }

        return true
    }

    private fun parseReference(): Boolean {
        if (at(IDENTIFIER)) {
            val mark = psiBuilder.mark()
            advance()
            mark.done(REFERENCE)
            return true
        }
        return false
    }

    private fun parseObject(): Boolean {
        if (!at(LBRACE)) return false

        val objectMark = psiBuilder.mark()
        advance()

        while (!at(RBRACE)) {
            val objectClauseMark = psiBuilder.mark()

            val objectKeyMark = psiBuilder.mark()
            if (expect(IDENTIFIER, "Expected object key", advance = true)) {
                objectKeyMark.done(OBJECT_KEY)
            } else {
                objectKeyMark.drop()
                objectClauseMark.drop()
                continue
            }

            expect(COLON, "Expected colon between object key and value", advance = true)

            val objectValueMark = psiBuilder.mark()
            val parsed = parseExpression()
            if (!parsed) {
                error("Expected value for the object")
                objectValueMark.drop()
                objectClauseMark.drop()
                continue
            } else {
                objectValueMark.done(OBJECT_VALUE)
                objectClauseMark.done(OBJECT_CLAUSE)
            }

            skipSpace()

            if (at(COMMA)) {
                advance() // next clause
            } else {
                if (!at(RBRACE)) {
                    error("Object clauses must be separated by comma")
                }
            }
        }
        advance()

        objectMark.done(OBJECT)

        return true
    }

    private fun startPsiElement(requiredToken: JsToken): PsiBuilder.Marker {
        require(at(requiredToken)) { "Expected $requiredToken, but got ${current()}"}
        val mark = psiBuilder.mark()
        advance()
        return mark
    }

    private fun singleToken(type: IElementType) {
        val mark = psiBuilder.mark()
        next()
        mark.done(type)
    }

    private fun expect(token: JsToken, message: String, advance: Boolean = false, recovery: TokenSet = DEFAULT_RECOVERY_SET): Boolean {
        if (at(token)) {
            if (advance) {
                advance()
            }
            return true
        }

        error(message, recovery)

        return false
    }

    private fun error(message: String, recovery: TokenSet = DEFAULT_RECOVERY_SET)  {
        if (recovery.contains(current())) {
            psiBuilder.error(message)
        } else {
            psiBuilder.error(message)
            advance()
        }
    }

    private fun eof() = psiBuilder.eof()

    private fun current(): IElementType? = psiBuilder.tokenType
    private fun at(type: IElementType): Boolean = current() == type
    private fun advance() {
        next()
        skipSpace()
    }

    private fun skipSpace() {
        while (at(WHITESPACE)) next()
    }

    private fun next() {
        psiBuilder.advanceLexer()
    }
}
