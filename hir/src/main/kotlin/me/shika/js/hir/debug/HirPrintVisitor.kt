package me.shika.js.hir.debug

import me.shika.js.hir.elements.HirBody
import me.shika.js.hir.elements.HirCall
import me.shika.js.hir.elements.HirConst
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirFile
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirObjectExpression
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirReference
import me.shika.js.hir.elements.HirVariable
import me.shika.js.hir.elements.HirVisitor

class HirPrintVisitor : HirVisitor<StringBuilder, Unit> {
    private var indentation = 0

    override fun visitHirElement(hirElement: HirElement, data: StringBuilder) {
        hirElement.acceptChildren(this, data)
    }

    override fun visitHirFile(hirFile: HirFile, data: StringBuilder) {
        data.indentedLine("FILE name: ${hirFile.fileName}")

        withIndent {
            super.visitHirFile(hirFile, data)
        }
    }

    override fun visitHirFunction(hirFunction: HirFunction, data: StringBuilder) {
        data.indentedLine("FUNCTION name: ${hirFunction.name}")

        withIndent {
            super.visitHirFunction(hirFunction, data)
        }
    }

    override fun visitHirParameter(hirParameter: HirParameter, data: StringBuilder) {
        data.indentedLine("PARAMETER name: ${hirParameter.name}")

        withIndent {
            super.visitHirParameter(hirParameter, data)
        }
    }

    override fun visitHirBody(hirBody: HirBody, data: StringBuilder) {
        data.indentedLine("BODY")

        withIndent {
            super.visitHirBody(hirBody, data)
        }
    }

    override fun visitHirVariable(hirVariable: HirVariable, data: StringBuilder) {
        data.indentedLine("VAR name: ${hirVariable.name}")

        withIndent {
            super.visitHirVariable(hirVariable, data)
        }
    }

    override fun visitHirCall(hirCall: HirCall, data: StringBuilder) {
        data.indentedLine("CALL: ${hirCall.candidate?.dumpShallow() ?: "<unresolved ${hirCall.name}>"}")

        withIndent {
            super.visitHirCall(hirCall, data)
        }
    }

    override fun visitHirConst(hirConst: HirConst, data: StringBuilder) {
        data.indentedLine("CONST: ${hirConst.value}")

        withIndent {
            super.visitHirConst(hirConst, data)
        }
    }

    override fun visitHirReference(hirReference: HirReference, data: StringBuilder) {
        data.indentedLine("REF: ${hirReference.candidate?.dumpShallow() ?: "<unresolved ${hirReference.name}>"}")

        withIndent {
            super.visitHirReference(hirReference, data)
        }
    }

    override fun visitHirObjectExpression(hirObjectExpression: HirObjectExpression, data: StringBuilder) {
        data.indentedLine("OBJECT:")

        withIndent {
            for (entry in hirObjectExpression.entries) {
                data.indentedLine("KEY: ${entry.key}")
                data.indentedLine("VALUE:")

                withIndent {
                    entry.value.accept(this, data)
                }
            }
        }
    }

    private inline fun withIndent(block: () -> Unit) {
        indentation++
        block()
        indentation--
    }

    private fun StringBuilder.indentedLine(line: String) {
        repeat(indentation * 2) {
            append(' ')
        }
        appendLine(line)
    }

    private fun HirElement.dumpShallow(): String =
        when (this) {
            is HirFunction -> "function $name"
            is HirVariable -> "variable $name"
            is HirParameter -> "parameter $name"
            else -> toString()
        }
}

fun HirElement.dump(): String =
    buildString {
        accept(HirPrintVisitor(), this)
    }
