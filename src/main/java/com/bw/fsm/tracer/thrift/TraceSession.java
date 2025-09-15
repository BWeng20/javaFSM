package com.bw.fsm.tracer.thrift;

import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.NamedArgument;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TraceSession {

    public final long startMillis;
    public final String clientAddress;
    public final String fsmId;
    public final int session;
    public final String fsmName;

    public TraceSession(String fsmId, String clientAddress, String fsmName, int session) {
        startMillis = System.currentTimeMillis();
        this.fsmId = fsmId;
        this.clientAddress = clientAddress;
        this.session = session;
        this.fsmName = fsmName;
    }

    public final LinkedList<TraceEntry> traces = new LinkedList<>();

    public void addMessage(String message) {
        TraceEntry e = new TraceEntry(TraceType.message);
        e.description = message;
        synchronized (traces) {
            traces.add(e);
        }
    }

    public void addEventReceived(Event event) {
        TraceEntry e = new TraceEntry(TraceType.eventReceived);
        e.event = event;
        synchronized (traces) {
            traces.add(e);
        }
    }

    public void addEventSent(Event event) {
        TraceEntry e = new TraceEntry(TraceType.eventSent);
        e.event = event;
        synchronized (traces) {
            traces.add(e);
        }
    }

    public void cleanUp() {
        long ct = System.currentTimeMillis() - (10 * 60 * 1000);
        while (!(traces.isEmpty() || traces.getFirst().time < ct)) {
            traces.removeFirst();
        }
    }


    public void addTrace(TraceType traceType, String name, List<NamedArgument> arguments) {
        TraceEntry e = new TraceEntry(traceType);
        e.description = name;
        e.arguments = new ArrayList<>(arguments);
        synchronized (traces) {
            traces.add(e);
        }
    }
}
