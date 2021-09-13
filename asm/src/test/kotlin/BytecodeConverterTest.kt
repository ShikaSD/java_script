import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import me.shika.js.TestEnv
import me.shika.js.asm.CompiledClass
import me.shika.js.asm.Mir2AsmConverter
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.hir.AST2HirConverter
import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.resolve.HirReferenceResolver
import me.shika.js.hir.resolve.RootScope
import me.shika.js.mir.Hir2MirConverter
import me.shika.js.mir.elements.MirFile
import me.shika.js.mir.lowerings.Lowerings
import me.shika.js.parser.JsParser
import me.shika.js.parser.JsParserDefinition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

class BytecodeConverterTest {
    private fun getTestMir(): MirFile {
        val fileStream = javaClass.getResourceAsStream("Test.js")!!
        val jsParserDefinition = JsParserDefinition()
        val parentDisposable = Disposable {}
        val environment = TestEnv()
        val testProject = MockProject(null, parentDisposable)

        environment.registerFileType(JsFile.Type, ".js")
        environment.registerParserDefinition(jsParserDefinition)

        val psiBuilder = PsiBuilderFactoryImpl().createBuilder(
            jsParserDefinition,
            jsParserDefinition.createLexer(testProject),
            fileStream.bufferedReader().readText()
        )

        JsParser().parse(JsElementType.FILE, psiBuilder)
        val errorReporter = HirErrorReporter()
        val converter = AST2HirConverter(psiBuilder.lightTree, errorReporter)
        val hirFile = converter.convertFile(psiBuilder.latestDoneMarker!!, "Test.js")
        HirReferenceResolver(errorReporter).visitHirFile(hirFile, RootScope)
        val mirFile = Hir2MirConverter().convertFile(hirFile)
        Lowerings.forEach {
            mirFile.accept(it, null)
        }

        return mirFile
    }

    @Test
    fun testBytecodeConversion() {
        val mirFile = getTestMir()
        val stringWriter = StringWriter()
        val compiledClasses = Mir2AsmConverter().convertFile(mirFile)

        compiledClasses.forEach {
            val reader = ClassReader(it.bytes)
            reader.accept(TraceClassVisitor(PrintWriter(stringWriter)), 0)
            stringWriter.append('\n')
        }

        assertEquals(
            """
            // class version 52.0 (52)
            // access flags 0x11
            public final class Test_js extends js/JsObject {
            
            
              // access flags 0x1
              public <init>()V
                ALOAD 0
                INVOKESPECIAL js/JsObject.<init> ()V
                RETURN
                MAXSTACK = 1
                MAXLOCALS = 1
            
              // access flags 0x9
              public static <clinit>()V
               L0
                NEW name
                DUP
                INVOKESPECIAL name.<init> ()V
                ASTORE 1
                ALOAD 1
                CHECKCAST js/JsFunction
                LDC 2
                ANEWARRAY java/lang/Object
                DUP
                LDC 0
                LDC 0.6
                INVOKESTATIC java/lang/Double.valueOf (D)Ljava/lang/Double;
                AASTORE
                DUP
                LDC 1
                LDC ""
                AASTORE
                INVOKEINTERFACE js/JsFunction.invoke ([Ljava/lang/Object;)V (itf)
                RETURN
                LOCALVARIABLE name Ljava/lang/Object; L0 L0 1
                MAXSTACK = 6
                MAXLOCALS = 2
            }
            
            // class version 52.0 (52)
            // access flags 0x11
            public final class name extends js/JsObject implements js/JsFunction {
            
            
              // access flags 0x1
              public <init>()V
                ALOAD 0
                INVOKESPECIAL js/JsObject.<init> ()V
                RETURN
                MAXSTACK = 1
                MAXLOCALS = 1
            
              // access flags 0x1
              public invoke([Ljava/lang/Object;)V
               L0
                ALOAD 1
                LDC 0
                AALOAD
                ASTORE 2
                ALOAD 1
                LDC 1
                AALOAD
                ASTORE 3
                LDC "value"
                ASTORE 4
                NEW js/JsObject
                DUP
                INVOKESPECIAL js/JsObject.<init> ()V
                DUP
                LDC "key"
                ALOAD 4
                INVOKEVIRTUAL js/JsObject.put (Ljava/lang/String;Ljava/lang/Object;)V
                DUP
                LDC "secondKey"
                NEW js/JsObject
                DUP
                INVOKESPECIAL js/JsObject.<init> ()V
                DUP
                LDC "nestedKey"
                LDC "nestedValue"
                INVOKEVIRTUAL js/JsObject.put (Ljava/lang/String;Ljava/lang/Object;)V
                INVOKEVIRTUAL js/JsObject.put (Ljava/lang/String;Ljava/lang/Object;)V
                ASTORE 5
                ALOAD 5
                DUP
                ASTORE 4
                DUP
                ASTORE 5
                INVOKESTATIC js/ConsoleKt.print (Ljava/lang/Object;)V
                RETURN
                LOCALVARIABLE param1 Ljava/lang/Object; L0 L0 2
                LOCALVARIABLE param2 Ljava/lang/Object; L0 L0 3
                LOCALVARIABLE hello Ljava/lang/Object; L0 L0 4
                LOCALVARIABLE test Ljava/lang/Object; L0 L0 5
                MAXSTACK = 7
                MAXLOCALS = 6
            }""".trimIndent(),
            stringWriter.toString().trimEnd()
        )

        runClasses(compiledClasses)
    }

    fun runClasses(compiledClasses: List<CompiledClass>) {
        val classLoader = object : ClassLoader(javaClass.classLoader) {
            override fun findClass(name: String?): Class<*> {
                val cls = compiledClasses.find { it.name == name }
                return if (cls != null) {
                    super.defineClass(cls.name, cls.bytes, 0, cls.bytes.size)
                } else {
                    super.findClass(name)
                }
            }
        }


        // force init
        Class.forName(compiledClasses[0].name, true, classLoader)
    }
}
