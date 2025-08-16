package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Downloads and executes W3E Tests
 */
public class W3CTest {

    public static void main(String[] args) {
        if (args.length != 2) {
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
            String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
            Log.setLogFile(logDirectory.resolve("LOG_" + date + ".txt"));
        } catch (IOException io) {
            io.printStackTrace();
            Log.error("Failed to initialize logging. %s", io.getMessage());
        }

        Log.info("=== Download and transform tests to %s", testDirectory);
        TestDownloader downloader = new TestDownloader(testDirectory);
        downloader.downloadAndTransform();

        Log.info("=== Running Tests. Report file: %s", reportFile);

        java.util.List<Path> scxmlFiles;
        try (var dirStream = Files.list(downloader.scxml)) {
            scxmlFiles = dirStream.filter(path -> "scxml".equalsIgnoreCase(IOTool.getFileExtension(path)))
                    .toList();
        } catch (IOException e) {
            Log.exception("Failed to list files", e);
            System.exit(1);
            scxmlFiles = null;
        }

        List<Path> includePaths = new ArrayList<>();
        includePaths.add( downloader.dependenciesScxml );

        try {
            TestSpecification config = Tester.load_test_config(testDirectory.resolve("test_config.json"));

            Tester tester = new Tester( config );


            for (Path p : scxmlFiles) {
                if ( tester.runTest( Tester.load_fsm( p, includePaths), includePaths, TraceMode.ALL) ) {
                    System.out.println("===== Test "+p+" succeeded");
                } else {
                    System.out.println("===== Test "+p+" failed");
                }
            }

            Log.setLogFile(null);
        } catch (IOException ioe) {
            Log.exception("Failed in Test loop.", ioe);
        }

    }
}
