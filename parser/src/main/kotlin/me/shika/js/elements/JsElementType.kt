package me.shika.js.elements

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class JsElementType(debugName: String) : IElementType(debugName, JsLanguage) {
    companion object {
        val FILE = IFileElementType(JsLanguage)

        val FUNCTION = jsElement("FUNCTION")
        val VARIABLE = jsElement("VARIABLE")

        val ARGUMENT_LIST = jsElement("ARGUMENT_LIST")
        val PARAMETER_LIST = jsElement("PARAMETER_LIST")
        val BLOCK = jsElement("BLOCK")

        val CALL = jsElement("CALL")
        val DOT_CALL = jsElement("DOT_CALL")

        val PARAMETER = jsElement("PARAMETER")
        val ARGUMENT = jsElement("ARGUMENT")
        val REFERENCE = jsElement("REFERENCE")

        val OBJECT = jsElement("OBJECT")
        val OBJECT_CLAUSE = jsElement("OBJECT_CLAUSE")
        val OBJECT_KEY = jsElement("OBJECT_KEY")
        val OBJECT_VALUE = jsElement("OBJECT_VALUE")

        val STRING_CONSTANT = jsElement("STRING_CONSTANT")
        val BOOLEAN_CONSTANT = jsElement("BOOLEAN_CONSTANT")
        val NUMBER_CONSTANT = jsElement("NUMBER_CONSTANT")

        private fun jsElement(debugName: String) = JsElementType(debugName)
    }
}
