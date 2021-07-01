import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import junit.framework.Assert.assertEquals
import me.shika.js.TestEnv
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.hir.AST2HirConverter
import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.debug.dump
import me.shika.js.parser.JsParser
import me.shika.js.parser.JsParserDefinition
import org.junit.Test

class HirConverterTest {
    @Test
    fun testHirConversion() {
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
        val converter = AST2HirConverter(psiBuilder.lightTree, errorReporter = HirErrorReporter())
        val hirFile = converter.convertFile(psiBuilder.latestDoneMarker!!, "Test.js")

        assertEquals("""
            FILE name: Test.js
              FUNCTION name: name
                PARAMETER name: param1
                PARAMETER name: param2
                BODY
                  VAR name: hello
                    CONST: Str(value="value")
                  CALL name: print
                    REF: hello
              CALL name: name
                CONST: Number(value=0.0)
                CONST: Str(value="")
                CONST: Str(value="")
                CONST: Str(value="")
        """.trimIndent(),
            hirFile.dump()
        )
    }
}
