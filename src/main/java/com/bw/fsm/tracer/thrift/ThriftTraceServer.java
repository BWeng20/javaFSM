package com.bw.fsm.tracer.thrift;

import com.bw.fsm.Log;
import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.NamedArgument;
import com.bw.fsm.thrift.TraceServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ThriftTraceServer implements TraceServer.Iface {

    private Thread serverThread;
    private TServer server;

    private final Map<String, TraceSession> sessions = new HashMap<>();
    private final AtomicInteger fsmCount = new AtomicInteger(0);


    @Override
    public String registerFsm(String clientAddress, String fsmName, int session) {
        Log.info("registerFsm(address:" + clientAddress + ",session:" + session + ")");
        TraceSession ts = new TraceSession("TS" + (fsmCount.incrementAndGet()), clientAddress, fsmName, session);
        synchronized (sessions) {
            sessions.put(ts.fsmId, ts);
        }
        return ts.fsmId;
    }

    @Override
    public void unregisterFsm(String fsmId) {
        TraceSession ts;
        synchronized (sessions) {
            ts = sessions.remove(fsmId);
        }
        if (ts != null) {
            Log.info("unregisterFsm(" + ts + ")");
        } else {
            error(String.format("Tried to unregister unknown fsm '%s'", fsmId));
        }
    }

    @Override
    public void message(String fsmId, String message) {
        TraceSession ts = getSession(fsmId);
        if (ts != null)
            ts.addMessage(message);
        else
            error("Unknown Session '" + fsmId + "'");
        info("message: fsm:" + fsmId + ",message:" + message);
    }

    @Override
    public void sentEvent(String fsmId, Event event) {

        TraceSession ts = getSession(fsmId);
        if (ts != null)
            ts.addEventSent(event);
        else
            error("Unknown Session '" + fsmId + "'");


    }

    @Override
    public void receivedEvent(String fsmId, Event event) {
        TraceSession ts = getSession(fsmId);
        if (ts != null)
            ts.addEventReceived(event);
        else
            error("Unknown Session '" + fsmId + "'");
    }

    @Override
    public void enterMethod(String fsmId, String name, List<NamedArgument> arguments) {
        System.out.println("enterMethod: fsm:" + fsmId + ",name:" + name + ",arguments:" + arguments);

        TraceSession ts = getSession(fsmId);
        if (ts != null)
            ts.addTrace(TraceType.enter, name, arguments);
        else
            error("Unknown Session '" + fsmId + "'");


    }

    @Override
    public void exitMethod(String fsm, String name, List<NamedArgument> results) {
        System.out.println("exitMethod: fsm:" + fsm + ",name:" + name + ",results:" + results);

        TraceSession ts = getSession(fsm);
        if (ts != null)
            ts.addTrace(TraceType.exit, name, results);
        else
            error("Unknown Session '" + fsm + "'");
    }

    protected TraceSession getSession(String fsmId) {
        synchronized (sessions) {
            return sessions.get(fsmId);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            try {
                server.stop();
                serverThread.join();
            } catch (Exception e) {
                Log.exception("Stop of ThriftClient failed", e);
            }
            server = null;
            serverThread = null;
        }

    }

    public synchronized void start() {
        stop();

        Log.info("Starting ThriftTrace Server...");
        try {
            TraceServer.Processor<ThriftTraceServer> processor = new TraceServer.Processor<>(this);
            TServerTransport serverTransport = new TServerSocket(4212);
            server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

            final TServer localServer = server;
            serverThread = new Thread(() -> {
                try {
                    localServer.serve();
                } catch (Exception e) {
                    Log.exception("ThriftTrace Server failed", e);
                }
                info("ThriftTrace Server stopped");
            });
            serverThread.start();
        } catch (Exception e) {
            Log.exception("ThriftTraceServer failed", e);
        }
    }

    protected void error(String message) {
        Log.error("%s", message);
    }

    protected void info(String message) {
        Log.info("%s", message);
    }

    public static void main(String[] args) {
        ThriftTraceServer client = new ThriftTraceServer();
        client.start();

    }
}
