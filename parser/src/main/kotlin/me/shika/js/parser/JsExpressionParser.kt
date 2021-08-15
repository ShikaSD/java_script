package me.shika.js.parser

import com.intellij.lang.PsiBuilder
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsElementType.Companion.CALL
import me.shika.js.elements.JsElementType.Companion.EXPRESSION
import me.shika.js.elements.JsElementType.Companion.EXPRESSION_BINARY_OPERATOR
import me.shika.js.lexer.JsToken
import me.shika.js.lexer.JsToken.Companion.COMMA
import me.shika.js.lexer.JsToken.Companion.LPAR
import me.shika.js.lexer.JsToken.Companion.RPAR

/**
 * Expression parser based on [Pratt algorithm](https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html)
 */
class JsExpressionParser(override val psiBuilder: PsiBuilder) : ParserBase() {
    fun parseExpression(): Boolean =
        parseExpression(0)


    private fun parseExpression(minPrecedence: Int): Boolean {
        var expressionMark = psiBuilder.mark()

        if (at(LPAR)) {
            advance()
            parseExpression(0)

            if (!at(RPAR)) {
                error("Expected ')'")
            } else {
                advance()
            }
        } else {
            val atomic = parseAtomicExpression()
            if (!atomic) {
                expressionMark.drop()
                return false
            }
            expressionMark.done(EXPRESSION)
            expressionMark = expressionMark.precede()
        }

        while (!eof()) {
            val currentToken = current()

            val postfixPrecedence = postfixPrecedence[currentToken]
            if (postfixPrecedence != null) {
                val (lp, _) = postfixPrecedence
                if (lp < minPrecedence) {
                    break
                }

                val expressionType = when (currentToken) {
                    LPAR -> {
                        parseArgumentList()
                        CALL
                    }
                    else -> {
                        null
                    }
                }

                if (expressionType == null) {
                    expressionMark.error("Unknown expression start: $currentToken")
                    expressionMark = expressionMark.precede()
                    continue
                }

                expressionMark.done(expressionType)
                expressionMark = expressionMark.precede()
                expressionMark.done(EXPRESSION)
                expressionMark = expressionMark.precede()

                continue
            }

            val operatorMark = psiBuilder.mark()

            val binaryPrecedence = binaryPrecedence[currentToken]
            if (binaryPrecedence == null) {
                operatorMark.drop()
                break
            }

            val (lp, rp) = binaryPrecedence
            if (lp < minPrecedence) {
                operatorMark.drop()
                break
            }

            next()
            operatorMark.done(EXPRESSION_BINARY_OPERATOR)
            skipSpace()

            val parsedRhs = parseExpression(rp)
            if (!parsedRhs) {
                expressionMark.drop()
                return false
            }

            expressionMark.done(EXPRESSION)
            expressionMark = expressionMark.precede()
        }

        expressionMark.drop()

        return true
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

    private fun parseArgumentList() {
        val argumentListMark = startPsiElement(LPAR)

        while (!at(RPAR)) {
            val argumentMark = psiBuilder.mark()
            val result = parseExpression()

            if (result) {
                argumentMark.done(JsElementType.ARGUMENT)
            } else {
                argumentMark.error("Expected argument")
                advance()
            }

            if (at(COMMA)) {
                advance()
            }
        }

        expect(RPAR, "Expected \")\" after arguments", advance = true)
        argumentListMark.done(JsElementType.ARGUMENT_LIST)
    }

    private fun parseLiteral(): Boolean {
        when (current()) {
            JsToken.STRING_LITERAL -> singleToken(JsElementType.STRING_CONSTANT)
            JsToken.BOOLEAN_LITERAL -> singleToken(JsElementType.BOOLEAN_CONSTANT)
            JsToken.NUMBER_LITERAL -> singleToken(JsElementType.NUMBER_CONSTANT)
            else -> return false
        }

        return true
    }

    private fun parseReference(): Boolean {
        if (at(JsToken.IDENTIFIER)) {
            val mark = psiBuilder.mark()
            next()
            mark.done(JsElementType.REFERENCE)
            skipSpace()
            return true
        }
        return false
    }

    private fun parseObject(): Boolean {
        if (!at(JsToken.LBRACE)) return false

        val objectMark = psiBuilder.mark()
        advance()

        while (!at(JsToken.RBRACE)) {
            val objectClauseMark = psiBuilder.mark()

            val objectKeyMark = psiBuilder.mark()
            if (expect(JsToken.IDENTIFIER, "Expected object key", advance = true)) {
                objectKeyMark.done(JsElementType.OBJECT_KEY)
            } else {
                objectKeyMark.drop()
                objectClauseMark.drop()
                continue
            }

            expect(JsToken.COLON, "Expected colon between object key and value", advance = true)

            val objectValueMark = psiBuilder.mark()
            val parsed = parseExpression()
            if (!parsed) {
                error("Expected value for the object")
                objectValueMark.drop()
                objectClauseMark.drop()
                continue
            } else {
                objectValueMark.done(JsElementType.OBJECT_VALUE)
                objectClauseMark.done(JsElementType.OBJECT_CLAUSE)
            }

            skipSpace()

            if (at(COMMA)) {
                advance() // next clause
            } else {
                if (!at(JsToken.RBRACE)) {
                    error("Object clauses must be separated by comma")
                }
            }
        }
        advance()

        objectMark.done(JsElementType.OBJECT)

        return true
    }

    companion object {
        private const val UNSET = -1

        private val binaryPrecedence: Map<JsToken, Pair<Int, Int>> =
            mapOf(
                JsToken.EQ to Pair(2, 1),
                JsToken.DOT to Pair(5, 6)
            )

        private val postfixPrecedence: Map<JsToken, Pair<Int, Int>> =
            mapOf(
                LPAR to Pair(3, UNSET)
            )
    }
}


