package com.bw.fsm.thrift;

import org.apache.thrift.TConfiguration;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Needs Java 16+
 */
public class TUnixSocketTransport extends TTransport {
    private final SocketChannel channel;
    private TConfiguration configuration;

    public TUnixSocketTransport(Path socketPath) throws IOException {
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        this.channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        this.channel.connect(address);
    }

    public TUnixSocketTransport(SocketChannel channel) throws IOException {
        this.channel = channel;
    }

    @Override
    public boolean isOpen() {
        return channel.isConnected();
    }

    @Override
    public void open() throws TTransportException {
        if (isOpen())
            return;
        throw new TTransportException("Channel is not open");
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        try {
            return channel.read(buffer);
        } catch (IOException e) {
            throw new TTransportException(e);
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
        ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
        try {
            channel.write(buffer);
        } catch (IOException e) {
            throw new TTransportException(e);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public TConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void updateKnownMessageSize(long size) {
    }

    @Override
    public void checkReadBytesAvailable(long numBytes) {
    }
}