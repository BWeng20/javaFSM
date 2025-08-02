package com.bw.fsm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * POJO to represent a test-specification that can be loaded from json with schema/test_specification_schema.json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestSpecification {

    @JsonProperty("file")
    public String file;

    @JsonProperty("events")
    public List<Event> events;

    @JsonProperty("final_configuration")
    public List<String> finalConfiguration;

    @JsonProperty("timeout_milliseconds")
    public Integer timeoutMilliseconds;

    @JsonProperty("options")
    public Map<String, Object> options;

    /**
     * Loads a JSON test specification
     */
    public static TestSpecification fromJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(), TestSpecification.class);
    }

    public static TestSpecification fromYaml(Path path) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(path.toFile(), TestSpecification.class);
    }

    // Event properties
    public static class Event {
        @JsonProperty("name")
        public String name;

        @JsonProperty("delay_ms")
        public int delayMs;

        @JsonProperty("shall_reach_state")
        public String shallReachState;

        @JsonProperty("shall_send_event")
        public String shallSendEvent;
    }
}