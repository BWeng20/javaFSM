package com.bw.fsm.serializer;

import com.bw.fsm.Data;
import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DefaultProtocolWriter implements ProtocolWriter, DefaultProtocolDefinitions {

    private final OutputStream writer;

    public DefaultProtocolWriter(OutputStream writer) {
        this.writer = writer;
    }

    protected void write_type_and_value(int type_id, long value, int size) throws IOException {
        size -= 4;
        if (size < 0)
            size = 0;
        writer.write(type_id | ((int) (((value >> size)) & 0x0F)));
        while (size > 0) {
            size -= 8;
            if (size < 0)
                size = 0;
            writer.write(((int) (value >> size)) & 0x00FF);
        }
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

    @Override
    public void write_boolean(boolean value) throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug("BOOL %s", value);
        writer.write(value ? FSM_PROTOCOL_TYPE_BOOLEAN_TRUE : FSM_PROTOCOL_TYPE_BOOLEAN_FALSE);
    }

    public void write_option_string(@Nullable String value) throws IOException {
        if (value != null) {
            write_str(value);
        } else {
            writer.write(FSM_PROTOCOL_TYPE_OPT_STRING_NONE);
        }
    }

    public void write_data(Data value) throws IOException {
        switch (value.type) {
            case Integer -> {
                write_long(1);
                write_str(value.toString());
            }
            case Double -> {
                write_long(2);
                write_str(value.toString());
            }
            case String -> {
                write_long(3);
                write_str(value.toString());
            }
            case Boolean -> {
                write_long(4);
                write_boolean(((Data.Boolean) value).value);
            }
            case Array -> {
                Data.Array array = (Data.Array) value;
                write_long(5);
                write_long(array.values.size());
                for (var v : array.values) {
                    write_data(v);
                }
            }
            case Map -> {
                Data.Map map = (Data.Map) value;
                write_long(6);
                write_long(map.values.size());
                for (var entry : map.values.entrySet()) {
                    write_str(entry.getKey());
                    write_data(entry.getValue());
                }
            }
            case Error -> {
                Data.Error err = (Data.Error) value;
                write_long(7);
                write_str(err.toString());
            }
            case Source -> {
                Data.Source source = (Data.Source) value;
                write_long(8);
                write_str(source.source.source);
                write_long(source.source.source_id);
            }
            case None -> write_long(9);
            case Null -> write_long(0);
        }
    }

    public void write_str(@NotNull String value) throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug("String %s", value);
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        var len = data.length;
        if (len < (1L << 4)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_STRING_LENGTH_4BIT, len, 4);
        } else {
            write_type_and_value(FSM_PROTOCOL_TYPE_STRING_LENGTH_12BIT, len, 12);
            len &= 0x0FFF;
        }
        writer.write(data, 0, len);
    }

    public void write_long(long value) throws IOException {
        if (StaticOptions.debug_serializer)
            Log.debug("uint %d", value);
        if (value < (1L << 4)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_4BIT, value, 4);
        } else if (value < (1L << 12)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_12BIT, value, 12);
        } else if (value < (1L << 20)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_20BIT, value, 20);
        } else if (value < (1L << 28)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_28BIT, value, 28);
        } else if (value < (1L << 36)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_36BIT, value, 36);
        } else if (value < (1L << 44)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_44BIT, value, 44);
        } else if (value < (1L << 52)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_52BIT, value, 52);
        } else if (value < (1L << 60)) {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_60BIT, value, 60);
        } else {
            write_type_and_value(FSM_PROTOCOL_TYPE_INT_68BIT, value, 64);
        }
    }

    public OutputStream get_writer() {
        return writer;
    }
}
