package me.shika.js.elements

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElementVisitor
import javax.swing.Icon

class JsFile(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, JsLanguage) {
    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitFile(this)
    }

    override fun getFileType(): FileType = Type

    override fun toString(): String =
        "JsFile"

    object Type : LanguageFileType(JsLanguage) {
        override fun getName(): String = "JS"
        override fun getDescription(): String = "JS source file"
        override fun getDefaultExtension(): String = ".js"
        override fun getIcon(): Icon? = null
    }
}
