package com.bw.fsm;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Loads a test specification and executes the given FSM.<br>
 */
public class Tester {


    public static final String TRACE_ARGUMENT_OPTION = "trace";
    public static final String INCLUDE_PATH_ARGUMENT_OPTION = "includePaths";

    /**
     * Main for manual tests.
     */
    public static void main(String[] args) {
        Arguments arguments = new Arguments(args, new Arguments.Option[]{
                new Arguments.Option(TRACE_ARGUMENT_OPTION).withValue(),
                new Arguments.Option(INCLUDE_PATH_ARGUMENT_OPTION).withValue()});

        Tester tester = new Tester(arguments);
    }

    public Fsm load_fsm(Path file_path, java.util.List<Path> include_paths) throws XMLStreamException, IOException {
        String extension = Arguments.getFileExtension(file_path);
        ScxmlReader reader = new ScxmlReader().withIncludePaths(include_paths);
        switch (extension.toLowerCase(Locale.CANADA)) {
            // case "rfsm" -> { }
            case "scxml", "xml" -> {
                return reader.read(file_path);
            }

            default -> throw new IllegalArgumentException(String.format("No reader to load '%s'", file_path));
        }
    }


    TestSpecification config;
    String test_spec_file;

    public Tester(Arguments arguments) {
        try {
            java.util.List<Path> include_paths = new ArrayList<>();

            String incPath = arguments.options.get(INCLUDE_PATH_ARGUMENT_OPTION);
            if (incPath != null)
                Arrays.stream(incPath.split(File.pathSeparator)).map(Paths::get).forEach(include_paths::add);
            Fsm fsm = null;
            for (String arg : arguments.final_args) {
                final String ext;
                int extIdx = arg.lastIndexOf('.');
                if (extIdx >= 0) {
                    ext = arg.substring(extIdx + 1);
                } else {
                    ext = "";
                }
                switch (ext.toLowerCase(Locale.CANADA)) {
                    case "yaml", "yml" -> {
                        config = TestSpecification.fromYaml(Paths.get(arg));
                        test_spec_file = arg;
                    }
                    case "json", "js" -> {
                        config = TestSpecification.fromJson(Paths.get(arg));
                        test_spec_file = arg;
                    }
                    case "rfsm", "scxml", "xml" -> fsm = load_fsm(Paths.get(arg), include_paths);
                    default -> abort_test(String.format("File '%s' has unsupported extension.", arg));
                }
            }
            if (config != null) {
                TestUseCase uc = new TestUseCase();
                uc.name = test_spec_file;
                if (config.file != null) {
                    if (config.file != null) {
                        if (fsm != null) {
                            abort_test(String.format("Test Specification '%s' contains a fsm path, but program arguments define some other fsm",
                                    test_spec_file));
                        }
                        uc.fsm = load_fsm(Paths.get(test_spec_file), include_paths);
                        // uc.fsm.tracer.enable_trace(trace);
                        if (StaticOptions.debug_option)
                            Log.debug("Loaded %s", test_spec_file);
                    }
                } else {
                    uc.fsm = fsm;
                }
                ;
                uc.specification = config;
                uc.trace_mode = arguments.getTraceMode();
                uc.include_paths.addAll(include_paths);
                run_test(uc);
            } else {
                abort_test("No test specification given.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run_test(TestUseCase testCase) {
        // TODO
    }

    protected void abort_test(String message) {
        Log.error("Test Failed: %s", message);
        Log.setLogFile(null);
        System.exit(1);
    }

}
