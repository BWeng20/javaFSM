package com.bw.fsm.datamodel.ecma;

import com.bw.fsm.ScriptProducer;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Stack;

public class ECMAScriptProducer implements ScriptProducer {
    final static String valueUndefined = "undefined";
    final static String valueNull = "null";


    private enum ElementType {
        MAP,
        ARRAY,
        SIMPLE
    }

    private static final class StackItem {
        int entryIndex = 0;
        ElementType type = ElementType.SIMPLE;
    }

    StackItem current = new StackItem();
    Stack<StackItem> stack = new Stack<>();

    private void push(ElementType newType) {
        stack.push(current);
        current = new StackItem();
        current.type = newType;
    }

    private void pop() {
        current = stack.pop();
    }


    final boolean mapNull2Undefined;
    StringBuilder builder = new StringBuilder(200);
    static NumberFormat nf = NumberFormat.getInstance(Locale.UK);

    public ECMAScriptProducer() {
        this(false);
    }


    public ECMAScriptProducer(boolean mapNull2Undefined) {
        this.mapNull2Undefined = mapNull2Undefined;
    }

    @Override
    public void startArray() {
        push(ElementType.ARRAY);
        builder.append('[');
    }

    @Override
    public void startArrayMember() {
        if (current.type != ElementType.ARRAY)
            throw new IllegalStateException("Called startArrayMember, but array is not the current structure");
        if (current.entryIndex++ > 0)
            builder.append(',');
    }

    @Override
    public void endArrayMember() {
        if (current.type != ElementType.ARRAY)
            throw new IllegalStateException("Called endArrayMember, but array is not the current structure");
    }


    @Override
    public void endArray() {
        if (current.type != ElementType.ARRAY)
            throw new IllegalStateException("Called endArray, but array is not the current structure");
        builder.append(']');
        pop();
    }

    @Override
    public void startMap() {
        push(ElementType.MAP);
        builder.append('{');

    }

    @Override
    public void endMap() {
        if (current.type != ElementType.MAP)
            throw new IllegalStateException("Called endMap, but map is not the current structure");
        builder.append('}');
        pop();
    }


    @Override
    public void startMember(String name) {
        if (current.type != ElementType.MAP)
            throw new IllegalStateException("Called startMember, but map is not the current structure");
        if (current.entryIndex++ > 0)
            builder.append(",");
        builder.append(asStringValue(name)).append(':');
    }

    @Override
    public void endMember() {
        if (current.type != ElementType.MAP)
            throw new IllegalStateException("Called endMember, but map is not the current structure");
    }

    @Override
    public void addNull() {
        builder.append(mapNull2Undefined ? valueUndefined : valueNull);

    }

    @Override
    public void addValue(Number value) {
        double d = value.doubleValue();
        if (((long) d) == d)
            builder.append(nf.format(((long) d)));
        else
            builder.append(nf.format(d));

    }

    @Override
    public void addToken(String value) {
        if (value == null)
            value = mapNull2Undefined ? valueUndefined : valueNull;
        builder.append(value);

    }

    @Override
    public String asStringValue(String value) {
        return value == null
                ? (mapNull2Undefined ? valueUndefined : valueNull)
                : "'" + value.replace("\\", "\\\\")
                .replace("\n", "\\n'")
                .replace("\r", "\\r'")
                .replace("'", "\\'") + "'";
    }

    public String finish() {
        String r = builder.toString();
        builder.setLength(0);
        do {
            switch (current.type) {
                case MAP -> endMap();
                case ARRAY -> endArray();
                case SIMPLE -> {
                }
            }
            if (stack.empty())
                break;
            pop();
        } while (true);
        current.entryIndex = 0;
        current.type = ElementType.SIMPLE;
        return r;
    }

}
