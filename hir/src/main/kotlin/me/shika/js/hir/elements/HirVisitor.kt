package me.shika.js.hir.elements

interface HirVisitor<ContextType, DataType> {
    fun visitHirElement(hirElement: HirElement, data: ContextType): DataType

    fun visitHirFile(hirFile: HirFile, data: ContextType) =
        visitHirElement(hirFile, data)

    fun visitHirFunction(hirFunction: HirFunction, data: ContextType) =
        visitHirElement(hirFunction, data)

    fun visitHirParameter(hirParameter: HirParameter, data: ContextType) =
        visitHirElement(hirParameter, data)

    fun visitHirBody(hirBody: HirBody, data: ContextType) =
        visitHirElement(hirBody, data)

    fun visitHirVariable(hirVariable: HirVariable, data: ContextType) =
        visitHirElement(hirVariable, data)

    fun visitHirExpression(hirExpression: HirExpression, data: ContextType) =
        visitHirElement(hirExpression, data)

    fun visitHirConst(hirConst: HirConst, data: ContextType) =
        visitHirExpression(hirConst, data)

    fun visitHirReference(hirReference: HirReference, data: ContextType) =
        visitHirExpression(hirReference, data)

    fun visitHirCall(hirCall: HirCall, data: ContextType) =
        visitHirExpression(hirCall, data)
}
