package com.bw.fsm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Arguments {

    public final static class Option {
        public String name;
        public boolean with_value = false;
        public boolean required = false;

        public Option(String name)
        {
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

    /// Parse program arguments.
    public Arguments(String[] appArgs, Option[] arguments) {

        final_args = new ArrayList<>();
        var args = Arrays.asList(appArgs);
        int idx = 1;
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
                    Log.panic("Unknown option '%s'", arg);
                }
            } else {
                final_args.add(arg);
            }
        }
        this.options = map;
    }
    public static String getFileExtension(Path path) {
        return getFileExtension(path.getFileName().toString());
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 ) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
}
