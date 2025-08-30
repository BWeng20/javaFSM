package com.bw.fsm.expression_engine;

public class ExpressionException extends Exception {

    public ExpressionException(String message) {
        super(message);
    }

    public ExpressionException(Exception e) {
        super(e);
    }

}
