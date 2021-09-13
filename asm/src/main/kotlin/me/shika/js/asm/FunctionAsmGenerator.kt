@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package me.shika.js.asm

import me.shika.js.ConstValue
import me.shika.js.mir.BuiltInsFile
import me.shika.js.mir.debug.dump
import me.shika.js.mir.elements.MirBody
import me.shika.js.mir.elements.MirCall
import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirConst
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirGetProperty
import me.shika.js.mir.elements.MirGetValue
import me.shika.js.mir.elements.MirNewInstance
import me.shika.js.mir.elements.MirObjectExpression
import me.shika.js.mir.elements.MirParameter
import me.shika.js.mir.elements.MirParameterSymbol
import me.shika.js.mir.elements.MirSetProperty
import me.shika.js.mir.elements.MirSetValue
import me.shika.js.mir.elements.MirSymbol
import me.shika.js.mir.elements.MirVariable
import me.shika.js.mir.elements.MirVariableSymbol
import me.shika.js.mir.elements.MirVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.AALOAD
import org.objectweb.asm.Opcodes.AASTORE
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ANEWARRAY
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.INVOKEINTERFACE
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.NEW

class MirFunction2Asm : MirVisitor<FunctionScope, Unit> {
    override fun visitMirFunction(function: MirFunction, scope: FunctionScope) {
        super.visitMirFunction(function, scope)
    }

    override fun visitMirBody(body: MirBody, scope: FunctionScope) {
        val mv = scope.methodVisitor

        mv.visitCode()
        mv.visitLabel(scope.startLabel)

        // push params on stack
        scope.function.parameters.forEachIndexed { index, arg ->
            val varIndex = scope.localMap.localIndex(arg.symbol)

            mv.visitVarInsn(ALOAD, scope.localMap.localIndex(scope.argumentsSymbol))
            mv.visitLdcInsn(index)
            mv.visitInsn(AALOAD)

            mv.visitVarInsn(ASTORE, varIndex)
        }

        super.visitMirBody(body, scope)

        mv.visitLabel(scope.endLabel)
        mv.visitInsn(Opcodes.RETURN)

        mv.visitMaxs(-1, -1)
        mv.visitEnd()
    }

    override fun visitMirParameter(parameter: MirParameter, scope: FunctionScope) {
        val index = scope.localMap.define(parameter.symbol)
        scope.methodVisitor.visitLocalVariable(
            parameter.name,
            JOBJECT_SIGNATURE,
            null,
            scope.startLabel,
            scope.endLabel,
            index,
        )
        super.visitMirParameter(parameter, scope)
    }

    override fun visitMirVariable(variable: MirVariable, scope: FunctionScope) {
        val variableIndex = scope.localMap.define(variable.symbol)
        scope.methodVisitor.visitLocalVariable(
            variable.name,
            JOBJECT_SIGNATURE,
            null,
            scope.startLabel,
            scope.endLabel,
            variableIndex
        )
        super.visitMirVariable(variable, scope)
        if (variable.value != null) {
            scope.methodVisitor.visitVarInsn(ASTORE, variableIndex)
        }
    }

    override fun visitMirNewInstance(newInstance: MirNewInstance, scope: FunctionScope) {
        val mv = scope.methodVisitor

        val cls = newInstance.classSymbol.owner
        var fqName = cls.name
        var nextOwner: MirClass? = cls.parent as? MirClass
        while (nextOwner != null) {
            fqName = "${nextOwner.name}${fqName}"
            nextOwner = cls.parent as? MirClass
        }

        mv.visitTypeInsn(NEW, fqName)
        mv.visitInsn(DUP)
        mv.visitMethodInsn(
            INVOKESPECIAL,
            fqName,
            "<init>",
            "()V",
            false
        )
    }

    override fun visitMirGetValue(getValue: MirGetValue, scope: FunctionScope) {
        val symbol = getValue.symbol

        require(symbol is MirVariableSymbol || symbol is MirParameterSymbol) {
            "Can only reference variables and parameters, found ${getValue.dump()}"
        }

        val index = scope.localMap.localIndex(symbol)
        if (index == -1) {
            "Could not find ${getValue.dump()} in the frame map"
        }

        scope.methodVisitor.visitVarInsn(Opcodes.ALOAD, index)
    }

    override fun visitMirSetValue(setValue: MirSetValue, scope: FunctionScope) {
        val symbol = setValue.symbol

        // put value on stack
        setValue.value.accept(this, scope)
        // leave it on the stack after assignment
        scope.methodVisitor.visitInsn(Opcodes.DUP)

        val index = scope.localMap.localIndex(symbol)
        if (index == -1) {
            "Could not find ${setValue.dump()} in the frame map"
        }

        scope.methodVisitor.visitVarInsn(Opcodes.ASTORE, index)
    }

