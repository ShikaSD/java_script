package me.shika.js.asm

import me.shika.js.ConstValue
import me.shika.js.mir.BuiltInsFile
import me.shika.js.mir.debug.dump
import me.shika.js.mir.elements.MirBody
import me.shika.js.mir.elements.MirCall
import me.shika.js.mir.elements.MirConst
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirElementWithParent
import me.shika.js.mir.elements.MirExpression
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirGetValue
import me.shika.js.mir.elements.MirObjectExpression
import me.shika.js.mir.elements.MirParameter
import me.shika.js.mir.elements.MirParameterSymbol
import me.shika.js.mir.elements.MirSetValue
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirVariable
import me.shika.js.mir.elements.MirVariableSymbol
import me.shika.js.mir.elements.MirVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.PUTSTATIC
import org.objectweb.asm.Opcodes.RETURN

private const val VERSION = Opcodes.V1_8

private const val JOBJECT_NAME = "java/lang/Object"
private const val JOBJECT_SIGNATURE = "L$JOBJECT_NAME;"

private const val JSOBJECT_NAME = "js/JsObject"

private const val JS_ACC_CONST = ACC_PUBLIC or ACC_STATIC or ACC_FINAL

class Mir2AsmConverter {
    fun convertFile(mirFile: MirFile, classVisitor: ClassVisitor) =
        MirFile2AsmClass(classVisitor).visitMirFile(mirFile, null)

    class MirFile2AsmClass(private val classVisitor: ClassVisitor) : MirVisitor<Nothing?> {
        private val staticInitVisitor: MethodVisitor by lazy {
            val visitor = classVisitor.visitMethod(
                ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                emptyArray()
            )
            visitor.visitCode()
            visitor
        }

        override fun visitMirElement(element: MirElement, data: Nothing?) {
            element.acceptChildren(this, null)
        }

        override fun visitMirFile(file: MirFile, data: Nothing?) {
            classVisitor.visit(
                VERSION,
                ACC_PUBLIC,
                file.className(),
                null,
                JOBJECT_NAME,
                emptyArray()
            )
            super.visitMirFile(file, null)

            staticInitVisitor.visitInsn(RETURN)
            staticInitVisitor.visitMaxs(-1, -1)
            staticInitVisitor.visitEnd()

            classVisitor.visitEnd()
        }

        override fun visitMirFunction(function: MirFunction, data: Nothing?) {
            val methodVisitor = classVisitor.visitMethod(
                JS_ACC_CONST,
                function.name,
                function.descriptor(),
                null,
                null
            )

            val methodBuilder = Mir2AsmMethod(function)
            methodBuilder.visitMirFunction(function, methodVisitor)

            methodVisitor.visitEnd()
        }

        override fun visitMirVariable(variable: MirVariable, data: Nothing?) {
            val field = classVisitor.visitField(
                ACC_STATIC or ACC_PUBLIC,
                variable.name,
                JOBJECT_SIGNATURE,
                null,
                null
            )
            field.visitEnd()

            if (variable.value != null) {
                variable.value?.accept(
                    Mir2AsmMethod(null), staticInitVisitor
                )
                staticInitVisitor.visitFieldInsn(
                    PUTSTATIC,
                    (variable.parent as MirFile).className(),
                    variable.name,
                    JOBJECT_SIGNATURE
                )
            }
        }

        override fun visitMirExpression(expression: MirExpression, data: Nothing?) {
            expression.accept(
                Mir2AsmMethod(null), staticInitVisitor
            )
        }
    }

    private class FrameMap(private val shift: Int) {
        private val symbols = arrayListOf<MirSymbol<*>>()

        fun define(symbol: MirSymbol<*>): Int {
            if (symbol !in symbols) {
                symbols.add(symbol)
            }
            return localIndex(symbol)
        }

        fun localIndex(symbol: MirSymbol<*>): Int {
            val symbolIndex = symbols.indexOf(symbol)
            return if (symbolIndex != -1) {
                symbolIndex + shift
            } else {
                -1
            }
        }
    }

    class Mir2AsmMethod(private val currentFunction: MirFunction?) : MirVisitor<MethodVisitor> {
        private val startLabel = Label()
        private val endLabel = Label()
        private var frameMap = FrameMap(0)

        override fun visitMirElement(element: MirElement, data: MethodVisitor) {
            element.acceptChildren(this, data)
        }

        override fun visitMirBody(body: MirBody, data: MethodVisitor) {
            data.visitCode()
            data.visitLabel(startLabel)

            super.visitMirBody(body, data)

            data.visitLabel(endLabel)
            data.visitInsn(RETURN)

            data.visitMaxs(-1, -1)
            data.visitEnd()
        }

        override fun visitMirParameter(parameter: MirParameter, data: MethodVisitor) {
            val index = frameMap.define(parameter.symbol)
            data.visitLocalVariable(
                parameter.name,
                JOBJECT_SIGNATURE,
                null,
                startLabel,
                endLabel,
                index,
            )
            super.visitMirParameter(parameter, data)
        }

