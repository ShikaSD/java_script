package me.shika.js.hir

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType.BAD_CHARACTER
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import me.shika.js.ConstValue.Bool
import me.shika.js.ConstValue.Number
import me.shika.js.ConstValue.Str
import me.shika.js.SourceOffset
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsElementType.Companion.ARGUMENT
import me.shika.js.elements.JsElementType.Companion.ARGUMENT_LIST
import me.shika.js.elements.JsElementType.Companion.ASSIGNMENT
import me.shika.js.elements.JsElementType.Companion.ASSIGNMENT_VALUE
import me.shika.js.elements.JsElementType.Companion.BLOCK
import me.shika.js.elements.JsElementType.Companion.BOOLEAN_CONSTANT
import me.shika.js.elements.JsElementType.Companion.CALL
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
import me.shika.js.hir.elements.HirGetValue
import me.shika.js.hir.elements.HirObjectExpression
import me.shika.js.hir.elements.HirParameter
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
            SEMICOLON, // fixme: cheating here
            WHITESPACE -> null
            else -> convertExpression(astNode)
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

    private fun convertExpression(astNode: LighterASTNode): HirExpression? =
        when (astNode.tokenType) {
            STRING_CONSTANT ->
                HirConst(Str(astNode.toString().trim { it == '"' }), astNode.sourceOffset)
            NUMBER_CONSTANT ->
                HirConst(Number(astNode.toString().toDouble()), astNode.sourceOffset)
            BOOLEAN_CONSTANT ->
                HirConst(Bool(astNode.toString().toBooleanStrict()), astNode.sourceOffset)
            REFERENCE -> convertGetValue(astNode)
            CALL -> convertCall(astNode)
            OBJECT -> convertObjectExpression(astNode)
            ASSIGNMENT -> convertSetValue(astNode)
            BAD_CHARACTER -> {
                errorReporter.reportError(
                    "Found bad character",
                    astNode.sourceOffset
                )
                null
            }
            else -> {
                throw IllegalStateException("Unknown expression: ${astNode.tokenType}")
            }
        }

    private fun convertGetValue(astNode: LighterASTNode): HirGetValue =
        HirGetValue(
            name = astNode.toString(),
            source = astNode.sourceOffset
        )

    private fun convertCall(astNode: LighterASTNode): HirCall {
        val name = astNode.findOfType(REFERENCE)
        val arguments = convertArgumentList(astNode.findOfType(ARGUMENT_LIST)!!)

        return HirCall(
            name = name.toString(),
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

    private fun convertSetValue(astNode: LighterASTNode): HirSetValue {
        val name = astNode.findOfType(REFERENCE).toString()

        // fixme: error handling with !! is not fun
        val expression = convertExpression(astNode.findOfType(ASSIGNMENT_VALUE)!!.firstChild()!!)
        return HirSetValue(name, expression!!)
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
