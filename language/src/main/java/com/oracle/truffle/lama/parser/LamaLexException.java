package com.oracle.truffle.lama.parser;

final class LamaLexException extends RuntimeException {
    private final int line;
    private final int col;

    LamaLexException(String message, int line, int col) {
        super(message + " at " + line + ":" + col);
        this.line = line;
        this.col = col;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return col;
    }
}

