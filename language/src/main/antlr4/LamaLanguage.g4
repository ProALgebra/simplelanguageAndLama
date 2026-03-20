grammar LamaLanguage;

program
    : topLevelItem (SEMI* topLevelItem)* SEMI* EOF
    ;

topLevelItem
    : definition
    | expression
    ;

definition
    : varDefinition
    | functionDefinition
    | infixDefinition
    ;

varDefinition
    : VAR varBinding (COMMA varBinding)*
    ;

varBinding
    : IDENTIFIER (EQ assignmentExpression)?
    ;

functionDefinition
    : FUN IDENTIFIER parameterList blockExpression
    ;

infixDefinition
    : infixKind operatorSymbol infixPlacement operatorSymbol parameterList blockExpression
    ;

infixKind
    : INFIX
    | INFIXL
    | INFIXR
    ;

infixPlacement
    : AT
    | BEFORE
    | AFTER
    ;

parameterList
    : LPAREN (pattern (COMMA pattern)*)? RPAREN
    ;

blockExpression
    : LBRACE scopedBody? RBRACE
    ;

scopedBody
    : scopedItem (SEMI* scopedItem)* SEMI*
    ;

scopedItem
    : definition
    | expression
    ;

expression
    : sequenceExpression
    ;

sequenceExpression
    : assignmentExpression (SEMI assignmentExpression)*
    ;

assignmentExpression
    : consExpression (ASSIGN_COLON assignmentExpression)?
    ;

consExpression
    : orExpression (COLON consExpression)?
    ;

orExpression
    : andExpression ((EXCL2 | OROR) andExpression)*
    ;

andExpression
    : customInfixExpression (ANDAND customInfixExpression)*
    ;

customInfixExpression
    : compareExpression (OPERATOR compareExpression)*
    ;

compareExpression
    : additiveExpression ((EQ | EQEQ | NEQ | LT | LE | GT | GE) additiveExpression)?
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | SLASH | PERCENT) unaryExpression)*
    ;

unaryExpression
    : (PLUS | MINUS | ETA) unaryExpression
    | postfixExpression
    ;

postfixExpression
    : primaryExpression postfixPart*
    ;

postfixPart
    : LPAREN argumentList? RPAREN
    | DOT IDENTIFIER
    | LBRACK expression RBRACK
    ;

argumentList
    : expression (COMMA expression)*
    ;

primaryExpression
    : literal
    | IDENTIFIER
    | ifExpression
    | whileExpression
    | doWhileExpression
    | forExpression
    | caseExpression
    | letExpression
    | funLiteral
    | infixReference
    | KW_SKIP
    | LPAREN scopedBody? RPAREN
    ;

ifExpression
    : IF expression THEN scopeExpression (ELIF expression THEN scopeExpression)* (ELSE scopeExpression)? FI
    ;

whileExpression
    : WHILE expression DO scopeExpression OD
    ;

doWhileExpression
    : DO scopeExpression WHILE expression OD
    ;

forExpression
    : FOR forInit COMMA expression COMMA expression DO scopeExpression OD
    ;

scopeExpression
    : scopedBody?
    ;

forInit
    : KW_SKIP
    | varDefinition (SEMI expression)?
    | expression
    ;

caseExpression
    : CASE expression OF caseBranch (PIPE caseBranch)* ESAC
    ;

caseBranch
    : pattern ARROW expression
    ;

letExpression
    : LET letBinding IN expression
    ;

letBinding
    : pattern EQ expression
    ;

funLiteral
    : FUN parameterList blockExpression
    ;

infixReference
    : INFIX operatorSymbol
    ;

literal
    : INTEGER_LITERAL
    | STRING_LITERAL
    | CHAR_LITERAL
    | TRUE
    | FALSE
    | arrayLiteral
    | listLiteral
    ;

arrayLiteral
    : LBRACK (expression (COMMA expression)*)? RBRACK
    ;

listLiteral
    : LBRACE (expression (COMMA expression)*)? RBRACE
    ;

pattern
    : consPattern
    ;

consPattern
    : asPattern (COLON consPattern)?
    ;

asPattern
    : IDENTIFIER AT_SIGN asPattern
    | atomicPattern
    ;

atomicPattern
    : UNDERSCORE
    | typePattern
    | literalPattern
    | IDENTIFIER patternArguments?
    | LBRACK (pattern (COMMA pattern)*)? RBRACK
    | LBRACE (pattern (COMMA pattern)*)? RBRACE
    | LPAREN pattern RPAREN
    ;

patternArguments
    : LPAREN (pattern (COMMA pattern)*)? RPAREN
    ;

typePattern
    : HASH (IDENTIFIER | FUN)
    ;

literalPattern
    : INTEGER_LITERAL
    | STRING_LITERAL
    | CHAR_LITERAL
    | TRUE
    | FALSE
    ;

operatorSymbol
    : ASSIGN_COLON
    | COLON
    | EXCL2
    | ANDAND
    | OROR
    | EQ
    | EQEQ
    | NEQ
    | LT
    | LE
    | GT
    | GE
    | PLUS
    | MINUS
    | STAR
    | SLASH
    | PERCENT
    | OPERATOR
    ;

VAR         : 'var';
FUN         : 'fun';
IF          : 'if';
THEN        : 'then';
ELIF        : 'elif';
ELSE        : 'else';
FI          : 'fi';
WHILE       : 'while';
DO          : 'do';
OD          : 'od';
FOR         : 'for';
KW_SKIP     : 'skip';
TRUE        : 'true';
FALSE       : 'false';
LET         : 'let';
IN          : 'in';
CASE        : 'case';
OF          : 'of';
ESAC        : 'esac';
INFIX       : 'infix';
INFIXL      : 'infixl';
INFIXR      : 'infixr';
AT          : 'at';
BEFORE      : 'before';
AFTER       : 'after';
ETA         : 'eta';

ARROW       : '->';
ASSIGN_COLON: ':=';
EXCL2       : '!!';
ANDAND      : '&&';
OROR        : '||';
EQEQ        : '==';
NEQ         : '!=';
LE          : '<=';
GE          : '>=';
EQ          : '=';
LT          : '<';
GT          : '>';
PLUS        : '+';
MINUS       : '-';
STAR        : '*';
SLASH       : '/';
PERCENT     : '%';
COLON       : ':';
AT_SIGN     : '@';
HASH        : '#';
PIPE        : '|';
DOT         : '.';
COMMA       : ',';
SEMI        : ';';
LPAREN      : '(';
RPAREN      : ')';
LBRACE      : '{';
RBRACE      : '}';
LBRACK      : '[';
RBRACK      : ']';
UNDERSCORE  : '_';

IDENTIFIER
    : [a-zA-Z] [a-zA-Z0-9_]*
    ;

OPERATOR
    : [!$%&*+./:<=>?@\\^|~-]+
    ;

INTEGER_LITERAL
    : [0-9]+
    ;

STRING_LITERAL
    : '"' (ESC | ~["\\\r\n])* '"'
    ;

CHAR_LITERAL
    : '\'' (ESC | ~['\\\r\n]) '\''
    ;

fragment ESC
    : '\\' [btnfr"\\]
    ;

LINE_COMMENT
    : '--' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '(*' .*? '*)' -> skip
    ;

WS
    : [ \t\r\n\f]+ -> skip
    ;
