package com.bw.fsm.tracer.thrift;

import com.bw.fsm.Log;
import com.bw.fsm.thrift.Data;
import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.TraceClient;
import com.bw.fsm.thrift.TraceServer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class ThriftTraceClient implements TraceClient.Iface {

    Thread serverThread;
    TServer server;
    TraceServer.Client traceServer;
    TTransport traceServerTransport;

    @Override
    public void event(String fsm, Event event) throws TException {

    }

    @Override
    public Data getData(String fsm, String expression)  {
        return null;
    }

    public ThriftTraceClient() {
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
        if (traceServerTransport != null) {
            try {
                // TODO:
                // traceServerTransport.unregister...
                traceServerTransport.close();
            } catch (Exception e) {
                Log.exception("Failed to disconnect from TraceServer", e);
            }
            traceServerTransport = null;
            traceServer = null;
        }

    }

    public synchronized void start(String serverAddress) {
        stop();
        String r;
        try {
            traceServerTransport = ThriftIO.detectTransformFromAddress(serverAddress);
            traceServerTransport.open();
            TProtocol protocol = new TBinaryProtocol(traceServerTransport);
            traceServer = new TraceServer.Client(protocol);
            r = traceServer.registerFsm("localhost:4211", 1);
        } catch (TTransportException te) {
            Log.exception("Failed to connect to TraceServer", te);
            return;
        } catch (TException e) {
            Log.exception("Failed to registwer FSM", e);
            return;
        }
        Log.info("Registered: " + r);

        Log.info("Starting Thrift Client...");
        try {
            TraceClient.Processor<ThriftTraceClient> processor = new TraceClient.Processor<>(this);
            TServerTransport serverTransport = new TServerSocket(4211);
            server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

            final TServer localServer = server;
            serverThread = new Thread(() -> {

                try {
                    localServer.serve();
                } catch (Exception e) {
                    Log.exception("ThriftClient failed", e);
                }
                Log.info("Thrift Client stopped");
            });
            serverThread.start();
        } catch (Exception e) {
            Log.exception("ThriftClient failed", e);
        }
    }

    public static void main(String[] args) {
        ThriftTraceClient client = new ThriftTraceClient();
        client.start("localhost:4212");

    }
}
