package me.shika.js.mir.elements

interface MirVisitor<Context> {
    fun visitMirElement(element: MirElement, data: Context)

    fun visitMirFile(file: MirFile, data: Context) =
        visitMirElement(file, data)

    fun visitMirFunction(function: MirFunction, data: Context) =
        visitMirElement(function, data)

    fun visitMirParameter(parameter: MirParameter, data: Context) =
        visitMirElement(parameter, data)

    fun visitMirBody(body: MirBody, data: Context) =
        visitMirElement(body, data)

    fun visitMirVariable(variable: MirVariable, data: Context) =
        visitMirElement(variable, data)

    fun visitMirExpression(expression: MirExpression, data: Context) =
        visitMirElement(expression, data)

    fun visitMirConst(const: MirConst, data: Context) =
        visitMirExpression(const, data)

    fun visitMirGetValue(getValue: MirGetValue, data: Context) =
        visitMirExpression(getValue, data)

    fun visitMirSetValue(setValue: MirSetValue, data: Context) =
        visitMirExpression(setValue, data)

    fun visitMirGetProperty(getProperty: MirGetProperty, data: Context) =
        visitMirExpression(getProperty, data)

    fun visitMirSetProperty(setProperty: MirSetProperty, data: Context) =
        visitMirExpression(setProperty, data)

    fun visitMirObjectExpression(objectExpression: MirObjectExpression, data: Context) =
        visitMirExpression(objectExpression, data)

    fun visitMirCall(call: MirCall, data: Context) =
        visitMirExpression(call, data)
}
