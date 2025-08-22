package com.bw.fsm;

public class ParamPair {
    public String name;
    public Data value;

    public ParamPair(String name, Data data) {
        this.name = name;
        this.value = data;
    }

    @Override
    public String toString() {
        return name + "=" + String.valueOf(value);
    }
}