        override fun visitMirVariable(variable: MirVariable, data: MethodVisitor) {
            val variableIndex = frameMap.define(variable.symbol)
            data.visitLocalVariable(
                variable.name,
                JOBJECT_SIGNATURE,
                null,
                startLabel,
                endLabel,
                variableIndex
            )
            super.visitMirVariable(variable, data)
            if (variable.value != null) {
                data.visitVarInsn(ASTORE, variableIndex)
            }
        }

        override fun visitMirGetValue(getValue: MirGetValue, data: MethodVisitor) {
            val symbol = getValue.symbol

            require(symbol is MirVariableSymbol || symbol is MirParameterSymbol) {
                "Can only reference variables and parameters, found ${getValue.dump()}"
            }

            if (symbol.owner is MirElementWithParent) {
                val parent = (symbol.owner as MirElementWithParent).parent
                if (parent is MirFile) {
                    // top of the file, use getstatic instead
                    data.visitFieldInsn(
                        GETSTATIC,
                        parent.className(),
                        (symbol.owner as MirVariable).name,
                        JOBJECT_SIGNATURE
                    )
                    return
                }
            }

            val index = frameMap.localIndex(symbol)
            if (index == -1) {
                "Could not find ${getValue.dump()} in the frame map"
            }

            data.visitVarInsn(ALOAD, index)
        }

        override fun visitMirSetValue(setValue: MirSetValue, data: MethodVisitor) {
            val symbol = setValue.symbol

            require(symbol is MirVariableSymbol) {
                "Can only set value to variables, found ${setValue.dump()}"
            }

            // put value on stack
            setValue.value.accept(this, data)
            // leave it on the stack after assignment
            data.visitInsn(DUP)

            val parent = (symbol.owner as MirElementWithParent).parent
            if (parent is MirFile) {
                // top of the file, use putstatic instead
                data.visitFieldInsn(
                    PUTSTATIC,
                    parent.className(),
                    symbol.owner.name,
                    JOBJECT_SIGNATURE
                )
                return
            }

            val index = frameMap.localIndex(symbol)
            if (index == -1) {
                "Could not find ${setValue.dump()} in the frame map"
            }

            data.visitVarInsn(ASTORE, index)
        }

        override fun visitMirCall(call: MirCall, data: MethodVisitor) {
            super.visitMirCall(call, data)

            val owner = call.symbol.owner
            if (owner.parent == BuiltInsFile) {
                when (owner.name) {
                    "print" -> return visitPrintCall(call, data)
                    else -> throw IllegalStateException("Unknown builtin: ${owner.name}")
                }
            }

            data.visitMethodInsn(
                INVOKESTATIC,
                (owner.parent as MirFile).className(),
                owner.name,
                owner.descriptor(),
                false
            )
        }

        private fun visitPrintCall(call: MirCall, data: MethodVisitor) {
            // INVOKESTATIC js/ConsoleKt.print (Ljava/lang/Object;)V

            data.visitMethodInsn(
                INVOKESTATIC,
                "js/ConsoleKt",
                "print",
                "(Ljava/lang/Object;)V",
                false
            )
        }

        override fun visitMirObjectExpression(objectExpression: MirObjectExpression, data: MethodVisitor) {
            //    NEW js/JsObject
            //    DUP
            //    INVOKESPECIAL js/JsObject.<init> ()V
            data.visitTypeInsn(NEW, JSOBJECT_NAME)
            data.visitInsn(DUP)
            data.visitMethodInsn(
                INVOKESPECIAL,
                JSOBJECT_NAME,
                "<init>",
                "()V",
                false
            )

            if (objectExpression.entries.isNotEmpty()) {
                for (entry in objectExpression.entries) {
                    // ensure object is kept on stack after we add entry
                    data.visitInsn(DUP)
                    data.visitLdcInsn(entry.key)
                    entry.value.accept(this, data)
                    data.visitMethodInsn(
                        INVOKEVIRTUAL,
                        JSOBJECT_NAME,
                        "add",
                        "(Ljava/lang/String;Ljava/lang/Object;)V",
                        false
                    )
                }
            }
        }

        override fun visitMirConst(const: MirConst, data: MethodVisitor) {
            data.visitLdcInsn(const.value.value)
            when (const.value) {
                is ConstValue.Bool -> {
                    data.visitMethodInsn(
                        INVOKESTATIC,
                        "java/lang/Boolean",
                        "valueOf",
                        "(Z)Ljava/lang/Boolean;",
                        false
                    )
                }
                is ConstValue.Number -> {
                    data.visitMethodInsn(
                        INVOKESTATIC,
                        "java/lang/Double",
                        "valueOf",
                        "(D)Ljava/lang/Double;",
                        false
                    )
                }
                is ConstValue.Str -> { /* do nothing, string is already object */ }
            }
        }
    }
}

private fun MirFunction.descriptor(): String =
    parameters.joinToString(separator = "", prefix = "(", postfix = ")V") { parameter ->
        JOBJECT_SIGNATURE
    }

private fun MirFile.className(): String =
    fileName.capitalize().replace('.', '_')
