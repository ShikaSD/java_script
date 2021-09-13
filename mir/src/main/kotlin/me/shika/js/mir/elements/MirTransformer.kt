package me.shika.js.mir.elements

interface MirTransformer<Context> : MirVisitor<Context, MirElement> {
    override fun visitMirElement(element: MirElement, data: Context): MirElement {
        element.acceptChildren(this, data)
        return element
    }

    override fun visitMirFile(file: MirFile, data: Context): MirElement {
        file.statements = file.statements.transform(data)
        return file
    }

    override fun visitMirClass(cls: MirClass, data: Context): MirElement {
        cls.statements = cls.statements.transform(data)
        return cls
    }

    override fun visitMirFunction(function: MirFunction, data: Context): MirElement {
        function.parameters = function.parameters.transform(data)
        function.body = function.body.transform(data)
        return function
    }

    override fun visitMirBody(body: MirBody, data: Context): MirBody {
        body.statements = body.statements.transform(data)
        return body
    }

    override fun visitMirVariable(variable: MirVariable, data: Context): MirElement {
        variable.value = variable.value?.transform(data)
        return variable
    }

    override fun visitMirCall(call: MirCall, data: Context): MirElement {
        call.receiver = call.receiver.transform(data)
        call.arguments = call.arguments.transform(data)
        return call
    }

    override fun visitMirSetProperty(setProperty: MirSetProperty, data: Context): MirElement {
        setProperty.receiver = setProperty.receiver.transform(data)
        setProperty.value = setProperty.value.transform(data)
        return setProperty
    }

    override fun visitMirObjectExpression(objectExpression: MirObjectExpression, data: Context): MirElement {
        objectExpression.entries = objectExpression.entries.mapValues { it.value.transform(data) }
        return objectExpression
    }

    override fun visitMirSetValue(setValue: MirSetValue, data: Context): MirElement {
        setValue.value = setValue.value.transform(data)
        return setValue
    }

    override fun visitMirNewInstance(newInstance: MirNewInstance, data: Context): MirElement {
        newInstance.arguments = newInstance.arguments.transform(data)
        return newInstance
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : MirElement?> List<T>.transform(data: Context): List<T> =
        map { it?.transform(data) as T }

    @Suppress("UNCHECKED_CAST")
    private fun <T : MirElement> T.transform(data: Context): T =
        accept(this@MirTransformer, data) as T
}
