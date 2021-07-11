import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.shika.js.parseFile
import org.junit.Assert.assertEquals
import org.junit.Test

class JsParserTest {
    @Test
    fun testLexer() {
        val fileText = javaClass.getResourceAsStream("Test.js")!!
        val jsFile = parseFile("Test.js", fileText)

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
        assertEquals("""
            JsFile
              ASTWrapperPsiElement(FUNCTION)
                PsiElement(FUNCTION_KEYWORD)
                PsiElement(WHITESPACE)
                PsiElement(IDENTIFIER)
                ASTWrapperPsiElement(PARAMETER_LIST)
                  PsiElement(()
                  ASTWrapperPsiElement(PARAMETER)
                    PsiElement(IDENTIFIER)
                  PsiElement(,)
                  PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(PARAMETER)
                    PsiElement(IDENTIFIER)
                  PsiElement())
                  PsiElement(WHITESPACE)
                ASTWrapperPsiElement(BLOCK)
                  PsiElement({)
                  PsiElement(WHITESPACE)
                  PsiElement(WHITESPACE)
                  PsiElement(WHITESPACE)
                  PsiElement(WHITESPACE)
                  PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(VARIABLE)
                    PsiElement(VAR_KEYWORD)
                    PsiElement(WHITESPACE)
                    PsiElement(IDENTIFIER)
                    PsiElement(WHITESPACE)
                    PsiElement(=)
                    PsiElement(WHITESPACE)
                    ASTWrapperPsiElement(ARGUMENT)
                      ASTWrapperPsiElement(STRING_CONSTANT)
                        PsiElement(STRING_LITERAL)
                    PsiElement(;)
                    PsiElement(WHITESPACE)
                    PsiElement(WHITESPACE)
                    PsiElement(WHITESPACE)
                    PsiElement(WHITESPACE)
                    PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(CALL)
                    ASTWrapperPsiElement(REFERENCE)
                      PsiElement(IDENTIFIER)
                    ASTWrapperPsiElement(ARGUMENT_LIST)
                      PsiElement(()
                      ASTWrapperPsiElement(ARGUMENT)
                        ASTWrapperPsiElement(REFERENCE)
                          PsiElement(IDENTIFIER)
                      PsiElement())
                      PsiElement(WHITESPACE)
                  PsiElement(})
                  PsiElement(WHITESPACE)
                  PsiElement(WHITESPACE)
              ASTWrapperPsiElement(CALL)
                ASTWrapperPsiElement(REFERENCE)
                  PsiElement(IDENTIFIER)
                ASTWrapperPsiElement(ARGUMENT_LIST)
                  PsiElement(()
                  ASTWrapperPsiElement(ARGUMENT)
                    ASTWrapperPsiElement(NUMBER_CONSTANT)
                      PsiElement(NUMBER_LITERAL)
                  PsiElement(,)
                  PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(ARGUMENT)
                    ASTWrapperPsiElement(STRING_CONSTANT)
                      PsiElement(STRING_LITERAL)
                  PsiElement(,)
                  PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(ARGUMENT)
                    ASTWrapperPsiElement(STRING_CONSTANT)
                      PsiElement(STRING_LITERAL)
                  PsiElement(,)
                  PsiElement(WHITESPACE)
                  ASTWrapperPsiElement(ARGUMENT)
                    ASTWrapperPsiElement(STRING_CONSTANT)
                      PsiElement(STRING_LITERAL)
                  PsiElement())
                  PsiElement(WHITESPACE)
        """.trimIndent(), result.trimEnd().toString())
    }
}
