package com.bw.fsm.serializer;

import com.bw.fsm.Data;
import com.bw.fsm.Log;
import com.bw.fsm.StaticOptions;
import com.bw.fsm.datamodel.SourceCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultProtocolReader<R extends InputStream> implements ProtocolReader<R>, DefaultProtocolDefinitions {

    protected R reader;
    protected boolean ok = true;
    protected TypeAndValue type_and_value = new TypeAndValue();
    protected final byte[] buffer = new byte[4096];

    public DefaultProtocolReader() {
    }

    protected void error(String err, Object... arguments) {
        if (ok) {
            Log.error(err, arguments);
            this.ok = false;
            this.type_and_value.type_id = 0;
            this.type_and_value.number = 0;
            this.type_and_value.string = "";
        }
    }

    protected boolean verify_number_type() {
        if (this.ok) {
            return switch (this.type_and_value.type_id) {
                case FSM_PROTOCOL_TYPE_INT_4BIT
                , FSM_PROTOCOL_TYPE_INT_12BIT
                , FSM_PROTOCOL_TYPE_INT_20BIT
                , FSM_PROTOCOL_TYPE_INT_28BIT
                , FSM_PROTOCOL_TYPE_INT_36BIT
                , FSM_PROTOCOL_TYPE_INT_44BIT
                , FSM_PROTOCOL_TYPE_INT_52BIT
                , FSM_PROTOCOL_TYPE_INT_60BIT
                , FSM_PROTOCOL_TYPE_INT_68BIT -> true;
                default -> {
                    error("Expected numeric type, got %d", this.type_and_value.type_id);
                    yield false;
                }
            };
        } else {
            return false;
        }
    }

    protected boolean verify_string_type() {
        if (this.ok) {
            return
                    switch (this.type_and_value.type_id) {
                        case FSM_PROTOCOL_TYPE_STRING_LENGTH_4BIT, FSM_PROTOCOL_TYPE_STRING_LENGTH_12BIT -> true;
                        default -> {
                            error("Expected string type, got #%s", type_and_value.type_id);
                            yield false;
                        }
                    };
        } else {
            return false;
        }
    }

    protected void read_additional_number_bytes(int length) throws IOException {
        while (length > 0 && this.ok) {
            int value = this.reader.read();
            this.type_and_value.number = (this.type_and_value.number << 8) | (value & 0x00FF);
            length -= 1;
        }
    }

    protected Data read_data_value_payload(int what) throws IOException {
        return switch (what) {
            case 0 -> Data.Null.NULL;
            case 1 -> {
                String rv = this.read_string();
                try {
                    yield new Data.Integer(Integer.parseInt(rv));
                } catch (Exception e) {
                    error("Protocol error in Integer data value: %s -> %s", rv, e.getMessage());
                    yield Data.Null.NULL;
                }
            }
            case 2 -> {
                String rv = this.read_string();
                try {
                    yield new Data.Double(Double.parseDouble(rv));
                } catch (Exception e) {
                    error("Protocol error in Double data value: %s -> %s", rv, e.getMessage());
                    yield Data.Null.NULL;
                }
            }
            case 3 -> new Data.String(this.read_string());
            case 4 -> new Data.Boolean(this.read_boolean());
            case 5 -> {
                int len = (int) this.read_long();
                List<Data> val = new ArrayList<>(len);
                while (len > 0) {
                    val.add(this.read_data());
                    --len;
                }
                yield new Data.Array(val);
            }
            case 6 -> {
                int len = (int) this.read_long();
                Map<String, Data> val = new HashMap<>();
                while (len > 0) {
                    String k = this.read_string();
                    val.put(k, this.read_data());
                    --len;
                }
                yield new Data.Map(val);
            }
            case 7 -> new Data.Error(this.read_string());
            case 8 -> {
                String k = this.read_string();
                int id = (int) this.read_long();
                yield new Data.Source(new SourceCode(k, id));
            }
            case 9 -> Data.None.NONE;
            default -> {
                error("Protocol error in data value: unknown variant %s", what);
                yield Data.Null.NULL;
            }
        };
    }

    protected void read_type_and_size() throws IOException {
        if (this.ok) {
            this.type_and_value.string = "";
            int val = this.reader.read();
            switch (val & 0x00F0) {
                case 0x10 -> this.type_and_value.type_id = val;
                case FSM_PROTOCOL_TYPE_INT_4BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_4BIT;
                    this.type_and_value.number = (val & 0x0F);
                }
                case FSM_PROTOCOL_TYPE_INT_12BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_12BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(1);
                }
                case FSM_PROTOCOL_TYPE_INT_20BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_20BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(2);
                }
                case FSM_PROTOCOL_TYPE_INT_28BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_28BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(3);
                }
                case FSM_PROTOCOL_TYPE_INT_36BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_36BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(4);
                }
                case FSM_PROTOCOL_TYPE_INT_44BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_44BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(5);
                }
                case FSM_PROTOCOL_TYPE_INT_52BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_52BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(6);
                }
                case FSM_PROTOCOL_TYPE_INT_60BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_60BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(7);
                }
                case FSM_PROTOCOL_TYPE_INT_68BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_INT_68BIT;
                    this.type_and_value.number = (val & 0x0F);
                    this.read_additional_number_bytes(8);
                }
                case FSM_PROTOCOL_TYPE_STRING_LENGTH_4BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_STRING_LENGTH_4BIT;
                    this.type_and_value.number = 0;
                    int us = (val & 0x0F);
                    int r = this.reader.read(this.buffer, 0, us);
                    this.type_and_value.string = new String(this.buffer, 0, us) + this.type_and_value.string;
                }
                case FSM_PROTOCOL_TYPE_STRING_LENGTH_12BIT -> {
                    this.type_and_value.type_id = FSM_PROTOCOL_TYPE_STRING_LENGTH_12BIT;
                    this.type_and_value.number = 0;
                    int us = (val & 0x0F);
                    int value = this.reader.read();
                    us = (us << 8) | (value & 0x00FF);
                    int read = this.reader.read(this.buffer, 0, us);
                    this.type_and_value.string = new String(this.buffer, 0, us) + this.type_and_value.string;
                }
                default -> {
                }
            }
        }
    }


    public void verify_version() throws IOException {
        String vs = this.read_string();
        if (!FSM_PROTOCOL_TYPE_PROTOCOL_VERSION.equals(vs)) {
            this.error("Wrong protocol version '%s'", vs);
        }
    }

    public void close() {
    }

    public boolean read_boolean() throws IOException {
        if (this.ok) {
            int type_id = this.reader.read();
            return switch (type_id) {
                case FSM_PROTOCOL_TYPE_BOOLEAN_TRUE -> true;
                case FSM_PROTOCOL_TYPE_BOOLEAN_FALSE -> false;
                default -> {
                    error("Expected bool, got %d", type_id);
                    yield false;
                }
            };
        } else {
            return false;
        }
    }

    public String read_option_string() throws IOException {
        if (this.ok) {
            this.read_type_and_size();
            return switch (this.type_and_value.type_id) {
                case FSM_PROTOCOL_TYPE_OPT_STRING_NONE -> null;
                case FSM_PROTOCOL_TYPE_STRING_LENGTH_12BIT, FSM_PROTOCOL_TYPE_STRING_LENGTH_4BIT ->
                        this.type_and_value.string;
                default -> {
                    Log.error("Expected string, got %s",
                            this.type_and_value.type_id);
                    yield null;
                }
            };
        }
        return null;
    }

    @Override
    public Data read_data() throws IOException {
        int what = (int) this.read_long();
        return this.read_data_value_payload(what);
    }

    @Override
    public String read_string() throws IOException {
        this.read_type_and_size();
        if (StaticOptions.debug)
            Log.debug("Read String %s", this.type_and_value.string);
        if (this.verify_string_type()) {
            return this.type_and_value.string;
        } else {
            return "";
        }
    }


    /// Reads a integer value
    public long read_long() throws IOException {
        this.read_type_and_size();
        if (this.verify_number_type()) {
            return this.type_and_value.number;
        } else {
            return 0;
        }
    }

    @Override
    public boolean has_error() {
        return !this.ok;
    }

}
