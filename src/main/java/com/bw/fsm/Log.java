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

    private static void logInternal(PrintStream ps, String context, String message, Object[] arguments) {
        String output = String.format("[%5s] %s\n", context, String.format(message, arguments));
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
        logInternal(System.err, "error", message, arguments);
    }
}
