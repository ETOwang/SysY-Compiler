package cn.edu.nju.software.frontend.lexer;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class LexerErrorListener extends BaseErrorListener {
    private boolean error;
    public LexerErrorListener() {
        this.error = false;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPosInLine,
                            String msg, RecognitionException e) {
        error = true;
        msg = "Unexpected token " + msg.substring(msg.length() - 3) + '.';
        System.out.println("Error type A at Line " + line + ": " + msg);
    }

    public boolean noLexerError() {
        return !error;
    }
}
