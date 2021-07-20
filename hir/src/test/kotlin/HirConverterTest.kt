import com.intellij.lang.LighterASTNode
import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.util.diff.FlyweightCapableTreeStructure
import me.shika.js.TestEnv
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.hir.AST2HirConverter
import me.shika.js.hir.HirError
import me.shika.js.hir.HirErrorReporter
import me.shika.js.hir.debug.dump
import me.shika.js.hir.resolve.HirReferenceResolver
import me.shika.js.hir.resolve.RootScope
import me.shika.js.parser.JsParser
import me.shika.js.parser.JsParserDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class HirConverterTest {
    private fun getTestAST(): Pair<FlyweightCapableTreeStructure<LighterASTNode>, LighterASTNode?> {
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
        return Pair(psiBuilder.lightTree, psiBuilder.latestDoneMarker)
    }

    @Test
    fun testHirConversion() {
        val (tree, rootNode) = getTestAST()
        val errorReporter = HirErrorReporter()
        val converter = AST2HirConverter(tree, errorReporter)
        val hirFile = converter.convertFile(rootNode!!, "Test.js")

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
                        REF: <unresolved hello>
                      KEY: secondKey
                      VALUE:
                        OBJECT:
                          KEY: nestedKey
                          VALUE:
                            CONST: Str(value=nestedValue)
                  CALL: <unresolved print>
                    REF: <unresolved hello>
              CALL: <unresolved name>
                CONST: Number(value=0.6)
                CONST: Str(value=)
                CONST: Str(value=)
                CONST: Str(value=)
        """.trimIndent(),
            hirFile.dump().trimEnd()
        )

        assertEquals(errorReporter.getErrors(), emptyList<HirError>())
    }

    @Test
    fun testHirResolve() {
        val (tree, rootNode) = getTestAST()
        val errorReporter = HirErrorReporter()
        val converter = AST2HirConverter(tree, errorReporter)
        val hirFile = converter.convertFile(rootNode!!, "Test.js")
        HirReferenceResolver(errorReporter).visitHirFile(hirFile, RootScope)

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
                        REF: variable hello
                      KEY: secondKey
                      VALUE:
                        OBJECT:
                          KEY: nestedKey
                          VALUE:
                            CONST: Str(value=nestedValue)
                  CALL: function print
                    REF: variable hello
              CALL: function name
                CONST: Number(value=0.6)
                CONST: Str(value=)
                CONST: Str(value=)
                CONST: Str(value=)
        """.trimIndent(),
            hirFile.dump().trimEnd()
        )

        assertEquals(errorReporter.getErrors(), emptyList<HirError>())
    }
}
