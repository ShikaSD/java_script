package me.shika.js.mir

import me.shika.js.hir.builtins.BuiltIns
import me.shika.js.hir.debug.dump
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
import me.shika.js.hir.elements.HirVisitor
import me.shika.js.hir.elements.functions
import me.shika.js.mir.elements.MirBody
import me.shika.js.mir.elements.MirCall
import me.shika.js.mir.elements.MirConst
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirExpression
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirFunctionSymbol
import me.shika.js.mir.elements.MirGetProperty
import me.shika.js.mir.elements.MirGetValue
import me.shika.js.mir.elements.MirObjectExpression
import me.shika.js.mir.elements.MirParameter
import me.shika.js.mir.elements.MirParameterSymbol
import me.shika.js.mir.elements.MirSetProperty
import me.shika.js.mir.elements.MirSetValue
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirVariable
import me.shika.js.mir.elements.MirVariableSymbol
import me.shika.js.mir.util.patchParents

class Hir2MirConverter {
    private val symbolTable = Hir2MirSymbolTable()
    private val visitor = Hir2MirVisitor()

    init {
        val function = visitor.visitHirFunction(BuiltIns.Print, null)
        function.patchParents(BuiltInsFile)
    }

    fun convertFile(hirFile: HirFile): MirFile =
        visitor.visitHirFile(hirFile, null).also {
            it.patchParents(null)
        }

    private inner class Hir2MirVisitor : HirVisitor<Nothing?, MirElement?> {
        override fun visitHirElement(hirElement: HirElement, data: Nothing?): MirElement? {
            hirElement.acceptChildren(this, null)
            return null
        }

        override fun visitHirFile(hirFile: HirFile, data: Nothing?): MirFile {
            // Top level functions can be referenced before declared
            hirFile.functions.forEach { symbolTable.declareFunctionSymbol(it) }

            return MirFile(
                fileName = hirFile.fileName,
                statements = hirFile.statements.mapNotNull { it.accept(this, null) },
                source = hirFile.source
            )
        }

        override fun visitHirFunction(hirFunction: HirFunction, data: Nothing?): MirFunction =
            MirFunction(
                symbol = symbolTable.declareFunctionSymbol(hirFunction),
                name = hirFunction.name,
                parameters = hirFunction.parameters.map { visitHirParameter(it, null) },
                body = visitHirBody(hirFunction.body, null),
                source = hirFunction.source
            )

        override fun visitHirParameter(hirParameter: HirParameter, data: Nothing?): MirParameter =
            MirParameter(
                symbol = symbolTable.declareParameterSymbol(hirParameter),
                name = hirParameter.name,
                source = hirParameter.source
            )

        override fun visitHirBody(hirBody: HirBody, data: Nothing?): MirBody =
            MirBody(
                statements = hirBody.statements.map { it.accept(this, null)!! },
                source = hirBody.source
            )

        override fun visitHirVariable(hirVariable: HirVariable, data: Nothing?): MirVariable =
            MirVariable(
                symbol = symbolTable.declareVariableSymbol(hirVariable),
                name = hirVariable.name,
                value = hirVariable.value?.let { it.accept(this, null) as MirExpression },
                source = hirVariable.source
            )

        override fun visitHirExpression(hirExpression: HirExpression, data: Nothing?): MirExpression {
            return super.visitHirExpression(hirExpression, data) as MirExpression
        }

        override fun visitHirCall(hirCall: HirCall, data: Nothing?): MirCall =
            MirCall(
                receiver = hirCall.receiver.accept(this, null) as MirExpression, // TODO
                arguments = hirCall.arguments.map { arg ->
                    arg?.let { it.accept(this, null) as MirExpression }
                },
                source = hirCall.source
            )

        override fun visitHirGetValue(hirGetValue: HirGetValue, data: Nothing?): MirGetValue =
            MirGetValue(
                symbol = symbolTable.referenceSymbol(hirGetValue.candidate!!),
                source = hirGetValue.source
            )

        override fun visitHirSetProperty(hirSetProperty: HirSetProperty, data: Nothing?): MirSetProperty =
            MirSetProperty(
                receiver = hirSetProperty.receiver.mir(),
                name = hirSetProperty.property,
                value = hirSetProperty.value.mir(),
                source = hirSetProperty.source
            )

        override fun visitHirGetProperty(hirGetProperty: HirGetProperty, data: Nothing?): MirGetProperty =
            MirGetProperty(
                receiver = hirGetProperty.receiver.mir(),
                name = hirGetProperty.property,
                source = hirGetProperty.source
            )

        override fun visitHirSetValue(hirSetValue: HirSetValue, data: Nothing?): MirSetValue =
            MirSetValue(
                symbol = symbolTable.referenceSymbol(hirSetValue.candidate!!),
                value = hirSetValue.value.mir(),
                source = hirSetValue.source
            )

        override fun visitHirObjectExpression(hirObjectExpression: HirObjectExpression, data: Nothing?): MirObjectExpression =
            MirObjectExpression(
                entries = hirObjectExpression.entries.mapValues {
                    it.value.mir()
                },
                source = hirObjectExpression.source
            )

        override fun visitHirConst(hirConst: HirConst, data: Nothing?): MirConst =
            MirConst(
                value = hirConst.value,
                source = hirConst.source
            )

        private fun HirExpression.mir(): MirExpression =
            accept(this@Hir2MirVisitor, null) as MirExpression
    }

    class Hir2MirSymbolTable {
        private val symbols = hashMapOf<HirElement, MirSymbol<*>>()

        fun declareFunctionSymbol(function: HirFunction): MirFunctionSymbol =
            symbols.getOrPut(function) { MirFunctionSymbol() } as MirFunctionSymbol

        fun declareParameterSymbol(parameter: HirParameter): MirParameterSymbol =
            symbols.getOrPut(parameter) { MirParameterSymbol() } as MirParameterSymbol

        fun declareVariableSymbol(variable: HirVariable): MirVariableSymbol =
            symbols.getOrPut(variable) { MirVariableSymbol() } as MirVariableSymbol

        fun referenceSymbol(element: HirElement): MirSymbol<*> =
            symbols.getOrElse(element) {
                throw IllegalStateException("Symbol not defined: ${element.dump()}")
            }
    }
}
