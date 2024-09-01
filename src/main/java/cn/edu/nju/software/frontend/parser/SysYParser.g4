parser grammar SysYParser;

options {
    tokenVocab = SysYLexer;
}

program
   : compUnit
   ;

funcUse:
    funcName L_PAREN funcRParams? R_PAREN
    ;

funcName:
    IDENT
    ;

compUnit
   : (funcDef | decl)+ EOF
   ;

decl:
    constDecl | varDecl
    ;

constDecl:
    CONST bType constDef (COMMA constDef)* SEMICOLON
    ;

bType:
    INT | FLOAT
    ;

constDef:
    IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN constInitVal
    ;

constInitVal:
    constExp
    | L_BRACE (constInitVal (COMMA constInitVal)*)? R_BRACE
    ;

varDecl:
    bType varDef (COMMA varDef)* SEMICOLON
    ;

varDef:
    IDENT (L_BRACKT constExp R_BRACKT)*
    | IDENT (L_BRACKT constExp R_BRACKT)* ASSIGN initVal
    ;

initVal:
    exp | L_BRACE (initVal (COMMA initVal)*)? R_BRACE
    ;

funcDef:
    funcType funcName L_PAREN funcFParams? R_PAREN block
    ;

funcType:
    VOID | INT | FLOAT
    ;

funcFParams:
    funcFParam (COMMA funcFParam)*
    ;

funcFParam:
    bType IDENT (L_BRACKT R_BRACKT (L_BRACKT exp R_BRACKT)*)?
    ;

block:
    L_BRACE blockItem* R_BRACE
    ;

blockItem:
    decl | stmt
    ;

stmt:
    lVal ASSIGN exp SEMICOLON | exp? SEMICOLON | block
    | IF cond ifStmt (ELSE elseStmt)?
    | WHILE cond whileStmt
    | BREAK SEMICOLON | CONTINUE SEMICOLON
    | RETURN exp? SEMICOLON
    ;

elseStmt:
    stmt
    ;

ifStmt:
    stmt
    ;

whileStmt:
    stmt
    ;

exp
   : L_PAREN exp R_PAREN
   | lVal
   | number
   | funcUse
   | unaryOp exp
   | exp (MUL | DIV | MOD) exp
   | exp (PLUS | MINUS) exp
   ;

cond
   : L_PAREN cond R_PAREN
   | exp
   | cond (LT | GT | LE | GE) cond
   | cond (EQ | NEQ) cond
   | cond AND cond
   | cond OR cond
   ;

lVal
   : IDENT (L_BRACKT exp R_BRACKT)*
   ;

number
   : INTEGER_CONST | FLOAT_CONST
   ;

unaryOp
   : PLUS
   | MINUS
   | NOT
   ;

funcRParams
   : param (COMMA param)*
   ;

param
   : exp
   ;

constExp
   : exp
   ;