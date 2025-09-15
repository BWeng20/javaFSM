package com.bw.fsm.eventIoProcessor.thrift;

import com.bw.fsm.Log;
import com.bw.fsm.thrift.Event;
import com.bw.fsm.thrift.EventIOProcessor;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.util.List;

public class ThriftEventIOProcessorServer implements EventIOProcessor.Iface {

    Thread serverThread;
    TServer server;

    @Override
    public void sendEvent(int session, Event event) {
        System.out.println("Event received. For Session " + session + " Event " + event.name);
    }

    @Override
    public List<String> getConfiguration(int session) throws TException {
        // TODO
        return List.of();
    }

    public synchronized void stop() {
        if (server != null) {
            try {
                server.stop();
                serverThread.join();
            } catch (Exception e) {
                Log.exception("Stop of Thrift-EventIOProcessor failed", e);
            }
            server = null;
            serverThread = null;
        }

    }

    public synchronized void start() {
        stop();

        Log.info("Starting Thrift-EventIOProcessor...");
        try {
            EventIOProcessor.Processor<ThriftEventIOProcessorServer> processor = new EventIOProcessor.Processor<>(this);
            TServerTransport serverTransport = new TServerSocket(4213);
            server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

            final TServer localServer = server;
            serverThread = new Thread(() -> {
                try {
                    localServer.serve();
                } catch (Exception e) {
                    Log.exception("Thrift-EventIOProcessor Server failed", e);
                }
                Log.info("hrift-EventIOProcessor Server stopped");
            });
            serverThread.start();
        } catch (Exception e) {
            Log.exception("Thrift-EventIOProcessor failed", e);
        }
    }
}
