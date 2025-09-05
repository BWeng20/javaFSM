package com.bw.fsm;

import java.util.ArrayList;

public class Transition {

    public int id;
    public int doc_id;

    // TODO: Possibly we need some type to express event ids
    public java.util.List<String> events;
    public boolean wildcard;
    public Data cond = Data.None.NONE;
    public State source;
    public final java.util.List<State> target = new ArrayList<>(1);
    public TransitionType transition_type = TransitionType.External;
    public ExecutableContentBlock content;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(20);
        stringBuilder.append("Transition ").append(source).append(" -> ");
        for (State t : target)
            stringBuilder.append(t);
        return stringBuilder.toString();
    }


    /**
     * <b>W3C says</b>:<br>
     * An event descriptor matches an event name if its string of tokens is an exact match or a prefix
     * of the set of tokens in the event's name. In all cases, the token matching is case sensitive.<br>
     * For example, a transition with an 'event' attribute of "error foo" will match event names
     * "error", "error.send", "error.send.failed", etc. (or "foo", "foo.bar" etc.) but would not
     * match events named "errors.my.custom", "errorhandler.mistake", "error.send" or "foobar".<br>
     * For compatibility with CCXML, and to make the prefix matching possibly more clear to a reader
     * of the SCXML document, an event descriptor MAY also end with the wildcard '.*', which matches
     * zero or more tokens at the end of the processed event's name. Note that a transition with
     * 'event' of "error", one with "error.", and one with "error.*" are functionally equivalent
     * since they are token prefixes of exactly the same set of event names.<br>
     * <br>
     * Implementation Note:<br>
     * Terminating "." and ".*" are already stripped by the parser.
     */
    public boolean nameMatch(String name) {
        if (this.wildcard) {
            return true;
        } else {
            for (String e : this.events) {
                if (name.startsWith(e)) {
                    if (name.length() == e.length()) {
                        // Full match
                        return true;
                    } else if (name.length() > e.length()) {
                        // partial match, token needs to be terminated with "."
                        if (name.charAt(e.length()) == '.') {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

}
