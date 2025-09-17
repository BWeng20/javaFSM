package com.bw.fsm;

import com.bw.fsm.actions.ActionWrapper;
import com.bw.fsm.datamodel.ecma.ECMAScriptDatamodel;
import com.bw.fsm.datamodel.expression_engine.RFsmExpressionDatamodel;
import com.bw.fsm.datamodel.null_datamodel.NullDatamodel;
import com.bw.fsm.tracer.Tracer;
import com.bw.fsm.tracer.thrift.ThriftTracerFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;

public class Main {

    public static void usage(PrintStream os) {
        os.println(
                """                    
                 FSM Main - loads and executes a FSM.
                
                 Usage:
                  java com.bw.fsm.Main <run-configuration.json> [option...]
                  Options:
                    -help                 Prints this message and exists.
                
                 """);

    }

    public static void main(String[] args) throws IOException {

        Arguments arguments;
        try {
            arguments = new Arguments(args, new Arguments.Option[]{
                    new Arguments.Option("help")});
        } catch (IllegalArgumentException ia) {
            System.err.println(ia.getMessage());
            usage(System.err);
            System.exit(1);
            // Will never been reached:
            return;
        }

        if (arguments.options.containsKey("help")) {
            usage(System.out);
            System.exit(0);
        }

        if (!arguments.final_args.isEmpty()) {

            if (StaticOptions.ecma) {
                ECMAScriptDatamodel.register();
            }
            NullDatamodel.register();
            RFsmExpressionDatamodel.register();

            RunConfiguration config = null;

            String arg = arguments.final_args.get(0);

            final String ext;
            int extIdx = arg.lastIndexOf('.');
            if (extIdx >= 0) {
                ext = arg.substring(extIdx + 1);
            } else {
                ext = "";
            }
            Path runConfigPath = Paths.get(arg);
            Path runConfigFolder = runConfigPath.getParent().toAbsolutePath();
            config = load_run_config(runConfigPath);

            IncludePaths ic = new IncludePaths();
            for (String p : config.includes) {
                Path icp = Paths.get(p);
                if (!icp.isAbsolute()) {
                    icp = runConfigFolder.resolve(icp);
                }
                ic.add(icp);
            }

            if ( config.thrift.serverAddress != null) {
                ThriftTracerFactory.serverAddress = config.thrift.serverAddress;
                if ( config.thrift.clientAddress != null)
                    ThriftTracerFactory.clientAddress = config.thrift.clientAddress;
                Tracer.set_tracer_factory(new ThriftTracerFactory());
            }

            FsmExecutor executor = new FsmExecutor(true);
            executor.set_include_paths(ic);

            for (RunConfiguration.Machine m : config.machines) {
                Path p = Paths.get(m.path);
                if (!p.isAbsolute()) {
                    p = runConfigFolder.resolve(p);
                }
                Fsm fsm = load_fsm(p, ic);

                final ScxmlSession session = fsm.start_fsm_with_data(
                        new ActionWrapper(),
                        executor, Collections.emptyList(), m.options.trace.mode
                );

                Timer watchdog = null;
                if (m.timeoutMS > 0) {
                    watchdog = new Timer(m.timeoutMS, e -> {
                        if (session.thread.isAlive()) {
                            session.sender.enqueue(Event.new_simple(Fsm.EVENT_CANCEL_SESSION));
                        }
                    });
                    watchdog.setRepeats(false);
                    watchdog.start();
                }
            }

            for (String fsm : arguments.final_args) {
                executor.execute(fsm, null, arguments.getTraceMode());
            }
        } else {
            usage(System.out);
        }
    }


    public static Fsm load_fsm(Path file_path, IncludePaths include_paths) throws IOException {
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


    public static RunConfiguration load_run_config(Path path) throws IOException {
        RunConfiguration config = null;
        String ext = IOTool.getFileExtension(path);
        switch (ext.toLowerCase(Locale.CANADA)) {
            case "yaml", "yml" -> config = RunConfiguration.fromYaml(path);
            case "json", "js" -> config = RunConfiguration.fromJson(path);
        }
        return config;
    }

}