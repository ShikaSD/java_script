package me.shika.js.hir

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType.BAD_CHARACTER
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsElementType.Companion.ARGUMENT
import me.shika.js.elements.JsElementType.Companion.ARGUMENT_LIST
import me.shika.js.elements.JsElementType.Companion.BLOCK
import me.shika.js.elements.JsElementType.Companion.FUNCTION
import me.shika.js.elements.JsElementType.Companion.PARAMETER
import me.shika.js.elements.JsElementType.Companion.PARAMETER_LIST
import me.shika.js.elements.JsElementType.Companion.VARIABLE
import me.shika.js.hir.elements.HirBody
import me.shika.js.hir.elements.HirCall
import me.shika.js.hir.elements.HirConst
import me.shika.js.hir.elements.HirConst.Value.Bool
import me.shika.js.hir.elements.HirConst.Value.Number
import me.shika.js.hir.elements.HirConst.Value.Str
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirExpression
import me.shika.js.hir.elements.HirFile
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirReference
import me.shika.js.hir.elements.HirSource
import me.shika.js.hir.elements.HirVariable
import me.shika.js.lexer.JsToken
import me.shika.js.lexer.JsToken.Companion.WHITESPACE

private typealias ASTTree = FlyweightCapableTreeStructure<LighterASTNode>

class AST2HirConverter(private val tree: ASTTree, private val errorReporter: HirErrorReporter) {
    fun convertFile(astNode: LighterASTNode, fileName: String): HirFile {
        return HirFile(
            fileName,
            astNode.getChildren().mapNotNull { if (it != null) convert(it) else null },
            astNode.hirSource
        )
    }

    fun convert(astNode: LighterASTNode): HirElement? =
        when (astNode.tokenType) {
            FUNCTION -> convertFunction(astNode)
            VARIABLE -> convertVariable(astNode)
            WHITESPACE -> null
            else -> convertExpression(astNode)
        }

    private fun convertFunction(astNode: LighterASTNode): HirFunction {
        val source = astNode.hirSource
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
                source = it.hirSource
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

        return HirBody(statements, astNode.hirSource)
    }

    private fun convertVariable(astNode: LighterASTNode): HirVariable {
        val nameNode = astNode.findOfType(JsToken.IDENTIFIER)

        return HirVariable(
            name = nameNode.toString(),
            value = astNode.findOfType(ARGUMENT)?.let { convertExpression(it.firstChild()!!) },
            source = astNode.hirSource
        )
    }

    private fun convertExpression(astNode: LighterASTNode): HirExpression? =
        when (astNode.tokenType) {
            JsElementType.STRING_CONSTANT ->
                HirConst(Str(astNode.toString()), astNode.hirSource)
            JsElementType.NUMBER_CONSTANT ->
                HirConst(Number(astNode.toString().toDouble()), astNode.hirSource)
            JsElementType.BOOLEAN_CONSTANT ->
                HirConst(Bool(astNode.toString().toBooleanStrict()), astNode.hirSource)
            JsElementType.REFERENCE -> convertReference(astNode)
            JsElementType.CALL -> convertCall(astNode)
            BAD_CHARACTER -> {
                errorReporter.reportError(
                    "Found bad character",
                    astNode.hirSource
                )
                null
            }
            else -> {
                throw IllegalStateException("Unknown expression: ${astNode.tokenType}")
            }
        }

    private fun convertReference(astNode: LighterASTNode): HirReference =
        HirReference(
            name = astNode.toString(),
            source = astNode.hirSource
        )

    private fun convertCall(astNode: LighterASTNode): HirCall {
        val name = astNode.findOfType(JsElementType.REFERENCE)
        val arguments = convertArgumentList(astNode.findOfType(ARGUMENT_LIST)!!)

        return HirCall(
            name = name.toString(),
            arguments = arguments,
            source = astNode.hirSource
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

    private val LighterASTNode.hirSource get() = HirSource(startOffset, endOffset)

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