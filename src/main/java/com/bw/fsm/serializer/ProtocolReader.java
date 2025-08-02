package com.bw.fsm.serializer;

import com.bw.fsm.Data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Trait for reading binary data in some platform independent way.<br>
 * The resulting data should be sharable with different systems (different OS, Byte-Order... whatever).
 */
public interface ProtocolReader<R extends InputStream> {


    /**
     * Reads and verify the protocol version
     * Goes to error state if version doesn't match.
     */
    void verify_version() throws IOException;

    /**
     * Close the underlying stream
     */
    void close();

    /**
     * Reads a boolean
     */
    boolean read_boolean() throws IOException;

    /**
     * Reads an optional string
     */
    String read_option_string() throws IOException;

    /**
     * Reads a Data (enum) value
     */
    Data read_data() throws IOException;

    /**
     * Reads a string
     */
    String read_string() throws IOException;

    /**
     * Reads long value
     */
    long read_long() throws IOException;


    boolean has_error();

}
