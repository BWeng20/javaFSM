package com.bw.fsm.tracer.thrift;

import com.bw.fsm.Log;
import com.bw.fsm.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;

/**
 * Client side of a ThriftTracer.
 */
public class ThriftTraceClient implements TraceClient.Iface {

    protected Thread serverThread;
    protected TServer server;
    protected TraceServer.Client traceServer;
    protected TTransport transport;

    @Override
    public void event(String fsm, Event event) throws TException {
    }

    @Override
    public Data getData(String fsm, String expression) {
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
        if (transport != null) {
            try {
                // TODO:
                // traceServerTransport.unregister...
                transport.close();
            } catch (Exception e) {
                Log.exception("Failed to disconnect from TraceServer", e);
            }
            transport = null;
            traceServer = null;
        }

    }

    public synchronized void start(String address) {
        stop();
        Log.info("Starting Thrift Client...");
        try {
            TraceClient.Processor<ThriftTraceClient> processor = new TraceClient.Processor<>(this);
            TServerTransport serverTransport = ThriftIO.createServerTransportFromAddress(address);
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
        client.start("tcp:localhost:4212");
    }
}
