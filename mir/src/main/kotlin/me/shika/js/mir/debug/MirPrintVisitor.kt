package me.shika.js.mir.debug

import me.shika.js.mir.elements.MirBody
import me.shika.js.mir.elements.MirCall
import me.shika.js.mir.elements.MirConst
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirGetValue
import me.shika.js.mir.elements.MirObjectExpression
import me.shika.js.mir.elements.MirParameter
import me.shika.js.mir.elements.MirSetValue
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirVariable
import me.shika.js.mir.elements.MirVisitor

class MirPrintVisitor : MirVisitor<StringBuilder> {
    private var indentation = 0

    override fun visitMirElement(element: MirElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }

    override fun visitMirFile(file: MirFile, data: StringBuilder) {
        data.indentedLine("FILE name: ${file.fileName}")

        withIndent {
            super.visitMirFile(file, data)
        }
    }

    override fun visitMirFunction(function: MirFunction, data: StringBuilder) {
        data.indentedLine("FUNCTION name: ${function.name}")

        withIndent {
            super.visitMirFunction(function, data)
        }
    }

    override fun visitMirParameter(parameter: MirParameter, data: StringBuilder) {
        data.indentedLine("PARAMETER name: ${parameter.name}")

        withIndent {
            super.visitMirParameter(parameter, data)
        }
    }

    override fun visitMirBody(body: MirBody, data: StringBuilder) {
        data.indentedLine("BODY")

        withIndent {
            super.visitMirBody(body, data)
        }
    }

    override fun visitMirVariable(variable: MirVariable, data: StringBuilder) {
        data.indentedLine("VAR name: ${variable.name}")

        withIndent {
            super.visitMirVariable(variable, data)
        }
    }

    override fun visitMirCall(call: MirCall, data: StringBuilder) {
        data.indentedLine("CALL: ${call.symbol.dump()}")

        withIndent {
            super.visitMirCall(call, data)
        }
    }

    override fun visitMirConst(const: MirConst, data: StringBuilder) {
        data.indentedLine("CONST: ${const.value}")

        withIndent {
            super.visitMirConst(const, data)
        }
    }

    override fun visitMirGetValue(getValue: MirGetValue, data: StringBuilder) {
        data.indentedLine("GET: ${getValue.symbol.dump()}")

        withIndent {
            super.visitMirGetValue(getValue, data)
        }
    }

    override fun visitMirSetValue(setValue: MirSetValue, data: StringBuilder) {
        data.indentedLine("SET: ${setValue.symbol.dump()}")

        withIndent {
            super.visitMirSetValue(setValue, data)
        }
    }

    override fun visitMirObjectExpression(objectExpression: MirObjectExpression, data: StringBuilder) {
        data.indentedLine("OBJECT:")

        withIndent {
            for (entry in objectExpression.entries) {
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

    private fun MirSymbol<*>.dump(): String {
        if (!isBound) {
            return "unbound symbol ${toString()}"
        }

        return when (val owner = owner) {
            is MirFunction -> "function ${owner.name}"
            is MirVariable -> "variable ${owner.name}"
            is MirParameter -> "parameter ${owner.name}"
            else -> toString()
        }
    }
}

fun MirElement.dump(): String =
    buildString {
        accept(MirPrintVisitor(), this)
    }
