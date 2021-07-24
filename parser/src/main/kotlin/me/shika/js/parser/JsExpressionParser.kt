package me.shika.js.parser

import com.intellij.lang.PsiBuilder
import me.shika.js.elements.JsElementType
import me.shika.js.lexer.JsToken

/**
 * Expression parser based on [Pratt algorithm](https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html)
 */
class JsExpressionParser(override val psiBuilder: PsiBuilder) : ParserBase() {
    fun parseExpression(): Boolean {
        val expressionMark = psiBuilder.mark()

        val hasAtomic = parseAtomicExpression()
        if (!hasAtomic) {
            expressionMark.rollbackTo()
            return false
        }

        when (current()) {
            JsToken.LPAR -> {
                parseArgumentList()
                expressionMark.done(JsElementType.CALL)
                return true
            }
            JsToken.EQ -> {
                advance()
                val valueMark = psiBuilder.mark()
                val parsedValue = parseExpression()
                if (!parsedValue) {
                    valueMark.drop()
                    expressionMark.drop()

                    error("Couldn't parse value for the assignment")

                    return false
                }
                valueMark.done(JsElementType.ASSIGNMENT_VALUE)
                expressionMark.done(JsElementType.ASSIGNMENT)
                return true
            }
            JsToken.DOT -> {
                advance()
                val dotMark = psiBuilder.mark()
                val parsedValue = parseExpression()
                if (!parsedValue) {
                    dotMark.drop()
                    expressionMark.drop()

                    error("Couldn't parse dot access for value")

                    return false
                }

                dotMark.done(JsElementType.MEMBER_REF_VALUE)
                expressionMark.done(JsElementType.MEMBER_REF)
                return true
            }
            else -> {
                expressionMark.drop()
                return true
            }
        }
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
        val argumentListMark = startPsiElement(JsToken.LPAR)

        while (!at(JsToken.RPAR)) {
            val argumentMark = psiBuilder.mark()
            val result = parseAtomicExpression()

            if (result) {
                argumentMark.done(JsElementType.ARGUMENT)
            } else {
                argumentMark.error("Expected argument")
                advance()
            }

            if (at(JsToken.COMMA)) {
                advance()
            }
        }

        expect(JsToken.RPAR, "Expected \")\" after arguments", advance = true)
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

            if (at(JsToken.COMMA)) {
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
        private val binaryPrecendence: Map<JsToken, Pair<Int, Int>> =
            mapOf(
                JsToken.EQ to Pair(1, 2),
                JsToken.DOT to Pair(3, 4)
            )
    }
}


