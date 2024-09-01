package cn.edu.nju.software.frontend.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ParserErrorListener extends BaseErrorListener {
    public ParserErrorListener() {
        this.error = false;
    }
    private boolean error;
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPosInLine,
                            String msg, RecognitionException e) {
        error = true;
        System.out.println("Error type B at Line " + line + ": " + msg);
    }

    public boolean noParseError() {
        return !error;
    }
}
