package com.bw.fsm.expression_engine;

public final class Pair {

    public Expression key;
    public Expression value;

    public Pair(Expression key, Expression value) {
        this.key = key;
        this.value = value;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append('{');
        sb.append(key);
        sb.append(" = ");
        sb.append(key);
        sb.append('}');
        return sb.toString();
    }
}
