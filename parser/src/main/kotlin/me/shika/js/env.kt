package me.shika.js

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.testFramework.LightVirtualFile
import me.shika.js.elements.JsFile
import me.shika.js.elements.JsLanguage
import me.shika.js.parser.JsParserDefinition
import java.io.InputStream

class TestEnv : CoreApplicationEnvironment({})

fun parseFile(fileName: String, inputStream: InputStream): JsFile {
    val parentDisposable = Disposable {}
    val environment = TestEnv()
    val project = MockProject(null, parentDisposable)

    environment.registerFileType(JsFile.Type, ".js")
    environment.registerParserDefinition(JsParserDefinition())

    val psiManager = PsiManagerImpl(project)
    val fileText = inputStream.bufferedReader().readText()

    val fileViewProvider = SingleRootFileViewProvider(psiManager, LightVirtualFile(fileName, JsLanguage, fileText))

    return JsFile(fileViewProvider)
}
