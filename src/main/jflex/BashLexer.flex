import java_cup.runtime.Symbol;

%%

%class BashLexer
%unicode
%cup
%line
%column

%{

  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1);
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }

%}

WHITESPACE = [ \t\f]+
NEWLINE    = \r\n|\r|\n
IDENT      = [a-zA-Z_][a-zA-Z0-9_-]*

%%

{WHITESPACE}   { /* ignore */ }

{NEWLINE}      { return symbol(sym.NEWLINE); }
"|"            { return symbol(sym.PIPE); }
";"            { return symbol(sym.SEMI); }

{IDENT}        { return symbol(sym.IDENT, yytext()); }

<<EOF>>        { return symbol(sym.EOF); }
