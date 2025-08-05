package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.ecma.ECMAScriptDatamodel;
import com.bw.fsm.datamodel.null_datamodel.NullDatamodel;
import com.bw.fsm.tracer.TraceMode;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

        if (StaticOptions.ecma_script_model)
            ECMAScriptDatamodel.register();
        NullDatamodel.register();

        Arguments arguments = new Arguments(args, new Arguments.Option[]{
                new Arguments.Option(TRACE_ARGUMENT_OPTION).withValue(),
                new Arguments.Option(INCLUDE_PATH_ARGUMENT_OPTION).withValue()});

        Tester tester = new Tester(arguments);
    }

    public Fsm load_fsm(Path file_path, java.util.List<Path> include_paths) throws IOException {
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


    TestSpecification config;
    String test_spec_file;

    public Tester(Arguments arguments) {
        try {
            java.util.List<Path> include_paths = IOTool.splitPaths(arguments.options.get(INCLUDE_PATH_ARGUMENT_OPTION));
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
                    if (fsm != null) {
                        abort_test(String.format("Test Specification '%s' contains a fsm path, but program arguments define some other fsm",
                                test_spec_file));
                    }
                    uc.fsm = load_fsm(Paths.get(test_spec_file), include_paths);
                    // uc.fsm.tracer.enable_trace(trace);
                    if (StaticOptions.debug_option)
                        Log.debug("Loaded %s", test_spec_file);
                } else {
                    uc.fsm = fsm;
                }
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

    public void run_test(TestUseCase test) {
        if (test.fsm == null) {
            abort_test(String.format("No FSM given in test '%s'", test.name));
        }

        Fsm fsm = test.fsm;

        int timeout = test.specification.timeout_milliseconds == null ? 0 : test.specification.timeout_milliseconds;
        var final_expected_configuration = test.specification.final_configuration;

        Map<String, String> options = test.specification.options == null ? new HashMap<>() : test.specification.options;

        if (!run_test_manual(test.name, options, fsm, test.include_paths, test.trace_mode, timeout, final_expected_configuration)) {
            System.exit(-1);
        } else {
            System.exit(0);
        }

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

        Log.info("FSM started. Waiting to terminate...");
        if (session.thread == null) {
            Log.panic("Internal error: session_thread not available");
        }
        try {
            session.thread.join();
        } catch (Exception e) {

        }

        if (watchdog_sender != null) {
            // Inform watchdog
            disable_watchdog(watchdog_sender);
        }

        if (expected_final_configuration == null) {
            return true;
        } else {
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
                                "[%s] ==> Expected final state '%s' not reached. Final configuration: %s",
                                test_name,
                                session.global_data.final_configuration,
                                String.join(",", session.global_data.final_configuration));
                        return false;
                    }
                }
            }
        }
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

    protected void abort_test(String message) {
        Log.error("Test Failed: %s", message);
        Log.setLogFile(null);
        System.exit(1);
    }


}
