package com.bw.fsm.executableContent;

/**
 * Stores &lt;param> elements for &lt;send>, &lt;donedata> or &lt;invoke>
 */
public class Parameter {
    public String name;
    public String expr;
    public String location;

    public boolean hasLocation() {
        return location != null && !location.isEmpty();
    }

    public boolean hasExpression() {
        return expr != null && !expr.isEmpty();
    }
}
