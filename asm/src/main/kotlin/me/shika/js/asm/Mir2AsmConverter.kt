package me.shika.js.asm

import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirVisitor

const val JOBJECT_NAME = "java/lang/Object"
const val JOBJECT_SIGNATURE = "L$JOBJECT_NAME;"
const val JSOBJECT_NAME = "js/JsObject"
const val JSFUNCTION_NAME = "js/JsFunction"
const val JSFUNCTION_INVOKE_SIGNATURE = "([${JOBJECT_SIGNATURE})V"

class Mir2AsmConverter {
    val mirClass2Asm = MirClass2Asm()

    fun convertFile(mirFile: MirFile): List<CompiledClass> {
        val classBytes = mutableListOf<CompiledClass>()

        mirFile.accept(TopLevelClassVisitor(classBytes), null)

        return classBytes
    }

    inner class TopLevelClassVisitor(private val classBytes: MutableList<CompiledClass>) : MirVisitor<Nothing?, Unit> {
        override fun visitMirElement(element: MirElement, data: Nothing?) {
            element.acceptChildren(this, data)
        }

        override fun visitMirClass(cls: MirClass, data: Nothing?) {
            val classScope = ClassScope(cls, cls.name)
            mirClass2Asm.visitMirClass(cls, classScope)
            classBytes += classScope.compiled()
        }
    }
}

class CompiledClass(
    val name: String,
    val bytes: ByteArray
)
