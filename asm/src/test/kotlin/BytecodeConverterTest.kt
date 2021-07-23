import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import me.shika.js.TestEnv
import me.shika.js.asm.Mir2AsmConverter
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.hir.AST2HirConverter
import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.resolve.HirReferenceResolver
import me.shika.js.hir.resolve.RootScope
import me.shika.js.mir.Hir2MirConverter
import me.shika.js.mir.elements.MirFile
import me.shika.js.parser.JsParser
import me.shika.js.parser.JsParserDefinition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
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

        return mirFile
    }

    @Test
    fun testMirConversion() {
        val mirFile = getTestMir()
        val stringWriter = StringWriter()
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES) //
        Mir2AsmConverter().convertFile(mirFile, writer)
        val clsBytes = writer.toByteArray()

        runClass(clsBytes, "Test_js")

        val reader = ClassReader(clsBytes)
        reader.accept(TraceClassVisitor(PrintWriter(stringWriter)), 0)

        assertEquals(
            """
            // class version 52.0 (52)
            // access flags 0x1
            public class Test_js {
            
            
              // access flags 0x19
              public final static name(Ljava/lang/Object;Ljava/lang/Object;)V
               L0
                LDC "value"
                ASTORE 2
                NEW js/JsObject
                DUP
                INVOKESPECIAL js/JsObject.<init> ()V
                DUP
                LDC "key"
                ALOAD 2
                INVOKEVIRTUAL js/JsObject.add (Ljava/lang/String;Ljava/lang/Object;)V
                DUP
                LDC "secondKey"
                NEW js/JsObject
                DUP
                INVOKESPECIAL js/JsObject.<init> ()V
                DUP
                LDC "nestedKey"
                LDC "nestedValue"
                INVOKEVIRTUAL js/JsObject.add (Ljava/lang/String;Ljava/lang/Object;)V
                INVOKEVIRTUAL js/JsObject.add (Ljava/lang/String;Ljava/lang/Object;)V
                ASTORE 3
                ALOAD 3
                DUP
                ASTORE 2
                DUP
                ASTORE 3
                ALOAD 2
                INVOKESTATIC js/ConsoleKt.print (Ljava/lang/Object;)V
                RETURN
                LOCALVARIABLE param1 Ljava/lang/Object; L0 L0 0
                LOCALVARIABLE param2 Ljava/lang/Object; L0 L0 1
                LOCALVARIABLE hello Ljava/lang/Object; L0 L0 2
                LOCALVARIABLE test Ljava/lang/Object; L0 L0 3
                MAXSTACK = 7
                MAXLOCALS = 4
            
              // access flags 0x8
              static <clinit>()V
                LDC 0.6
                INVOKESTATIC java/lang/Double.valueOf (D)Ljava/lang/Double;
                LDC ""
                INVOKESTATIC Test_js.name (Ljava/lang/Object;Ljava/lang/Object;)V
                RETURN
                MAXSTACK = 2
                MAXLOCALS = 0
            }
            """.trimIndent(),
            stringWriter.toString().trimEnd()
        )

    }

    fun runClass(bytes: ByteArray, clsName: String) {
        val classLoader = object : ClassLoader(javaClass.classLoader) {
            override fun findClass(name: String?): Class<*> =
                if (name == clsName) {
                    defineClass(clsName, bytes, 0, bytes.size)
                } else {
                    super.findClass(name)
                }
        }
        // force init
        Class.forName(clsName, true, classLoader)
    }
}
