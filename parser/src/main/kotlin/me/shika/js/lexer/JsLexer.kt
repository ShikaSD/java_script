package me.shika.js.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.FlexLexer
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharSequenceReader

class JsLexer : FlexAdapter(JbFlexLexerAdapter())

class JbFlexLexerAdapter : FlexLexer {
    private val lexer = GeneratedJsFlexLexer(null)

    override fun yybegin(newState: Int) {
        lexer.yybegin(newState)
    }

    override fun yystate(): Int =
        lexer.yystate()

    override fun getTokenStart(): Int =
        lexer.tokenStart

    override fun getTokenEnd(): Int =
        lexer.tokenStart + lexer.yylength()

    override fun advance(): IElementType? =
        lexer.next()

    override fun reset(text: CharSequence, start: Int, end: Int, initialState: Int) {
//        lexer.reset(CharSequenceReader(text), start, end, initialState)
        lexer.yyreset(CharSequenceReader(text))
    }

}
