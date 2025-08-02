package com.bw.fsm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class W3CTest {

    public static void main(String[] args) {
        if ( args.length != 2) {
            Log.error("Wrong number of arguments.");
            Log.error(
                    """
                    Usage:
                      java com.bw.fsm.W3CTest <testDirectory> <ReportFile>
                    """);
            System.exit(2);
        }

        Path testDirectory = Paths.get(args[0]);
        Path reportFile = Paths.get(args[1]);

        Path logDirectory = testDirectory.resolve("logs");
        try {
            Files.createDirectories(logDirectory);
            String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()) ;
            Log.setLogFile( logDirectory.resolve("LOG_"+date+".txt") );
        } catch ( IOException io) {
            io.printStackTrace();
            Log.error("Failed to initialize logging. %s", io.getMessage() );
        }

        Log.info("=== Download and transform tests to %s", testDirectory);
        TestDownloader.downloadAndTransform(testDirectory);

        Log.info("=== Running Tests. Report file: %s", reportFile);
        System.out.println("TODO");

        Log.setLogFile(null);

    }
}
