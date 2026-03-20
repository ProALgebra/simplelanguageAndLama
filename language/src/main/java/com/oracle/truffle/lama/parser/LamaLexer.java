package com.oracle.truffle.lama.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LamaLexer {
    private static final Map<String, LamaTokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("var", LamaTokenType.KW_VAR);
        KEYWORDS.put("fun", LamaTokenType.KW_FUN);
        KEYWORDS.put("if", LamaTokenType.KW_IF);
        KEYWORDS.put("then", LamaTokenType.KW_THEN);
        KEYWORDS.put("elif", LamaTokenType.KW_ELIF);
        KEYWORDS.put("else", LamaTokenType.KW_ELSE);
        KEYWORDS.put("fi", LamaTokenType.KW_FI);
        KEYWORDS.put("while", LamaTokenType.KW_WHILE);
        KEYWORDS.put("do", LamaTokenType.KW_DO);
        KEYWORDS.put("od", LamaTokenType.KW_OD);
        KEYWORDS.put("for", LamaTokenType.KW_FOR);
        KEYWORDS.put("skip", LamaTokenType.KW_SKIP);
        KEYWORDS.put("true", LamaTokenType.KW_TRUE);
        KEYWORDS.put("false", LamaTokenType.KW_FALSE);
        KEYWORDS.put("let", LamaTokenType.KW_LET);
        KEYWORDS.put("in", LamaTokenType.KW_IN);
        KEYWORDS.put("case", LamaTokenType.KW_CASE);
        KEYWORDS.put("of", LamaTokenType.KW_OF);
        KEYWORDS.put("esac", LamaTokenType.KW_ESAC);
        KEYWORDS.put("infix", LamaTokenType.KW_INFIX);
        KEYWORDS.put("infixl", LamaTokenType.KW_INFIXL);
        KEYWORDS.put("infixr", LamaTokenType.KW_INFIXR);
        KEYWORDS.put("at", LamaTokenType.KW_AT_KW);
        KEYWORDS.put("before", LamaTokenType.KW_BEFORE);
        KEYWORDS.put("after", LamaTokenType.KW_AFTER);
    }

    private final String source;
    private final int length;
    private final List<LamaToken> tokens = new ArrayList<>();

    private int index;
    private int line = 1;
    private int col = 1;

    LamaLexer(String source) {
        this.source = source;
        this.length = source.length();
    }

    List<LamaToken> lex() {
        while (!isAtEnd()) {
            char c = peek();
            if (isWhitespace(c)) {
                consumeWhitespace();
                continue;
            }
            if (c == '-' && peekNext() == '-') {
                consumeLineComment();
                continue;
            }
            if (c == '(' && peekNext() == '*') {
                consumeBlockComment();
                continue;
            }

            int startIndex = index;
            int startLine = line;
            int startCol = col;

            switch (c) {
                case '(' -> {
                    advance();
                    add(LamaTokenType.LPAREN, "(", startIndex, startLine, startCol);
                }
                case ')' -> {
                    advance();
                    add(LamaTokenType.RPAREN, ")", startIndex, startLine, startCol);
                }
                case '[' -> {
                    advance();
                    add(LamaTokenType.LBRACK, "[", startIndex, startLine, startCol);
                }
                case ']' -> {
                    advance();
                    add(LamaTokenType.RBRACK, "]", startIndex, startLine, startCol);
                }
                case '{' -> {
                    advance();
                    add(LamaTokenType.LBRACE, "{", startIndex, startLine, startCol);
                }
                case '}' -> {
                    advance();
                    add(LamaTokenType.RBRACE, "}", startIndex, startLine, startCol);
                }
                case ',' -> {
                    advance();
                    add(LamaTokenType.COMMA, ",", startIndex, startLine, startCol);
                }
                case ';' -> {
                    advance();
                    add(LamaTokenType.SEMI, ";", startIndex, startLine, startCol);
                }
                case '.' -> {
                    advance();
                    add(LamaTokenType.DOT, ".", startIndex, startLine, startCol);
                }
                case '@' -> {
                    advance();
                    add(LamaTokenType.AT, "@", startIndex, startLine, startCol);
                }
                case '#' -> {
                    advance();
                    add(LamaTokenType.HASH, "#", startIndex, startLine, startCol);
                }
                case '"' -> lexString(startIndex, startLine, startCol);
                case '\'' -> lexChar(startIndex, startLine, startCol);
                default -> {
                    if (isDigit(c)) {
                        lexInt(startIndex, startLine, startCol);
                    } else if (isIdentStart(c)) {
                        lexIdent(startIndex, startLine, startCol);
                    } else if (isOperatorChar(c)) {
                        lexOperator(startIndex, startLine, startCol);
                    } else {
                        throw new LamaLexException("Unexpected character '" + c + "'", startLine, startCol);
                    }
                }
            }
        }
        tokens.add(new LamaToken(LamaTokenType.EOF, "", index, line, col));
        return tokens;
    }

    private void lexInt(int startIndex, int startLine, int startCol) {
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }
        add(LamaTokenType.INT, source.substring(startIndex, index), startIndex, startLine, startCol);
    }

    private void lexIdent(int startIndex, int startLine, int startCol) {
        advance();
        while (!isAtEnd() && isIdentPart(peek())) {
            advance();
        }
        String text = source.substring(startIndex, index);
        LamaTokenType keyword = KEYWORDS.get(text);
        if (keyword != null) {
            add(keyword, text, startIndex, startLine, startCol);
            return;
        }
        if (Character.isUpperCase(text.charAt(0))) {
            add(LamaTokenType.UIDENT, text, startIndex, startLine, startCol);
        } else {
            add(LamaTokenType.IDENT, text, startIndex, startLine, startCol);
        }
    }

    private void lexOperator(int startIndex, int startLine, int startCol) {
        while (!isAtEnd() && isOperatorChar(peek())) {
            if (peek() == '-' && peekNext() == '-') {
                break;
            }
            advance();
        }
        String op = source.substring(startIndex, index);
        if ("|".equals(op)) {
            add(LamaTokenType.PIPE, op, startIndex, startLine, startCol);
        } else if ("->".equals(op)) {
            add(LamaTokenType.ARROW, op, startIndex, startLine, startCol);
        } else {
            add(LamaTokenType.OP, op, startIndex, startLine, startCol);
        }
    }

    private void lexString(int startIndex, int startLine, int startCol) {
        advance(); // opening quote
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                add(LamaTokenType.STRING, sb.toString(), startIndex, startLine, startCol);
                return;
            }
            if (c == '\n' || c == '\r') {
                throw new LamaLexException("Unterminated string literal", startLine, startCol);
            }
            sb.append(c);
        }
        throw new LamaLexException("Unterminated string literal", startLine, startCol);
    }

    private void lexChar(int startIndex, int startLine, int startCol) {
        advance(); // opening '
        if (isAtEnd()) {
            throw new LamaLexException("Unterminated char literal", startLine, startCol);
        }
        char c = advance();
        if (isAtEnd() || advance() != '\'') {
            throw new LamaLexException("Invalid char literal", startLine, startCol);
        }
        add(LamaTokenType.CHAR, Character.toString(c), startIndex, startLine, startCol);
    }

    private void consumeWhitespace() {
        while (!isAtEnd() && isWhitespace(peek())) {
            advance();
        }
    }

    private void consumeLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private void consumeBlockComment() {
        advance(); // (
        advance(); // *
        int depth = 1;
        while (!isAtEnd()) {
            if (peek() == '(' && peekNext() == '*') {
                advance();
                advance();
                depth++;
                continue;
            }
            if (peek() == '*' && peekNext() == ')') {
                advance();
                advance();
                depth--;
                if (depth == 0) {
                    return;
                }
                continue;
            }
            advance();
        }
        throw new LamaLexException("Unterminated block comment", line, col);
    }

    private boolean isAtEnd() {
        return index >= length;
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        if (index + 1 >= length) {
            return 0;
        }
        return source.charAt(index + 1);
    }

    private char advance() {
        char c = source.charAt(index++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private void add(LamaTokenType type, String text, int index, int line, int col) {
        tokens.add(new LamaToken(type, text, index, line, col));
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return isIdentStart(c) || isDigit(c) || c == '\'';
    }

    private static boolean isOperatorChar(char c) {
        return "!$%&*+-/:<=>?@^|~".indexOf(c) >= 0;
    }
}
