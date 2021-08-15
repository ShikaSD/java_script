package me.shika.js.hir

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import me.shika.js.ConstValue.Bool
import me.shika.js.ConstValue.Number
import me.shika.js.ConstValue.Str
import me.shika.js.SourceOffset
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsElementType.Companion.ARGUMENT
import me.shika.js.elements.JsElementType.Companion.ARGUMENT_LIST
import me.shika.js.elements.JsElementType.Companion.BLOCK
import me.shika.js.elements.JsElementType.Companion.BOOLEAN_CONSTANT
import me.shika.js.elements.JsElementType.Companion.CALL
import me.shika.js.elements.JsElementType.Companion.EXPRESSION
import me.shika.js.elements.JsElementType.Companion.EXPRESSION_BINARY_OPERATOR
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
import me.shika.js.hir.elements.HirBody
import me.shika.js.hir.elements.HirCall
import me.shika.js.hir.elements.HirConst
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirExpression
import me.shika.js.hir.elements.HirFile
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirGetProperty
import me.shika.js.hir.elements.HirGetValue
import me.shika.js.hir.elements.HirObjectExpression
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirSetProperty
import me.shika.js.hir.elements.HirSetValue
import me.shika.js.hir.elements.HirVariable
import me.shika.js.lexer.JsToken
import me.shika.js.lexer.JsToken.Companion.SEMICOLON
import me.shika.js.lexer.JsToken.Companion.WHITESPACE

private typealias ASTTree = FlyweightCapableTreeStructure<LighterASTNode>

class AST2HirConverter(private val tree: ASTTree, private val errorReporter: HirErrorReporter) {
    fun convertFile(astNode: LighterASTNode, fileName: String): HirFile {
        return HirFile(
            fileName,
            astNode.getChildren().mapNotNull { if (it != null) convert(it) else null },
            astNode.sourceOffset
        )
    }

    fun convert(astNode: LighterASTNode): HirElement? =
        when (astNode.tokenType) {
            FUNCTION -> convertFunction(astNode)
            VARIABLE -> convertVariable(astNode)
            EXPRESSION -> convertExpression(astNode)
            SEMICOLON, // fixme: cheating here
            WHITESPACE -> null
            else -> {
                errorReporter.reportError("Unexpected ${astNode.tokenType}", astNode.sourceOffset)
                null
            }
        }

    private fun convertFunction(astNode: LighterASTNode): HirFunction {
        val source = astNode.sourceOffset
        val name = astNode.findOfType(JsToken.IDENTIFIER)

        if (name == null) {
            errorReporter.reportError(
                "Expected function name", source
            )
        }

        return HirFunction(
            name = name.toString(),
            parameters = convertParameters(astNode.findOfType(PARAMETER_LIST)!!),
            body = convertBody(astNode.findOfType(BLOCK)!!),
            source = source
        )
    }

    private fun convertParameters(astNode: LighterASTNode): List<HirParameter> {
        require(astNode.tokenType == PARAMETER_LIST) {
            "Expected node type to be PARAMETER_LIST"
        }

        val result = mutableListOf<HirParameter>()
        astNode.forEachChildren {
            if (it?.tokenType != PARAMETER) return@forEachChildren

            val hirParameter = HirParameter(
                name = it.firstChild().toString(),
                source = it.sourceOffset
            )
            result += hirParameter
        }

        return result
    }

    private fun convertBody(astNode: LighterASTNode): HirBody {
        require(astNode.tokenType == BLOCK) {
            "Expected node type to be BLOCK"
        }

        val statements = mutableListOf<HirElement>()
        astNode.forEachChildren {
            if (it?.tokenType !is JsElementType) return@forEachChildren

            val element = convert(it)
            if (element != null) {
                statements += element
            }
        }

        return HirBody(statements, astNode.sourceOffset)
    }

    private fun convertVariable(astNode: LighterASTNode): HirVariable {
        val nameNode = astNode.findOfType(JsToken.IDENTIFIER)

        return HirVariable(
            name = nameNode.toString(),
            value = astNode.findOfType(ARGUMENT)?.let { convertExpression(it.firstChild()!!) },
            source = astNode.sourceOffset
        )
    }

