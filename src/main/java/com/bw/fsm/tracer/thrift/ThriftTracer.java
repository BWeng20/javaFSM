package com.bw.fsm.tracer.thrift;


import com.bw.fsm.Log;
import com.bw.fsm.thrift.NamedArgument;
import com.bw.fsm.thrift.ThriftIO;
import com.bw.fsm.thrift.TraceServer;
import com.bw.fsm.tracer.TraceArgument;
import com.bw.fsm.tracer.TraceMode;
import com.bw.fsm.tracer.Tracer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThriftTracer extends Tracer {

    private TraceServer.Iface server;
    private final Map<Integer, TraceSession> sessions = new HashMap<>();
    private final String clientAddress;
    private ThriftTraceClient client;

    public ThriftTracer(String serverAddress, String clientAddress) {
        this.clientAddress = clientAddress;
        try {
            TTransport transport = ThriftIO.createClientTransportFromAddress(serverAddress);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            server = new TraceServer.Client(protocol);
        } catch (TTransportException te) {
            Log.exception("ThriftTrace: Failed to connect to Server", te);
        }
        client = new ThriftTraceClient();
        client.start(clientAddress);
    }

    @Override
    public void stop() {
        if (client != null) {
            client.stop();
            client = null;
        }
        TraceServer.Iface serverTmp;
        Map<Integer, TraceSession> sessionsTmp;

        synchronized (sessions) {
            serverTmp = server;
            server = null;
            sessionsTmp = new HashMap<>(sessions);
            sessions.clear();
        }

        try {
            for (var session : sessionsTmp.values()) {
                serverTmp.unregisterFsm(session.fsmId);
            }
        } catch (TException te) {
            Log.exception("ThriftTrace: Failed to clean up", te);
        }
        Log.info("ThriftTrace stopped");
    }


    @Override
    public void registerSession(int sessionId, String fsmName) {
        try {
            String fsmId;
            if (server != null) {
                fsmId = server.registerFsm(clientAddress, fsmName, sessionId);
            } else {
                Log.error("ThriftTrace: No Server");
                fsmId = fsmName + ":" + sessionId;
            }
            TraceSession ts = new TraceSession(fsmId, clientAddress, fsmName, sessionId);
            synchronized (sessions) {
                sessions.put(sessionId, ts);
                Log.info("ThriftTrace: %s (session %d) registered as %s", fsmName, sessionId, fsmId);
            }
        } catch (TException te) {
            Log.exception(
                    String.format("ThriftTrace: Registration of %s (session %d) failed: %s", fsmName, sessionId, te.getMessage()),
                    te);
        }
    }

    @Override
    public void removeSession(int sessionId) {
        TraceSession ts = getSession(sessionId);
        if (ts != null) {
            try {
                if (server != null)
                    server.unregisterFsm(ts.fsmId);
                else
                    Log.error("ThriftTrace: No Server");

                synchronized (sessions) {
                    sessions.remove(sessionId);
                }
                Log.info("ThriftTrace: %s (session %d) un-registered as %s", ts.fsmName, ts.session, ts.fsmId);
            } catch (TException te) {
                Log.exception(
                        String.format("ThriftTrace: Un-registration of %s (session %d) failed: %s", ts.fsmName, sessionId, te.getMessage()), te);
            }
        }
    }

    @Override
    public void trace(int sessionId, String msg) {
        TraceSession ts = getSession(sessionId);
        if (ts != null) {
            try {
                if (server != null)
                    server.message(ts.fsmId, msg);
                else
                    Log.error("ThriftTrace: No Server");
            } catch (TException te) {
                logThriftException(te);
            }
        }
    }

    private void logThriftException(TException te) {
        Log.exception("ThriftTrace: Call failed", te);
    }


    @Override
    public void enable_trace(TraceMode flag) {
    }

    @Override
    public void disable_trace(TraceMode flag) {

    }

    @Override
    public boolean is_trace(TraceMode flag) {
        return true;
    }

    @Override
    public void enter_method(int sessionId, String what, TraceArgument... arguments) {

        if (server == null)
            Log.error("ThriftTrace: No Server");
        else {
            TraceSession ts = getSession(sessionId);
            if (ts != null) {
                try {
                    List<NamedArgument> args = new ArrayList<>(arguments.length);
                    for (TraceArgument a : arguments) {
                        NamedArgument ta = new NamedArgument();
                        ta.name = a.name;
                        ta.value = a.value == null ? null : a.value.toString();
                        args.add(ta);
                    }
                    server.enterMethod(ts.fsmId, what, args);
                } catch (TException te) {
                    logThriftException(te);
                }
            }
        }
    }


    @Override
    public void exit_method(int sessionId, String what, TraceArgument... results) {
        if (server == null)
            Log.error("ThriftTrace: No Server");
        else {
            TraceSession ts = getSession(sessionId);
            if (ts != null) {
                try {
                    List<NamedArgument> args = new ArrayList<>(results.length);
                    for (TraceArgument a : results) {
                        NamedArgument ta = new NamedArgument();
                        ta.name = a.name;
                        ta.value = a.value == null ? null : a.value.toString();
                        args.add(ta);
                    }
                    server.exitMethod(ts.fsmId, what, args);
                } catch (TException te) {
                    logThriftException(te);
                }
            }
        }
    }

    protected TraceSession getSession(int session) {
        synchronized (sessions) {
            Integer key = session;
            TraceSession ts = sessions.get(key);
            if (ts == null) {
                Log.error("ThriftTrace: Unknown session " + session);
            }
            return ts;

        }
    }

}
