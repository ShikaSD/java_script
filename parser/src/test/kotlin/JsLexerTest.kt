import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.testFramework.LightVirtualFile
import me.shika.js.elements.JsFile
import me.shika.js.elements.JsLanguage
import me.shika.js.parser.JsParserDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class JsLexerTest {
    class TestEnv : CoreApplicationEnvironment({})

    @Test
    fun testLexer() {
        val parentDisposable = Disposable{}
        val environment = TestEnv()
        val project = MockProject(null, parentDisposable)

        environment.registerFileType(JsFile.Type, ".js")
        environment.registerParserDefinition(JsParserDefinition())

        val psiManager = PsiManagerImpl(project)

        val fileText = javaClass.getResourceAsStream("Test.js").reader().use { it.readText() }
        val fileViewProvider = SingleRootFileViewProvider(psiManager, LightVirtualFile("Test.js", JsLanguage, fileText))
        val jsFile = JsFile(fileViewProvider)
        val result = StringBuilder()
        jsFile.accept(object : PsiElementVisitor() {
            var depth = 0
            override fun visitElement(element: PsiElement) {
                for (i in 0 until depth) {
                    result.append("  ")
                }
                result.append(element.toString())
                result.append("\n")

                depth++
                element.acceptChildren(this)
                depth--
            }
        })
        result.deleteCharAt(result.lastIndex)

        assertEquals("""
            JsFile
              ASTWrapperPsiElement(FUNCTION)
                PsiElement(FUNCTION_KEYWORD)
                PsiElement(IDENTIFIER)
                ASTWrapperPsiElement(PARAMETER_LIST)
                  PsiElement(()
                  ASTWrapperPsiElement(PARAMETER)
                    PsiElement(IDENTIFIER)
                  PsiElement(,)
                  ASTWrapperPsiElement(PARAMETER)
                    PsiElement(IDENTIFIER)
                  PsiElement())
                ASTWrapperPsiElement(BLOCK)
                  PsiElement({)
                  PsiElement(EOL)
                  ASTWrapperPsiElement(VARIABLE)
                    PsiElement(VAR_KEYWORD)
                    PsiElement(IDENTIFIER)
                    PsiElement(=)
                    ASTWrapperPsiElement(STRING_CONSTANT)
                      PsiElement(STRING_LITERAL)
                    PsiElement(;)
                  PsiElement(EOL)
                  ASTWrapperPsiElement(CALL)
                    ASTWrapperPsiElement(REFERENCE)
                      PsiElement(IDENTIFIER)
                    ASTWrapperPsiElement(ARGUMENT_LIST)
                      PsiElement(()
                      ASTWrapperPsiElement(ARGUMENT)
                        ASTWrapperPsiElement(REFERENCE)
                          PsiElement(IDENTIFIER)
                      PsiElement())
                  PsiElement(EOL)
                  PsiElement(})
              PsiElement(EOL)
              PsiElement(EOL)
              ASTWrapperPsiElement(CALL)
                ASTWrapperPsiElement(REFERENCE)
                  PsiElement(IDENTIFIER)
                ASTWrapperPsiElement(ARGUMENT_LIST)
                  PsiElement(()
                  ASTWrapperPsiElement(ARGUMENT)
                    ASTWrapperPsiElement(STRING_CONSTANT)
                      PsiElement(STRING_LITERAL)
                  PsiElement())
              PsiElement(EOL)
        """.trimIndent(), result.toString())
    }
}
