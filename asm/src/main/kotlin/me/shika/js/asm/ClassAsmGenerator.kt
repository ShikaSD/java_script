package me.shika.js.asm

import me.shika.js.mir.elements.MirClass
import me.shika.js.mir.elements.MirElement
import me.shika.js.mir.elements.MirFunction
import me.shika.js.mir.elements.MirVisitor
import me.shika.js.mir.origin.JsFunctionOrigin
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Opcodes.V1_8

class MirClass2Asm : MirVisitor<ClassScope, Unit> {
    private val function2Asm = MirFunction2Asm()

    override fun visitMirClass(cls: MirClass, scope: ClassScope) {
        var innerScope = scope
        // fixme: hack to avoid clashes on top classes
        if (scope.cls != cls) {
           innerScope = scope.childScope(cls)
        }

        super.visitMirClass(cls, innerScope)
    }

    override fun visitMirFunction(function: MirFunction, scope: ClassScope) {
        val staticFlag = if (function.isStatic) ACC_STATIC else 0
        val isJsInvoke = function.origin == JsFunctionOrigin

        val functionScope = FunctionScope(
            function = function,
            classScope = scope,
            methodVisitor = scope.classVisitor.visitMethod(
                ACC_PUBLIC or staticFlag,
                function.name,
                if (isJsInvoke) JSFUNCTION_INVOKE_SIGNATURE else function.descriptor(),
                null,
                emptyArray()
            ),
            localMap = FunctionScope.LocalMap(
                shift = if (function.isStatic) 0 else 1 // this is first param
            )
        )
        function2Asm.visitMirFunction(function, functionScope)
    }

    override fun visitMirElement(element: MirElement, data: ClassScope) {
        element.acceptChildren(this, data)
    }

    private fun MirFunction.descriptor(): String =
        parameters.joinToString(separator = "", prefix = "(", postfix = ")V") { parameter ->
            JOBJECT_SIGNATURE
        }
}

class ClassScope(
    val cls: MirClass,
    val descriptor: String,
    val classVisitor: ClassWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES),
) {
    init {
        classVisitor.visit(
            V1_8,
            ACC_FINAL or ACC_PUBLIC,
            cls.name,
            null,
            JSOBJECT_NAME,
            listOfNotNull(
                JSFUNCTION_NAME.takeIf { cls.origin == JsFunctionOrigin },
            ).toTypedArray()
        )

        val init = classVisitor.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            emptyArray()
        )
        init.visitCode()
        init.visitIntInsn(ALOAD, 0)
        init.visitMethodInsn(
            INVOKESPECIAL,
            JSOBJECT_NAME,
            "<init>",
            "()V",
            false
        )
        init.visitInsn(RETURN)
        init.visitMaxs(-1, -1)
        init.visitEnd()
    }

    private val innerClasses = mutableListOf<ClassScope>()

    fun childScope(cls: MirClass): ClassScope {
        val childScope = ClassScope(
            cls = cls,
            descriptor = "$descriptor\$${cls.name}",
        )

        innerClasses += childScope

        return childScope
    }

    fun compiled(): List<CompiledClass> =
        listOf(
            CompiledClass(descriptor, classVisitor.toByteArray())
        ) + innerClasses.flatMap {
            it.compiled()
        }
}
