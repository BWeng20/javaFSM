package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.ecma.ECMAScriptDatamodel;
import com.bw.fsm.datamodel.null_datamodel.NullDatamodel;
import com.bw.fsm.tracer.TraceMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loads a test specification and executes the given FSM.<br>
 */
public class Tester {


    public static final String TRACE_ARGUMENT_OPTION = "trace";
    public static final String INCLUDE_PATH_ARGUMENT_OPTION = "includePaths";

    private static boolean modelsInitialized = false;

    /**
     * Main for manual tests.
     */
    public static void main(String[] args) {

        try {
            Arguments arguments = new Arguments(args, new Arguments.Option[]{
                    new Arguments.Option(TRACE_ARGUMENT_OPTION).withValue(),
                    new Arguments.Option(INCLUDE_PATH_ARGUMENT_OPTION).withValue()});

            java.util.List<Path> include_paths = IOTool.splitPaths(arguments.options.get(INCLUDE_PATH_ARGUMENT_OPTION));
            List<Fsm> fsms = new ArrayList<>();
            TestSpecification config = null;

            for (String arg : arguments.final_args) {
                final String ext;
                int extIdx = arg.lastIndexOf('.');
                if (extIdx >= 0) {
                    ext = arg.substring(extIdx + 1);
                } else {
                    ext = "";
                }
                switch (ext.toLowerCase(Locale.CANADA)) {
                    case "yaml", "yml", "json", "js" -> {
                        if (config != null)
                            abort_test("more then one config given.");
                        config = load_test_config(Paths.get(arg));
                        if (config.file != null)
                            fsms.add(load_fsm(Paths.get(config.file), include_paths));
                    }
                    case "rfsm", "scxml", "xml" -> fsms.add(load_fsm(Paths.get(arg), include_paths));
                    default -> abort_test(String.format("File '%s' has unsupported extension.", arg));
                }
            }
            if ( config == null)
                abort_test("no config given.");
            Tester tester = new Tester(config);
            if ( fsms.isEmpty())
                abort_test("no FSMs given.");
            for (Fsm fsm : fsms)
                tester.runTest(fsm, include_paths, arguments.getTraceMode());

            System.exit(0);

        }  catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static TestSpecification load_test_config( Path path) throws IOException {
        TestSpecification config = null;
        String ext = IOTool.getFileExtension(path);
        switch (ext.toLowerCase(Locale.CANADA)) {
            case "yaml", "yml" -> config = TestSpecification.fromYaml(path);
            case "json", "js" -> config = TestSpecification.fromJson(path);
        }
        return config;

    }

    public static Fsm load_fsm(Path file_path, java.util.List<Path> include_paths) throws IOException {
        String extension = IOTool.getFileExtension(file_path);
        ScxmlReader reader = new ScxmlReader().withIncludePaths(include_paths);
        switch (extension.toLowerCase(Locale.CANADA)) {
            // case "rfsm" -> { }
            case "scxml", "xml" -> {
                return reader.parse_from_xml_file(file_path);
            }

            default -> throw new IllegalArgumentException(String.format("No reader to load '%s'", file_path));
        }
    }

    private TestSpecification config;

    public Tester(TestSpecification config)
    {
        this.config = config;
    }

    public boolean runTest( Fsm fsm, List<Path> include_paths, TraceMode traceMode) {
        if (config != null) {
            if ( !modelsInitialized) {
                modelsInitialized = true;
                if (StaticOptions.ecma_script_model)
                    ECMAScriptDatamodel.register();
                NullDatamodel.register();
            }
            TestUseCase uc = new TestUseCase();
            uc.name = fsm.name;
            uc.fsm = fsm;
            uc.specification = config;
            uc.trace_mode = traceMode;
            uc.include_paths.addAll(include_paths);
            return runTestCase(uc);
        } else {
            abort_test("No test specification given.");
            return false;
        }
    }

    public boolean runTestCase(TestUseCase test) {
        if (test.fsm == null) {
            abort_test(String.format("No FSM given in test '%s'", test.name));
        }

        Fsm fsm = test.fsm;

        int timeout = test.specification.timeout_milliseconds == null ? 0 : test.specification.timeout_milliseconds;
        var final_expected_configuration = new ArrayList<>(test.specification.final_configuration);
        Map<String, String> options = test.specification.options == null ? new HashMap<>() : test.specification.options;

        return run_test_manual(test.name, options, fsm, test.include_paths, test.trace_mode, timeout, final_expected_configuration);
    }

    public boolean run_test_manual(
            String test_name, Map<String, String> options, Fsm fsm, java.util.List<Path> include_paths, TraceMode trace_mode,
            int timeout, java.util.List<String> expected_final_configuration) {
        return run_test_manual_with_send(
                test_name,
                options,
                fsm,
                include_paths, trace_mode,
                timeout,
                expected_final_configuration,
                (queue) -> {
                });
    }

    public boolean run_test_manual_with_send(
            String test_name,
            Map<String, String> options,
            Fsm fsm, java.util.List<Path> include_paths,
            TraceMode trace_mode,
            int timeout, java.util.List<String> expected_final_configuration,
            Consumer<BlockingQueue<Event>> eventCb) {

        try {
            fsm.tracer.enable_trace(trace_mode);

            var executor = new FsmExecutor(false);
            executor.set_global_options_from_arguments(options);
            executor.set_include_paths(include_paths);

            ScxmlSession session = fsm.start_fsm_with_data_and_finish_mode(
                    new ActionWrapper(),
                    executor, Collections.emptyList(),
                    FinishMode.KEEP_CONFIGURATION
            );

            BlockingQueue<Event> watchdog_sender = null;
            if (timeout > 0) {
                watchdog_sender = start_watchdog(test_name, timeout);
            }

            // Sending some event
            eventCb.accept(session.sender);

            if (session.thread == null) {
                Log.panic("Internal error: session_thread not available");
            }
            try {
                Log.info("FSM started. Waiting to terminate...");
                session.thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.info("FSM '%s' terminated.", fsm.name );

            if (watchdog_sender != null) {
                // Inform watchdog
                disable_watchdog(watchdog_sender);
            }

            if (expected_final_configuration != null) {
                ScxmlSession s = executor.state.sessions.get(session.session_id);
                if (s == null) {
                    Log.error("FSM Session lost");
                    return false;
                } else {
                    if (session.global_data.final_configuration == null) {
                        Log.error("Final Configuration not available");
                        return false;
                    } else {
                        if (verify_final_configuration(expected_final_configuration, session.global_data.final_configuration)) {
                            Log.info(
                                    "[%s] ==> Final configuration '%s' reached",
                                    test_name, String.join(",", session.global_data.final_configuration));
                            return true;
                        } else {
                            Log.error(
                                    "[%s] ==> Expected final configuration %s not reached. Final configuration: %s",
                                    test_name,
                                    expected_final_configuration,
                                    String.join(",", session.global_data.final_configuration));
                            return false;
                        }
                    }
                }
            }
        } finally {
            System.err.flush();
            System.out.flush();
        }
        return true;
    }

    static Timer watchdog = new Timer(true);

    public BlockingQueue<Event> start_watchdog(String test_name, int timeout) {
        BlockingQueue<Event> watchdog_queue = new BlockingQueue<>();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!watchdog_queue.data.isEmpty()) {
                    // All ok, FSM terminated in time.
                } else {
                    abort_test(String.format("[%s] ==> FSM timed out after %d milliseconds", test_name, timeout));
                }

            }
        };
        watchdog.schedule(timerTask, timeout);
        return watchdog_queue;
    }

    /**
     * Informs the watchdog that the test has finished.
     * + watchdog_sender - the sender-channel to the watchdog.
     */
    public void disable_watchdog(BlockingQueue<Event> watchdog_queue) {
        watchdog_queue.enqueue(Event.new_simple("finished"));
    }

    /**
     * Verifies that the configuration contains a number of expected states
     * <p>
     * + expected_states - List of expected states, the FSM configuration must contain all of them.
     * + fsm_config - The final FSM configuration to verify. May contain more than the required states.
     */
    public boolean verify_final_configuration(java.util.List<String> expected_states,
                                              java.util.List<String> fsm_config) {
        for (String fc_name : expected_states) {
            if (!fsm_config.contains(fc_name)) {
                return false;
            }
        }
        return true;
    }

    protected static void abort_test(String message) {
        Log.error("Test Failed: %s", message);
        Log.setLogFile(null, false);
        throw new RuntimeException("Test Aborted");
    }


}
