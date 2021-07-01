package me.shika.js.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
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

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace  = [ \t\f]

Identifier = [:jletter:][:jletterdigit:]*
Digit = [0-9]
NumberLiteral = {Digit}+
StringLiteral = \"{InputCharacter}+\"

%state STRING

%%

/* keywords */
"function" { return JsToken.FUNCTION_KEYWORD; }
"var"      { return JsToken.VAR_KEYWORD; }
"if"       { return JsToken.IF_KEYWORD; }
"else"     { return JsToken.ELSE_KEYWORD; }
"return"   { return JsToken.RETURN_KEYWORD; }

/* operators */
"=="       { return JsToken.EQEQ; }

/* punctuation */
"("        { return JsToken.LPAR; }
")"        { return JsToken.RPAR; }
"{"        { return JsToken.LBRACE; }
"}"        { return JsToken.RBRACE; }
"="        { return JsToken.EQ; }
";"        { return JsToken.SEMICOLON; }
","        { return JsToken.COMMA; }
"."        { return JsToken.DOT; }

/* literals */
{NumberLiteral}     { return JsToken.NUMBER_LITERAL; }
"true"|"false"      { return JsToken.BOOLEAN_LITERAL; }
{StringLiteral}     { return JsToken.STRING_LITERAL; }

/* identifier */
{Identifier}        { return JsToken.IDENTIFIER; }

/* whitespace */
{WhiteSpace}        { /* ignore */ }
{LineTerminator}    { return JsToken.EOL; }

/* catch all */
[\s\S]              { return TokenType.BAD_CHARACTER; }

