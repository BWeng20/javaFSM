package com.bw.fsm;

import com.bw.fsm.tracer.TraceMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Stream;

/**
 * Downloads and executes W3E Tests
 */
public class W3CTest {

    static int succeededCount = 0;
    static int failedCount = 0;

    public static void main(String[] args) {

        Arguments arguments = new Arguments(args, new Arguments.Option[]{
                new Arguments.Option("dry"),
                new Arguments.Option("logOnlyFailure"),
                new Arguments.Option("parallel")});

        if (arguments.final_args.size() < 2) {
            Log.error("Wrong number of arguments.");
            Log.error(
                    """
                            Usage:
                              java com.bw.fsm.W3CTest [-logOnlyFailure][-dry] <testDirectory> <ReportFile>
                              Options:
                                logOnlyFailure Create logs only if a test fails.
                                dry            Tests existing scxml files without writing any files.
                                               No download, no transformation, no logs, no report file.
                                parallel       Use a parallel stream to process the tests.
                                testDirectory  Folder to put all tests files.
                                               Must contain at least the test-config "test_config.json".
                                               Created sub-folders:
                                                 logs:  main and test logs. One for each test file
                                                 txml:  downloaded manifest, transformation and test files
                                                 scxml: transformed scxml test files
                                                 dependencies: downloaded includes
                                                 dependencies/scxml: transformed includes
                                                 manual_txml: downloaded manual tests
                                                 manual_scxml: transformed manual tests (not used)
                                                 optional_txml: downloaded optional tests
                                                 optional_scxml: transformed optional tests (not used)
                                ReportFile     Report file to create (not yet).
                            """);
            System.exit(2);
        }

        final boolean dry = arguments.options.containsKey("dry");
        final boolean logOnlyFailure = arguments.options.containsKey("logOnlyFailure");
        final boolean parallel = arguments.options.containsKey("parallel");

        Path testDirectory = Paths.get(arguments.final_args.get(0));
        Path reportFile = Paths.get(arguments.final_args.get(1));

        final Path logDirectory = testDirectory.resolve("logs");
        if (!dry) {
            try {
                Files.createDirectories(logDirectory);
                String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
                Log.setLogFile(logDirectory.resolve("LOG_" + date + ".txt"), true);
            } catch (IOException io) {
                io.printStackTrace();
                Log.error("Failed to initialize logging. %s", io.getMessage());
            }
        }

        Log.info("=== Download and transform tests to %s", testDirectory);
        TestDownloader downloader = new TestDownloader(testDirectory);
        downloader.dry = dry;
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
        includePaths.add(downloader.dependenciesScxml);

        final PrintStream log = Log.getPrintStream(true);

        try {
            TestSpecification config = Tester.load_test_config(testDirectory.resolve("test_config.json"));

            Tester tester = new Tester(config);

            Stream<Path> s = parallel ? scxmlFiles.parallelStream() : scxmlFiles.stream();


            s.forEach(scxmlFile -> {
                ByteArrayOutputStream os = null;
                boolean succeeded = false;
                Path logFile = logDirectory.resolve(scxmlFile.getFileName() + ".log");
                try {

                    if (logOnlyFailure) {
                        os = new ByteArrayOutputStream(10240);
                        Log.setLogStream(new PrintStream(os, false, StandardCharsets.UTF_8), true);
                    } else if (!dry)
                        Log.setLogFile(logFile, false);
                    if (tester.runTest(Tester.load_fsm(scxmlFile, includePaths), includePaths, TraceMode.ALL)) {
                        log.println("===== Test " + scxmlFile + " succeeded");
                        succeeded = true;
                        ++succeededCount;
                    } else {
                        log.println("===== Test " + scxmlFile + " failed");
                        ++failedCount;
                    }
                } catch (Exception e) {
                    log.println("===== Test " + scxmlFile + " failed due to exception: " + e);
                    Log.exception("===== Test " + scxmlFile + " failed due to exception.", e);
                    ++failedCount;
                } finally {
                    Log.setLogFile(null, false);
                    if (os != null && !succeeded) {
                        if (dry)
                            log.print(os.toString(StandardCharsets.UTF_8));
                        else {
                            try {
                                Files.write(logFile, os.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                            } catch (IOException e) {
                                log.println("Failed to write log '" + logFile + "': " + e.getMessage());
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            Log.exception("Failed in Test loop.", e);
            ++failedCount;
        }
        log.flush();
        log.println("==== Results:");
        log.println(" Tests     " + scxmlFiles.size());
        log.println(" Succeeded " + succeededCount);
        log.println(" Failed    " + failedCount);

        log.close();
        System.exit(failedCount > 0 ? 1 : 0);

    }
}
