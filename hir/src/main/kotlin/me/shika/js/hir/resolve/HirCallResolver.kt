package me.shika.js.hir.resolve

import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirFile
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirGetValue
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirSetValue
import me.shika.js.hir.elements.HirVariable
import me.shika.js.hir.elements.HirVisitor
import me.shika.js.hir.elements.functions

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class HirReferenceResolver(private val errorReporter: HirErrorReporter) : HirVisitor<Scope, Unit> {
    override fun visitHirFile(hirFile: HirFile, scope: Scope) {
        // Root level functions can be referenced before they are declared
        hirFile.functions.forEach { function -> scope.addElement(function.name, function) }

        super.visitHirFile(hirFile, scope)
    }

    override fun visitHirElement(hirElement: HirElement, scope: Scope) {
        hirElement.acceptChildren(this, scope)
    }

    override fun visitHirFunction(hirFunction: HirFunction, scope: Scope) {
        scope.addElement(hirFunction.name, hirFunction)
        super.visitHirFunction(hirFunction, scope.subScope())
    }

    override fun visitHirParameter(hirParameter: HirParameter, scope: Scope) {
        super.visitHirParameter(hirParameter, scope)
        scope.addElement(hirParameter.name, hirParameter)
    }

    override fun visitHirVariable(hirVariable: HirVariable, scope: Scope) {
        super.visitHirVariable(hirVariable, scope)
        scope.addElement(hirVariable.name, hirVariable) // we cannot reference variable in its initializer
    }

    override fun visitHirGetValue(hirGetValue: HirGetValue, scope: Scope) {
        val referent = scope.named(hirGetValue.name)
        if (referent == null) {
            errorReporter.reportError("Unknown reference to ${hirGetValue.name}", hirGetValue.source)
        }

        hirGetValue.candidate = referent
        super.visitHirGetValue(hirGetValue, scope)
    }

    override fun visitHirSetValue(hirSetValue: HirSetValue, scope: Scope) {
        val referent = scope.named(hirSetValue.name)
        if (referent == null) {
            errorReporter.reportError("Unknown reference to ${hirSetValue.name}", hirSetValue.source)
        }

        hirSetValue.candidate = referent
        super.visitHirSetValue(hirSetValue, scope)
    }
}
