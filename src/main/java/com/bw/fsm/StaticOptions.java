package com.bw.fsm;

public interface StaticOptions {

    boolean debug_option = true;

    static void debug(String msg, Object... arguments) {
        if (debug_option) {
            System.out.printf(msg, arguments);
            System.out.println();
        }
    }

    static void panic(String message, Object... arguments) {
        throw new InternalError(String.format(message, arguments));
    }


    static void info(String message, Object... arguments) {
        System.out.printf(message, arguments);
        System.out.println();
    }

    static void warn(String message, Object... arguments) {
        System.out.printf(message, arguments);
        System.out.println();
    }

    static void error(String message, Object... arguments) {
        System.err.printf(message, arguments);
        System.err.println();
    }
}
