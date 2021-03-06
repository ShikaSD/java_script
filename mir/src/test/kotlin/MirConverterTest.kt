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
import me.shika.js.mir.lowerings.Lowerings
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
              FUNCTION name: name
                PARAMETER name: param1
                PARAMETER name: param2
                BODY
                  VAR name: hello
                    CONST: Str(value=value)
                  VAR name: test
                    OBJECT:
                      KEY: key
                      VALUE:
                        GET: variable hello
                      KEY: secondKey
                      VALUE:
                        OBJECT:
                          KEY: nestedKey
                          VALUE:
                            CONST: Str(value=nestedValue)
                  SET_PROP: key2 
                    GET: variable test
                    OBJECT:
                  CALL:
                    GET_PROP: func
                      GET: variable test
                    ARGS:
                  SET: variable hello
                    SET: variable test
                      CONST: Str(value=result)
                  CALL:
                    GET: function print
                    ARGS:
                      GET: variable hello
              CALL:
                GET: function name
                ARGS:
                  CONST: Number(value=0.6)
                  CONST: Str(value=)
                  CONST: Str(value=)
                  CONST: Str(value=)
        """.trimIndent(),
            mirFile.dump().trimEnd()
        )
    }

    @Test
    fun testMirLowerings() {
        val hirFile = getTestHir()
        val mirFile = Hir2MirConverter().convertFile(hirFile)
        Lowerings.forEach {
            mirFile.accept(it, null)
        }

        assertEquals("""
            FILE name: Test.js
              CLASS name: Test_js
                FUNCTION name: <clinit>
                  BODY
                    VAR name: name
                      NEW symbol: class name
                    CALL:
                      GET: variable name
                      ARGS:
                        CONST: Number(value=0.6)
                        CONST: Str(value=)
                        CONST: Str(value=)
                        CONST: Str(value=)
              CLASS name: name
                FUNCTION name: invoke
                  PARAMETER name: param1
                  PARAMETER name: param2
                  BODY
                    VAR name: hello
                      CONST: Str(value=value)
                    VAR name: test
                      OBJECT:
                        KEY: key
                        VALUE:
                          GET: variable hello
                        KEY: secondKey
                        VALUE:
                          OBJECT:
                            KEY: nestedKey
                            VALUE:
                              CONST: Str(value=nestedValue)
                    SET_PROP: key2 
                      GET: variable test
                      OBJECT:
                    CALL:
                      GET_PROP: func
                        GET: variable test
                      ARGS:
                    SET: variable hello
                      SET: variable test
                        CONST: Str(value=result)
                    CALL:
                      GET: function print
                      ARGS:
                        GET: variable hello
        """.trimIndent(),
            mirFile.dump().trimEnd()
        )
    }
}