    private fun convertExpression(astNode: LighterASTNode): HirExpression? {
        // binary expression
        val binaryOperator = astNode.findOfType(EXPRESSION_BINARY_OPERATOR)
        if (binaryOperator != null) {
            val operatorToken = binaryOperator.firstChild()?.tokenType
            return when (operatorToken) {
                JsToken.EQ -> convertSetValue(astNode)
                JsToken.DOT -> convertGetProperty(astNode)
                else -> {
                    errorReporter.reportError("Unknown ${operatorToken} for binary expression", astNode.sourceOffset)
                    null
                }
            }
        }

        // single node expression
        val expressionValue = astNode.firstChild()
        if (expressionValue == null) {
            errorReporter.reportError("Unexpected ${astNode.tokenType}, expected expression", astNode.sourceOffset)
            return null
        }
        return when (expressionValue.tokenType) {
            STRING_CONSTANT ->
                HirConst(Str(expressionValue.toString().trim { it == '"' }), astNode.sourceOffset)
            NUMBER_CONSTANT ->
                HirConst(Number(expressionValue.toString().toDouble()), astNode.sourceOffset)
            BOOLEAN_CONSTANT ->
                HirConst(Bool(expressionValue.toString().toBooleanStrict()), astNode.sourceOffset)
            REFERENCE -> convertGetValue(expressionValue)
            OBJECT -> convertObjectExpression(expressionValue)
            CALL -> convertCall(expressionValue)
            else -> throw IllegalStateException("Unknown singular token expression ${astNode.tokenType}")
        }
    }

    private fun convertGetValue(astNode: LighterASTNode): HirGetValue =
        HirGetValue(
            name = astNode.toString(),
            source = astNode.sourceOffset
        )

    private fun convertCall(astNode: LighterASTNode): HirCall {
        val receiver = convertExpression(astNode.findOfType(EXPRESSION)!!)
        val arguments = convertArgumentList(astNode.findOfType(ARGUMENT_LIST)!!)

        return HirCall(
            receiver = receiver!!,
            arguments = arguments,
            source = astNode.sourceOffset
        )
    }

    private fun convertArgumentList(astNode: LighterASTNode): List<HirExpression?> {
        val result = mutableListOf<HirExpression?>()

        astNode.forEachChildren {
            if (it?.tokenType != ARGUMENT) return@forEachChildren

            result += convertExpression(it.firstChild()!!)
        }

        return result
    }

    private fun convertObjectExpression(astNode: LighterASTNode): HirObjectExpression {
        val entries = mutableMapOf<String, HirExpression>()

        astNode.forEachChildren {
            if (it?.tokenType != OBJECT_CLAUSE) return@forEachChildren

            val key = it.findOfType(OBJECT_KEY)!!
            val value = it.findOfType(OBJECT_VALUE)!!

            val valueExpression = convertExpression(value.firstChild()!!)

            if (valueExpression != null) {
                entries[key.toString()] = valueExpression
            } else {
                errorReporter.reportError("Unknown expression", value.sourceOffset)
            }
        }

        return HirObjectExpression(entries, astNode.sourceOffset)
    }

    private fun convertSetValue(astNode: LighterASTNode): HirExpression {
        val children = astNode.getChildren().filter { it?.tokenType == EXPRESSION }
        require(children.size == 2) { "requires 2 expressions for assignment" }

        val (receiver, value) = children.map {
            val expression = convertExpression(it!!)
            require (expression != null) { "Failed to convert expression $it" }
            expression
        }

        return if (receiver is HirGetProperty) {
            HirSetProperty(
                receiver = receiver.receiver,
                property = receiver.property,
                value = value,
                source = astNode.sourceOffset
            )
        } else {
            HirSetValue(receiver, value, astNode.sourceOffset)
        }
    }

    private fun convertGetProperty(astNode: LighterASTNode): HirGetProperty {
        val children = astNode.getChildren().filter { it?.tokenType == EXPRESSION }
        require(children.size == 2) { "requires 2 expressions for assignment" }

        val (receiver, property) = children.map { convertExpression(it!!) }

        require(receiver is HirExpression) { "requires receiver to be an expression" }
        require(property is HirGetValue) { "requires access to be a reference" }

        return HirGetProperty(receiver, children[1].toString(), astNode.sourceOffset)
    }

    private val LighterASTNode.sourceOffset get() = SourceOffset(startOffset, endOffset)

    private fun LighterASTNode.getChildren(): Array<LighterASTNode?> {
        val ref = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, ref)
        return ref.get()
    }

    private fun LighterASTNode.findOfType(type: IElementType): LighterASTNode? =
        getChildren().find { it?.tokenType == type }

    private fun LighterASTNode.firstChild(): LighterASTNode? =
        getChildren().firstOrNull()

    private fun LighterASTNode.forEachChildren(lambda: (LighterASTNode?) -> Unit) {
        val children = getChildren()
        children.forEach(lambda)
    }
}
