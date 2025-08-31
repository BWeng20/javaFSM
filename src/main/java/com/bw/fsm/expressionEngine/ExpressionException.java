package com.bw.fsm.expressionEngine;

public class ExpressionException extends Exception {

    public ExpressionException(String message) {
        super(message);
    }

    public ExpressionException(Exception e) {
        super(e);
    }

}
