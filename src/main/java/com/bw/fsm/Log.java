package com.bw.fsm;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Log {

    private static Writer writer;

    public static void setLogFile(Path logFile) {
        try {
            System.out.flush();
            System.err.flush();
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
            if (logFile != null)
                writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static long start_time = System.currentTimeMillis();

    private static void logInternal(PrintStream ps, String context, String message, Object[] arguments) {
        try {
            String msg = String.format(message, arguments);
            String output = String.format("[%5s] [%6s] [+%7d] %s\n", context,
                    Thread.currentThread().getName(),
                    (System.currentTimeMillis() - start_time), msg);
            ps.print(output);
            if (writer != null) {
                try {
                    writer.write(output);
                } catch (Exception e) {
                    System.err.printf("Failed to write to log: %s\n", e.getMessage());
                    try {
                        writer.close();
                    } catch (Exception ignored) {
                    }
                    writer = null;
                }
            }
        } catch (Exception e) {
            System.err.println("Internal Error in logInternal: " + e.getMessage());
        }
    }

    public static void debug(String msg, Object... arguments) {
        if (StaticOptions.debug_option) {
            logInternal(System.out, "debug", msg, arguments);
        }
    }

    public static void panic(String message, Object... arguments) {
        logInternal(System.err, "panic", message, arguments);
        throw new InternalError(String.format(message, arguments));
    }

    public static void info(String message, Object... arguments) {
        logInternal(System.out, "info", message, arguments);
    }

    public static void warn(String message, Object... arguments) {
        logInternal(System.out, "warn", message, arguments);
    }

    public static void error(String message, Object... arguments) {
        System.out.flush();
        logInternal(System.err, "error", message, arguments);
        System.err.flush();
    }

    public static void exception(String message, Throwable t) {
        System.out.flush();
        logInternal(System.err, "error", message, null);
        t.printStackTrace(System.err);
        System.err.flush();
    }
}
