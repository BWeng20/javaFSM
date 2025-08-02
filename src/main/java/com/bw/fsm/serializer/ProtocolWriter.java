package com.bw.fsm.serializer;

import com.bw.fsm.Data;

import java.io.OutputStream;

/**
 * Trait for writing binary data in some platform independent way.<br>
 * The resulting data should be sharable with different systems (different OS, Byte-Order... whatever).
 */
public interface ProtocolWriter<W extends OutputStream> {

    /**
     * Writes the protocol version #
     */
    void write_version();

    /**
     * Flush and close the underlying stream
     */
    void close();

    /**
     * Writes a boolean
     */
    void write_boolean(boolean value);

    /**
     * Writes an optional string
     */
    void write_option_string(String value);

    /**
     * Writes a Data Value
     */
    void write_data(Data value);

    /**
     * Writes a str
     */
    void write_str(String value);

    /**
     * Writes an unsigned value
     */
    void write_long(long value);

    boolean has_error();

    W get_writer();

}
