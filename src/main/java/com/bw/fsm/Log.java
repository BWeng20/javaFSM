package com.bw.fsm;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Log {

    private static ThreadLocal<PrintStream> thread_writer = new ThreadLocal<>();
    private static ThreadLocal<Boolean> autoclose_writer = new ThreadLocal<>();

    public static void setLogFile(Path logFile, boolean append) {
        try {
            PrintStream writer = thread_writer.get();
            Boolean oldAutoClose = autoclose_writer.get();

            if (writer != null) {
                writer.flush();
                if (oldAutoClose)
                    writer.close();
                thread_writer.set(null);
            } else {
                System.out.flush();
                System.err.flush();
            }
            if (logFile != null) {
                thread_writer.set(new PrintStream(new BufferedOutputStream(Files.newOutputStream(logFile, append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)), false, StandardCharsets.UTF_8));
                autoclose_writer.set(true);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void setLogStream(PrintStream logStream, boolean autoClose) {
        PrintStream writer = thread_writer.get();
        Boolean oldAutoClose = autoclose_writer.get();
        if (writer != null) {
            writer.flush();
            if (oldAutoClose)
                writer.close();
        } else {
            System.out.flush();
            System.err.flush();
        }
        thread_writer.set(logStream);
        autoclose_writer.set(autoClose);
    }


    private static long start_time = System.currentTimeMillis();
    private static boolean outputToConsole = false;

    private static void logInternal(PrintStream ps, String context, String message, Object[] arguments) {
        try {
            String msg = String.format(message, arguments);
            String output = String.format("[%5s] [%6s] [+%7d] %s\n", context,
                    Thread.currentThread().getName(),
                    (System.currentTimeMillis() - start_time), msg);
            PrintStream writer = thread_writer.get();
            if (outputToConsole || writer == null)
                ps.print(output);
            if (writer != null) {
                try {
                    writer.print(output);
                } catch (Exception e) {
                    System.err.printf("Failed to write to log: %s\n", e.getMessage());
                    thread_writer.set(null);
                    if (autoclose_writer.get()) {
                        try {
                            writer.close();
                        } catch (Exception ignored) {
                        }
                    }
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
        if (thread_writer.get() == null) {
            System.out.flush();
            logInternal(System.err, "error", message, arguments);
            System.err.flush();
        } else {
            logInternal(System.err, "error", message, arguments);
        }
    }

    public static void exception(String message, Throwable t) {
        PrintStream writer = thread_writer.get();
        if (writer == null) {
            System.out.flush();
            logInternal(System.err, "error", message, null);
            t.printStackTrace(System.err);
            System.err.flush();
        } else {
            logInternal(System.err, "error", message, null);
            t.printStackTrace(writer);

        }
    }

    public static PrintStream getPrintStream() {
        PrintStream ps = thread_writer.get();
        return ps == null ? System.out : ps;
    }
}
