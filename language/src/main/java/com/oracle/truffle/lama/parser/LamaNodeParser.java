package com.oracle.truffle.lama.parser;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public final class LamaNodeParser {
    private LamaNodeParser() {
    }

    public static LamaExpressionNode parse(Source source) {
        LamaLanguageLexer lexer = new LamaLanguageLexer(CharStreams.fromString(source.getCharacters().toString()));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new LamaErrorListener(source));

        LamaLanguageParser parser = new LamaLanguageParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new LamaErrorListener(source));

        LamaLanguageParser.ProgramContext program = parser.program();
        return new LamaAstBuilderVisitor(source).visitProgram(program);
    }

    private static final class LamaErrorListener extends BaseErrorListener {
        private final Source source;

        LamaErrorListener(Source source) {
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                        RecognitionException e) {
            throw new LamaParseError(source, line, charPositionInLine + 1, 1, msg);
        }
    }
}
