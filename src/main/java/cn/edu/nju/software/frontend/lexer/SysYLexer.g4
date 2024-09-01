lexer grammar SysYLexer;

CONST : 'const';

INT : 'int';

VOID : 'void';

FLOAT: 'float';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

IDENT : ('_'|LETTER)('_'|LETTER|DIGIT)* //以下划线或字母开头，仅包含下划线、英文字母大小写、阿拉伯数字
   ;

INTEGER_CONST : ([0]|([1-9][0-9]*))|([0][xX][0-9a-fA-F]*)|([0][0-7]*)  //数字常量，包含十进制数，0开头的八进制数，0x或0X开头的十六进制数
   ;

FLOAT_CONST : Decimal_floating_constant | Hexadecimal_floating_constant ;

Decimal_floating_constant : Fractional_constant Exponent_part? Floating_suffix?
                          | Digit_sequence Exponent_part Floating_suffix? ;

Hexadecimal_floating_constant : Hexadecimal_prefix Hexadecimal_fractional_constant
                               Binary_exponent_part? Floating_suffix?
                               | Hexadecimal_prefix Hexadecimal_digit_sequence
                               Binary_exponent_part? Floating_suffix? ;

Fractional_constant : Digit_sequence? '.' Digit_sequence
                    | Digit_sequence '.' ;

fragment Exponent_part : ('e' | 'E') Sign? Digit_sequence ;

fragment Sign : '+' | '-' ;

fragment Digit_sequence : DIGIT+
               ;

fragment Hexadecimal_fractional_constant : Hexadecimal_digit_sequence? '.' Hexadecimal_digit_sequence
                                | Hexadecimal_digit_sequence '.' ;

fragment Binary_exponent_part : ('p' | 'P') Sign? Digit_sequence ;

fragment Hexadecimal_digit_sequence : Hexadecimal_digit+ ;

fragment Floating_suffix : ('f' | 'l' | 'F' | 'L') ;

fragment Hexadecimal_prefix : '0' ('x' | 'X') ;

fragment Hexadecimal_digit : DIGIT | HEX ;

WS
   : [ \r\n\t]+ -> skip
   ;

LINE_COMMENT
   : '//' ~[\n]* -> skip
   ;

MULTILINE_COMMENT
   : '/*' .*? '*/' -> skip
   ;

fragment LETTER : [a-zA-Z];

fragment DIGIT : [0-9];

fragment HEX : [a-f]|[A-F];
