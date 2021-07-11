package me.shika.js.hir.resolve

import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.elements.HirCall
import me.shika.js.hir.elements.HirElement
import me.shika.js.hir.elements.HirFunction
import me.shika.js.hir.elements.HirParameter
import me.shika.js.hir.elements.HirReference
import me.shika.js.hir.elements.HirVariable
import me.shika.js.hir.elements.HirVisitor

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class HirReferenceResolver(private val errorReporter: HirErrorReporter) : HirVisitor<Scope, Unit> {
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

    override fun visitHirReference(hirReference: HirReference, scope: Scope) {
        val referent = scope.named(hirReference.name)
        if (referent == null) {
            errorReporter.reportError("Unknown reference to ${hirReference.name}", hirReference.source)
        }

        hirReference.candidate = referent
        super.visitHirReference(hirReference, scope)
    }

    override fun visitHirCall(hirCall: HirCall, scope: Scope) {
        val referent = scope.named(hirCall.name)
        if (referent == null) {
            errorReporter.reportError("Unknown call to ${hirCall.name}", hirCall.source)
        }

        hirCall.candidate = referent
        super.visitHirCall(hirCall, scope)
    }
}
