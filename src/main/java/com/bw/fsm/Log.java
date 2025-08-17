package com.bw.fsm;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Thread aware logger.
 */
public class Log {

    private static final ThreadLocal<PrintStream> thread_writer = new ThreadLocal<>();

    /**
     * PrintStream with counter for automatic close if no longer used by any thread.
     */
    public final static class LogPrintStream extends PrintStream {

        private int count = 0;

        public final Path file;

        public LogPrintStream(@NotNull Path logFile, boolean append) throws IOException {
            super(
                    new BufferedOutputStream(Files.newOutputStream(logFile,
                            append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.CREATE)));
            file = logFile;
        }

        public synchronized void lock() {
            ++count;
        }

        public synchronized void unlock() {
            --count;
            if (count <= 0) {
                close();
            }

        }

    }

    /**
     * Sets the log file for the current thread.
     *
     * @param logFile The log file or null to reset to System.out.
     * @param append  If true, append to the file otherwise truncate.
     */
    public static LogPrintStream setLogFile(Path logFile, boolean append) {
        try {
            releaseStream();
            if (logFile != null) {
                LogPrintStream ls = new LogPrintStream(logFile, append);
                ls.lock();
                thread_writer.set(ls);
                return ls;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public static void setLogStream(PrintStream logStream) {
        if (logStream instanceof LogPrintStream lp)
            lp.lock();
        releaseStream();
        thread_writer.set(logStream);
    }

    /**
     * Current time millis of system start.
     */
    private static long start_time = System.currentTimeMillis();

    /**
     * If true, all output is additionally printed to System.out and System.err .
     */
    private static boolean outputToConsole = false;

    /**
     * Logs the message via the current log stream and (if {@link #outputToConsole} is true) to argument "ps".
     *
     * @param ps        The System output to use if outputToConsole is true or the current log stream is not set.
     * @param context   The context (info,error etc.)
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
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
                    releaseStream();
                }
            }
        } catch (Exception e) {
            System.err.println("Internal Error in logInternal: " + e.getMessage());
        }
    }

    public static void releaseStream() {
        PrintStream writer = thread_writer.get();
        if (writer != null) {
            thread_writer.set(null);
            writer.flush();
            if (writer instanceof LogPrintStream lp) {
                lp.unlock();
            }
        } else {
            System.out.flush();
            System.err.flush();
        }
    }

    /**
     * Prints if {@link StaticOptions#debug_option} is true.
     *
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
    public static void debug(String message, Object... arguments) {
        if (StaticOptions.debug_option) {
            logInternal(System.out, "debug", message, arguments);
        }
    }

    /**
     * Prints the message via context "panic" and throws an {@link InternalError}.
     *
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
    public static void panic(String message, Object... arguments) {
        logInternal(System.err, "panic", message, arguments);
        throw new InternalError(String.format(message, arguments));
    }

    /**
     * Prints the message via context "info".
     *
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
    public static void info(String message, Object... arguments) {
        logInternal(System.out, "info", message, arguments);
    }

    /**
     * Prints the message via context "warn".
     *
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
    public static void warn(String message, Object... arguments) {
        logInternal(System.out, "warn", message, arguments);
    }

    /**
     * Prints the message via context "error".
     *
     * @param message   The message-format-string to print
     * @param arguments Format arguments.
     */
    public static void error(String message, Object... arguments) {
        if (thread_writer.get() == null) {
            System.out.flush();
            logInternal(System.err, "error", message, arguments);
            System.err.flush();
        } else {
            logInternal(System.err, "error", message, arguments);
        }
    }

    /**
     * Prints the message via context "error" and prints the stacktrace of the exception.
     *
     * @param message The message to print
     * @param t       The throwable.
     */
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

    /**
     * Gets the current log stream (System.out if none was set).
     */
    public static PrintStream getPrintStream() {
        PrintStream ps = thread_writer.get();
        return ps == null ? System.out : ps;
    }
}
