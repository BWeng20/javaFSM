package com.bw.fsm.tracer.thrift;

import com.bw.fsm.thrift.Argument;
import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.NamedArgument;

import java.util.List;

public class TraceEntry {

    public final TraceType type;
    public List<Argument> values;
    public List<NamedArgument> arguments;
    public Event event;
    public String description;

    public final long time;

    public TraceEntry(TraceType type) {
        this.type = type;
        this.time = System.currentTimeMillis();
    }

}
