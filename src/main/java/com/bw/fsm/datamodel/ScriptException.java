package com.bw.fsm.datamodel;

public class ScriptException extends Exception {

    public ScriptException(String script, Exception error) {
        super(String.format("Execution of '%s' failed. %s", script, error.getMessage()), error);
    }


}
