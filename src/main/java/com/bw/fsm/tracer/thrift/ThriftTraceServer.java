package com.bw.fsm.tracer.thrift;

import com.bw.fsm.Log;
import com.bw.fsm.thrift.Argument;
import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.TraceServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.util.List;

public class ThriftTraceServer implements TraceServer.Iface {

    Thread serverThread;
    TServer server;


    @Override
    public String registerFsm(String clientAddress, int session) {
        System.out.println("registerFsm(address:" + clientAddress + ",session:" + session + ")");
        return "abc";
    }

    @Override
    public void message(String fsm, String message) {
        System.out.println("message: fsm:" + fsm + ",message:" + message);
    }

    @Override
    public void sentEvent(Event event) {
        System.out.println("sentEvent: event:" + event);

    }

    @Override
    public void receivedEvent(Event event) {
        System.out.println("receivedEvent(event:" + event);
    }

    @Override
    public void enterMethod(String fsm, String name, List<Argument> arguments) {
        System.out.println("enterMethod: fsm:" + fsm + ",name:" + name + ",arguments:" + arguments);

    }

    @Override
    public void exitMethod(String fsm, String name, List<Argument> results) {
        System.out.println("exitMethod: fsm:" + fsm + ",name:" + name + ",results:" + results);

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
                Log.info("ThriftTrace Server stopped");
            });
            serverThread.start();
        } catch (Exception e) {
            Log.exception("ThriftTraceServer failed", e);
        }
    }


    public static void main(String[] args) {
        ThriftTraceServer client = new ThriftTraceServer();
        client.start();

    }
}
