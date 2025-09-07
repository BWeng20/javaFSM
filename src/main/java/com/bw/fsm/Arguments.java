package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;

import java.nio.file.Path;
import java.util.*;

public class Arguments {

    public final static class Option {
        public String name;
        public boolean with_value = false;
        public boolean required = false;

        public Option(String name) {
            this.name = name;
        }

        public Option asRequired() {
            this.required = true;
            return this;
        }

        public Option withValue() {
            this.with_value = true;
            return this;
        }
    }

    public final java.util.Map<String, String> options;
    public final java.util.List<String> final_args;

    /**
     * Parse program arguments.
     */
    public Arguments(String[] appArgs, Option[] arguments) throws IllegalArgumentException {

        final_args = new ArrayList<>();
        var args = Arrays.asList(appArgs);
        int idx = 0;
        Map<String, String> map = new HashMap<>();

        // Don't use clap to parse arguments for now to reduce dependencies.
        while (idx < args.size()) {
            String arg = args.get(idx);
            idx += 1;

            if (arg.startsWith("-")) {
                var sarg = arg.substring(1);
                var match_found = false;
                for (var opt : arguments) {
                    match_found = opt.name.equals(sarg);
                    if (match_found) {
                        if (opt.with_value) {
                            if (idx >= args.size()) {
                                Log.panic("Missing value for argument '%s'", opt.name);
                            }
                            map.put(opt.name, args.get(idx));
                            idx += 1;
                        } else {
                            map.put(opt.name, "");
                        }
                        break;
                    }
                }
                if (!match_found) {
                    throw new IllegalArgumentException(String.format("Unknown option '%s'", arg));
                }
            } else {
                final_args.add(arg);
            }
        }
        this.options = map;
    }

    public static final Option TRACE_ARGUMENT_OPTION = new Option("trace");

    public TraceMode getTraceMode() {
        return TraceMode.fromString(options.get(TRACE_ARGUMENT_OPTION.name));
    }

    public static final Option INCLUDE_PATH_ARGUMENT_OPTION = new Option("includePaths").withValue();


    public java.util.List<Path> getIncludePaths() {
        java.util.List<Path> p;
        String includePathsOption = options.get(INCLUDE_PATH_ARGUMENT_OPTION.name);
        if (includePathsOption != null) {
            p = IOTool.splitPaths(includePathsOption);
        } else {
            p = Collections.emptyList();
        }
        return p;
    }


}
