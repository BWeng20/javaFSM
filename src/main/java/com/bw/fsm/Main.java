package com.bw.fsm;

import com.bw.fsm.tracer.thrift.ThriftTracerFactory;

import java.io.IOException;
import java.io.PrintStream;

public class Main {

    public static void usage(PrintStream os) {
        os.println(
                """
                        
                        FSM Main - loads and executes a FSM.
                        
                        Usage:
                          java com.bw.fsm.Main <fsm file>... [option...]
                          Options:
                        
                            -help                 Prints this message and exists.
                        
                            -thriftServerAddress  URL of thrift trace server. Enables Thrift bases Tracing.
                        
                            -thriftClientAddress  URL of local thrift client. Default is "tcp:localhost:4212".
                        """);

    }

    public static void main(String[] args) throws IOException {

        Arguments arguments;
        try {
            arguments = new Arguments(args, new Arguments.Option[]{
                    new Arguments.Option("help"),
                    Arguments.TRACE_ARGUMENT_OPTION,
                    Arguments.INCLUDE_PATH_ARGUMENT_OPTION,
                    new Arguments.Option("thriftServerAddress").withValue(),
                    new Arguments.Option("thriftClientAddress").withValue()});

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

        IncludePaths include_paths = new IncludePaths();
        include_paths.add(arguments.getIncludePaths());

        ThriftTracerFactory.serverAddress = arguments.options.get("thriftServerAddress");
        String thriftClientAddress = arguments.options.get("thriftClientAddress");
        if (thriftClientAddress != null)
            ThriftTracerFactory.clientAddress = thriftClientAddress;

        if (!arguments.final_args.isEmpty()) {
            FsmExecutor executor = new FsmExecutor(true);
            executor.set_include_paths(include_paths);

            for (String fsm : arguments.final_args) {
                executor.execute(fsm, null, arguments.getTraceMode());
            }
        } else {
            usage(System.out);
        }
    }
}