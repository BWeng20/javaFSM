package com.bw.fsm.thrift;

import org.apache.thrift.transport.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Tool class to create thrift transports dynamically from URLs.
 */
public final class ThriftIO {

    private static InetSocketAddress parseHostPortAddress(String address) throws TTransportException {
        int portIndex = address.indexOf(':');
        int port;
        if (portIndex > 0) {
            try {
                port = Integer.parseInt(address.substring(portIndex + 1));
            } catch (NumberFormatException ne) {
                throw new TTransportException("Port part has wrong format: " + address);
            }
        } else {
            portIndex = address.length();
            port = 4211;
        }
        String host = address.substring(0, portIndex);
        return new InetSocketAddress(host, port);
    }

    private static String getRequiredProperty(String key) throws TTransportException {
        String value = System.getProperty(key);
        if (value == null)
            throw new TTransportException("Missing property '" + key + "'");
        return value;
    }

    public static int getPropertyAsInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    /**
     * Creates a server transport from url.
     *
     * @param address The url of the server. Valid "protocols":<ul>
     *                <li>unix - creates to a local unix domain socket. E.g. "unix:/thrift/service1"</li>
     *                <li>tcp - creates a tcp socket. E.g. "tcp:localhost:1234". Used System properties:
     *                <li>tcp.timeout (optional, default is 10000)</li>
     *                </li>
     *                <li>ssl - creates a ssl socket. E.g. "ssl:localhost:1234".<br>
     *                Used System properties:<ul>
     *                <li>ssl.keystore.path (required)</li>
     *                <li>ssl.keystore.password (required)</li>
     *                <li>ssl.truststore.path (required)</li>
     *                <li>ssl.truststore.password (required)</li>
     *                <li>ssl.timeout (optional, default is 10000)</li>
     *                </ul>
     *                </li>
     *                </ul>
     * @return Return the Transport protocol.
     * @throws TTransportException
     */
    public static TServerTransport createServerTransportFromAddress(String address) throws TTransportException {
        int protocolIndex = address.indexOf(':');
        if (protocolIndex <= 0) {
            throw new TTransportException("Missing protocol in " + address);
        }
        String protocol = address.substring(0, protocolIndex).toLowerCase(Locale.CANADA);

        switch (protocol) {
            case "unix": {
                return new TUnixSocketServerTransport(Paths.get(address.substring(protocolIndex + 1)));
            }
            case "tcp": {
                var iadr = parseHostPortAddress(address.substring(protocolIndex + 1));
                return new TServerSocket(iadr, getPropertyAsInt("tcp.timeout", 10000));
            }
            case "ssl": {
                // TODO
                var iadr = parseHostPortAddress(address.substring(protocolIndex + 1));
                TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
                params.setKeyStore(getRequiredProperty("ssl.keystore.path"), getRequiredProperty("ssl.keystore.password"));
                params.setTrustStore(getRequiredProperty("ssl.truststore.path"), getRequiredProperty("ssl.truststore.password"));
                return TSSLTransportFactory.getServerSocket(
                        iadr.getPort(),
                        getPropertyAsInt("ssl.timeout", 10000),
                        iadr.getAddress(), params);
            }
            default: {
                throw new TTransportException("Unsupported protocol: " + address);
            }
        }
    }

    /**
     * Creates a client transport from url.
     *
     * @param address The url of the client. Valid "protocols":<ul>
     *                <li>unix - a unix domain socket path. E.g. "unix:/thrift/service1"</li>
     *                <li>tcp - a tcp socket address. E.g. "tcp:localhost:1234"</li>
     *                <li>ssl - a ssl socket address. E.g. "ssl:localhost:1234".<br>
     *                Used System properties:<ul>
     *                <li>ssl.truststore.path (required)</li>
     *                <li>ssl.truststore.password (required)</li>
     *                <li>ssl.timeout (optional, default is 10000)</li>
     *                </ul>
     *                </li>
     *                </ul>
     * @return Return the Transport protocol.
     * @throws TTransportException
     */
    public static TTransport createClientTransportFromAddress(String address) throws TTransportException {
        try {
            int protocolIndex = address.indexOf(':');

            if (protocolIndex <= 0) {
                throw new TTransportException("Missing protocol in " + address);
            }
            String protocol = address.substring(0, protocolIndex).toLowerCase(Locale.CANADA);

            switch (protocol) {
                case "unix": {
                    return new TUnixSocketTransport(Paths.get(address.substring(protocolIndex + 1)));
                }
                case "tcp": {
                    var iadr = parseHostPortAddress(address.substring(protocolIndex + 1));
                    return new TSocket(iadr.getHostName(), iadr.getPort());
                }
                case "ssl": {
                    var iadr = parseHostPortAddress(address.substring(protocolIndex + 1));
                    TSSLTransportFactory.TSSLTransportParameters clientParams = new TSSLTransportFactory.TSSLTransportParameters();
                    clientParams.setTrustStore(getRequiredProperty("ssl.truststore.path"), getRequiredProperty("ssl.truststore.password"));
                    return TSSLTransportFactory.getClientSocket(
                            iadr.getHostName(), iadr.getPort(),
                            getPropertyAsInt("ssl.timeout", 10000), clientParams);
                }
                default: {
                    throw new TTransportException("Unsupported protocol: " + address);
                }
            }
        } catch (IOException ie) {
            throw new TTransportException(ie);
        }
    }

}
