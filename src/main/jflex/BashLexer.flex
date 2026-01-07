import java_cup.runtime.Symbol;

%%

%class BashLexer
%unicode
%cup
%line
%column

%state STRING

%{
  private StringBuilder sb;

  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1);
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }
%}

WHITESPACE = [ \t\f]+
NEWLINE = \r\n|\r|\n
IDENT = [a-zA-Z0-9._/-]+

%%


{WHITESPACE}   { /* ignore */ }

{NEWLINE}      { return symbol(sym.NEWLINE); }
"|"            { return symbol(sym.PIPE); }
";"            { return symbol(sym.SEMI); }

">>"           { return symbol(sym.APPEND); }
">"            { return symbol(sym.REDIR); }

"$?"           { return symbol(sym.IDENT, "$?"); }

"\""           { sb = new StringBuilder(); yybegin(STRING); }

{IDENT}        { return symbol(sym.IDENT, yytext()); }

<<EOF>>        { return symbol(sym.EOF); }


<STRING> {
  "\"" {
    yybegin(YYINITIAL);
    return symbol(sym.IDENT, sb.toString());
  }

  "\\\""  { sb.append('"'); }
  "\\\\"  { sb.append('\\'); }

  "\\".   { sb.append(yytext()); }

  [^\"\\]+ { sb.append(yytext()); }

  .       { sb.append(yytext()); }

  <<EOF>> {
    System.err.println("Unterminated string literal");
    yybegin(YYINITIAL);
    return symbol(sym.IDENT, sb.toString());
  }
}
