package com.oracle.truffle.lama.parser;

final class LamaToken {
    final LamaTokenType type;
    final String text;
    final int index;
    final int line;
    final int column;

    LamaToken(LamaTokenType type, String text, int index, int line, int column) {
        this.type = type;
        this.text = text;
        this.index = index;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")@" + line + ":" + column;
    }
}

