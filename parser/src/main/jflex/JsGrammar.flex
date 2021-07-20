package me.shika.js.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import java.lang.StringBuilder;
import java.io.Reader;

%%

%class GeneratedJsFlexLexer
%unicode
//%line
//%column

%function next
%type IElementType

%scanerror JsLexerException

%{
    StringBuilder string = new StringBuilder();

    public void reset(Reader buffer, int start, int end, int initialState) {
        yyreset(buffer);
        zzCurrentPos = zzMarkedPos = zzStartRead = start;
        zzAtEOF  = false;
        zzAtBOL = true;
        zzEndRead = end;
        yybegin(initialState);
    }

    public int getTokenStart() {
      return zzStartRead;
    }
%}

%eof{
    return;
%eof}

%state STRING

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace  = [ \t\f]

Identifier = [:jletter:][:jletterdigit:]*
Digit = [0-9]
NumberLiteral = {Digit}*[\.]{Digit}+
DoubleQuote = [\"]

%state STRING

%%

/* keywords */
<YYINITIAL> "function" { return JsToken.FUNCTION_KEYWORD; }
<YYINITIAL> "var"      { return JsToken.VAR_KEYWORD; }
<YYINITIAL> "if"       { return JsToken.IF_KEYWORD; }
<YYINITIAL> "else"     { return JsToken.ELSE_KEYWORD; }
<YYINITIAL> "return"   { return JsToken.RETURN_KEYWORD; }

/* operators */
<YYINITIAL> "=="       { return JsToken.EQEQ; }

/* punctuation */
<YYINITIAL> "("        { return JsToken.LPAR; }
<YYINITIAL> ")"        { return JsToken.RPAR; }
<YYINITIAL> "{"        { return JsToken.LBRACE; }
<YYINITIAL> "}"        { return JsToken.RBRACE; }
<YYINITIAL> "="        { return JsToken.EQ; }
<YYINITIAL> ";"        { return JsToken.SEMICOLON; }
<YYINITIAL> ":"        { return JsToken.COLON; }
<YYINITIAL> ","        { return JsToken.COMMA; }
<YYINITIAL> "."        { return JsToken.DOT; }

/* literals */
<YYINITIAL> {NumberLiteral}     { return JsToken.NUMBER_LITERAL; }
<YYINITIAL> "true"|"false"      { return JsToken.BOOLEAN_LITERAL; }

/* strings */
<YYINITIAL> {DoubleQuote}       { yybegin(STRING); }
<STRING> [^{DoubleQuote}]       { /* ignore */ }
<STRING> {DoubleQuote}          { yybegin(YYINITIAL); return JsToken.STRING_LITERAL; }

/* identifier */
<YYINITIAL> {Identifier}        { return JsToken.IDENTIFIER; }

/* whitespace */
<YYINITIAL> {WhiteSpace}        { return JsToken.WHITESPACE; }
<YYINITIAL> {LineTerminator}    { return JsToken.WHITESPACE; }

/* catch all */
[\s\S]              { return TokenType.BAD_CHARACTER; }

