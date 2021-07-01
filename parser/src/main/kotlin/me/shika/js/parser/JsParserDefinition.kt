package me.shika.js.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import me.shika.js.elements.JsElementType
import me.shika.js.elements.JsFile
import me.shika.js.lexer.JsLexer

class JsParserDefinition : ParserDefinition {
    override fun createLexer(project: Project): Lexer = JsLexer()

    override fun createParser(project: Project): PsiParser = JsParser()

    override fun getFileNodeType(): IFileElementType = JsElementType.FILE_TYPE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(astNode: ASTNode): PsiElement =
        ASTWrapperPsiElement(astNode)

    override fun createFile(fileViewProvider: FileViewProvider): PsiFile = JsFile(fileViewProvider)
}
