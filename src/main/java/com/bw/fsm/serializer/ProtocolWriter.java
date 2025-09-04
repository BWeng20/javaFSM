package com.bw.fsm.serializer;

import com.bw.fsm.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Trait for writing binary data in some platform independent way.<br>
 * The resulting data should be sharable with different systems (different OS, Byte-Order... whatever).
 */
public interface ProtocolWriter {

    /**
     * Flush and close the underlying stream
     */
    void close() throws IOException;

    /**
     * Writes a boolean
     */
    void write_boolean(boolean value) throws IOException;

    /**
     * Writes an optional string
     */
    void write_option_string(@Nullable String value) throws IOException;

    /**
     * Writes a Data Value
     */
    void write_data(Data value) throws IOException;

    /**
     * Writes a str
     */
    void write_str(@NotNull String value) throws IOException;

    /**
     * Writes an unsigned value
     */
    void write_long(long value) throws IOException;

    OutputStream get_writer();

}