    override fun visitMirGetProperty(getProperty: MirGetProperty, scope: FunctionScope) {
        val mv = scope.methodVisitor

        getProperty.receiver.accept(this, scope)
        mv.visitTypeInsn(Opcodes.CHECKCAST, JSOBJECT_NAME)

        mv.visitLdcInsn(getProperty.name)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            JSOBJECT_NAME,
            "get",
            "(Ljava/lang/String;)Ljava/lang/Object;",
            false
        )
    }

    override fun visitMirSetProperty(setProperty: MirSetProperty, scope: FunctionScope) {
        val mv = scope.methodVisitor

        setProperty.receiver.accept(this, scope)
        mv.visitTypeInsn(Opcodes.CHECKCAST, JSOBJECT_NAME)

        // ensure object is kept on stack after we add entry
        mv.visitInsn(Opcodes.DUP)

        mv.visitLdcInsn(setProperty.value)
        setProperty.value.accept(this, scope)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            JSOBJECT_NAME,
            "put",
            "(Ljava/lang/String;Ljava/lang/Object;)V",
            false
        )
    }

    override fun visitMirCall(call: MirCall, scope: FunctionScope) {
        val mv = scope.methodVisitor

        // fixme builtins
        val owner = (call.receiver as? MirGetValue)?.symbol?.owner
        if (owner is MirFunction && owner.parent == BuiltInsFile) {
            when (owner.name) {
                "print" -> return visitPrintCall(call, scope)
                else -> throw IllegalStateException("Unknown builtin: ${owner.name}")
            }
        }

        // receiver
        call.receiver.accept(this, scope)
        mv.visitTypeInsn(CHECKCAST, JSFUNCTION_NAME)

        // args
        mv.visitLdcInsn(call.arguments.size)
        mv.visitTypeInsn(ANEWARRAY, JOBJECT_NAME)

        call.arguments.forEachIndexed { index, arg ->
            mv.visitInsn(DUP)
            mv.visitLdcInsn(index)

            if (arg == null) {
                mv.visitInsn(ACONST_NULL)
            } else {
                arg.accept(this, scope)
            }

            mv.visitInsn(AASTORE)
        }

        // call
        scope.methodVisitor.visitMethodInsn(
            INVOKEINTERFACE,
            JSFUNCTION_NAME,
            "invoke",
            "([$JOBJECT_SIGNATURE)V",
            true
        )
    }

    private fun visitPrintCall(call: MirCall, scope: FunctionScope) {
        // INVOKESTATIC js/ConsoleKt.print (Ljava/lang/Object;)V

        scope.methodVisitor.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "js/ConsoleKt",
            "print",
            "(Ljava/lang/Object;)V",
            false
        )
    }

    override fun visitMirObjectExpression(objectExpression: MirObjectExpression, scope: FunctionScope) {
        val mv = scope.methodVisitor

        //    NEW js/JsObject
        //    DUP
        //    INVOKESPECIAL js/JsObject.<init> ()V
        mv.visitTypeInsn(Opcodes.NEW, JSOBJECT_NAME)
        mv.visitInsn(Opcodes.DUP)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            JSOBJECT_NAME,
            "<init>",
            "()V",
            false
        )

        if (objectExpression.entries.isNotEmpty()) {
            for (entry in objectExpression.entries) {
                // ensure object is kept on stack after we add entry
                mv.visitInsn(Opcodes.DUP)
                mv.visitLdcInsn(entry.key)
                entry.value.accept(this, scope)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    JSOBJECT_NAME,
                    "put",
                    "(Ljava/lang/String;Ljava/lang/Object;)V",
                    false
                )
            }
        }
    }

    override fun visitMirConst(const: MirConst, scope: FunctionScope) {
        val mv = scope.methodVisitor
        mv.visitLdcInsn(const.value.value)
        when (const.value) {
            is ConstValue.Bool -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
            }
            is ConstValue.Number -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
            }
            is ConstValue.Str -> { /* do nothing, string is already object */ }
        }
    }

    override fun visitMirElement(element: MirElement, data: FunctionScope) {
        element.acceptChildren(this, data)
    }
}



class FunctionScope(
    val function: MirFunction,
    val classScope: ClassScope,
    val methodVisitor: MethodVisitor,
    val startLabel: Label = Label(),
    val endLabel: Label = Label(),
    val localMap: LocalMap = LocalMap()
) {
    val argumentsSymbol = MirParameterSymbol()

    init {
        localMap.define(argumentsSymbol)
    }

    class LocalMap(private val shift: Int = 0) {
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
}
