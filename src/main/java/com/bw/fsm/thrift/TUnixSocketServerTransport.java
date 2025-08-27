package com.bw.fsm.thrift;

import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Needs Java 16+
 */
public class TUnixSocketServerTransport extends TServerTransport {

    private final Path socketPath;
    private ServerSocketChannel serverChannel;
    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    public TUnixSocketServerTransport(Path socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public void listen() throws TTransportException {
        try {
            serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            serverChannel.bind(UnixDomainSocketAddress.of(socketPath));
        } catch (IOException e) {
            throw new TTransportException("Failed to bind Unix Domain Socket", e);
        }
    }

    @Override
    public TTransport accept() throws TTransportException {
        try {
            if (interrupted.get()) {
                throw new TTransportException("Server interrupted");
            }
            SocketChannel clientChannel = serverChannel.accept();
            return new TUnixSocketTransport(clientChannel);
        } catch (IOException e) {
            throw new TTransportException("Failed to accept connection", e);
        }
    }

    @Override
    public void close() {
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void interrupt() {
        interrupted.set(true);
        close();
    }
}
