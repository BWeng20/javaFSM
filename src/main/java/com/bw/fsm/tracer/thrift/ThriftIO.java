package com.bw.fsm.tracer.thrift;

import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.net.URL;

public class ThriftIO {

    public static TTransport detectTransformFromAddress(String address) throws TTransportException {
        try {
            URL url = new URL(address);
            if ("http".equalsIgnoreCase(url.getProtocol())) {
                return new THttpClient(address);
            } else {
                throw new TTransportException("Unsupported protocol: " + address);
            }
        } catch (Exception e) {
        }
        try {
            int i = address.indexOf(':');
            String host = address.substring(0, i);
            int port = Integer.parseInt(address.substring(i + 1));
            return new TSocket(host, port);
        } catch (NullPointerException | NumberFormatException ne) {
            throw new TTransportException("Unsupported protocol: " + address, ne);
        }
    }

}
