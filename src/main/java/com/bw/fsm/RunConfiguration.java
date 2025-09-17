package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * POJO to represent a run-configuration that can be loaded from json with schema/run_configuration_schema.json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class RunConfiguration {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class TraceOptions {

        @JsonProperty("mode")
        public TraceMode mode = TraceMode.STATES;

    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Options {

        @JsonProperty("trace")
        public TraceOptions trace;
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Machine {

        @JsonProperty("path")
        public String path;

        @JsonProperty("timeout_milliseconds")
        public int timeoutMS = 0;

        @JsonProperty("options")
        public Options options;

    }

    @JsonProperty("machines")
    public List<Machine> machines;

    @JsonProperty("includes")
    public List<String> includes;

    @JsonProperty("global_options")
    public Map<String, String> globalOptions;

    public static class ThriftOptions {

        /**
         * If thriftServerAddress is set, a Thrift based Tracer implementation will be used.
         */
        @JsonProperty(value = "server_address")
        public String serverAddress;

        @JsonProperty("client_address")
        public String clientAddress;
    }

    @JsonProperty("thrift")
    public  ThriftOptions thrift = new ThriftOptions();

    @JsonProperty("options")
    public Map<String, String> options;

    public TraceMode getTraceMode() {
        String traceMode =  options != null ? options.get("trace:mode") : null;
        return TraceMode.fromString(traceMode);
    }

    /**
     * Loads a JSON test specification
     */
    public static RunConfiguration fromJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(), RunConfiguration.class);
    }

    public static RunConfiguration fromYaml(Path path) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(path.toFile(), RunConfiguration.class);
    }

}