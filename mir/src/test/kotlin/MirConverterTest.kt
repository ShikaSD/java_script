import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import me.shika.js.TestEnv
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.hir.AST2HirConverter
import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.elements.HirFile
import me.shika.js.hir.resolve.HirReferenceResolver
import me.shika.js.hir.resolve.RootScope
import me.shika.js.mir.Hir2MirConverter
import me.shika.js.mir.debug.dump
import me.shika.js.parser.JsParser
import me.shika.js.parser.JsParserDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class MirConverterTest {
    private fun getTestHir(): HirFile {
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

        return hirFile
    }

    @Test
    fun testMirConversion() {
        val hirFile = getTestHir()
        val mirFile = Hir2MirConverter().convertFile(hirFile)

        assertEquals("""
            FILE name: Test.js
              FUNCTION name: myFunction
                PARAMETER name: param1
                PARAMETER name: param2
                BODY
                  VAR name: hello
                    CONST: Str(value="value")
                  CALL: function print
                    REF: variable hello
                    REF: parameter param1
                    REF: parameter param2
              CALL: function myFunction
                CONST: Number(value=0.1)
                CONST: Str(value="")
                CONST: Str(value="")
                CONST: Str(value="")
        """.trimIndent(),
            mirFile.dump().trimEnd()
        )
    }
}
