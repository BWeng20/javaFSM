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
import java.util.stream.Stream;

/**
 * Downloads and executes W3E Tests
 */
public class W3CTest {

    static int succeededCount = 0;
    static int failedCount = 0;

    public static void usage(PrintStream os) {
        os.println(
                """
                        
                        W3C Test - download, transformation and execution of the mandatory automated SCXML tests from W3C.
                        See https://www.w3.org/Voice/2013/scxml-irp/
                        
                        Usage:
                          java com.bw.fsm.W3CTest <testDirectory> [-report <file>] [-logOnlyFailure] [-stopOnError] [-dry] [-parallel] [-help]
                          Options:
                        
                            -logOnlyFailure Create logs only if a test fails.
                        
                            -stopOnError    Stops tests on first error.
                                            Remind that the stop may be delayed if also -parallel is given.
                        
                            -dry            Tests the existing files in scxml without writing any files.
                                            No download, no transformation, no report file.
                                            All log output is written to console.
                                            Useful during development to speed up test cycles.
                        
                            -parallel       Use a parallel stream to process the tests.
                                            Without this option the tests waits for each FSM to terminate.
                                            The order of execution of different tests is not deterministic.
                                            Useful for pipelines.
                        
                            -optionals      Run also the tests in "optional_scxml".
                        
                            -help           Prints this message and exists.
                        
                            -report         Creates a report file (not yet).
                        
                            testDirectory   Folder to store all tests files.
                                            Must contain at least the test-config "test_config.json".
                                            Created sub-folders:
                                              logs:  The main- and test-logs. One test-log for each test file
                                              txml:  Contains the downloaded manifest, transformation and test files
                                              scxml: The transformed scxml test files
                                              dependencies: The downloaded include files
                                              dependencies/scxml: The transformed include files
                                              manual_txml: The downloaded manual tests
                                              manual_scxml: The transformed manual tests (not used)
                                              optional_txml: The downloaded optional tests
                                              optional_scxml: The transformed optional scxml test files (not used)
                        
                        Examples:
                        
                        In a CI/CD pipeline you may want:
                           java com.bw.fsm.W3CTest w3cTests w3cTestReport.md -parallel -optionals
                        
                        During local development to test your code you may want (after an initial full run):
                           java com.bw.fsm.W3CTest w3cTests -dry -logOnlyFailure -stopOnError
                        """);

    }

    public static void main(String[] args) {

        Arguments arguments;
        try {
            arguments = new Arguments(args, new Arguments.Option[]{
                    new Arguments.Option("help"),
                    new Arguments.Option("dry"),
                    new Arguments.Option("report").withValue(),
                    new Arguments.Option("stopOnError"),
                    new Arguments.Option("optionals"),
                    new Arguments.Option("logOnlyFailure"),
                    new Arguments.Option("parallel")});

        } catch (IllegalArgumentException ia) {
            System.err.println(ia.getMessage());
            usage(System.err);
            System.exit(1);
            // Will never been reached:
            return;
        }

        final boolean dry = arguments.options.containsKey("dry");
        final boolean logOnlyFailure = arguments.options.containsKey("logOnlyFailure");
        final boolean parallel = arguments.options.containsKey("parallel");
        final boolean optionals = arguments.options.containsKey("optionals");
        final boolean stopOnError = arguments.options.containsKey("stopOnError");

        if (arguments.options.containsKey("help")) {
            usage(System.out);
            System.exit(0);
        }

        String reportOption = arguments.options.get("report");
        final Path reportFile = (reportOption == null) ? null : Paths.get(reportOption);

        if (arguments.final_args.size() != 1) {
            System.err.println("Wrong number of arguments.");
            usage(System.err);
            System.exit(1);
        }

        Path testDirectory = Paths.get(arguments.final_args.get(0));

        final Path logDirectory = testDirectory.resolve("logs");
        Log.LogPrintStream logPrintStream = null;
        if (!dry) {
            try {
                Files.createDirectories(logDirectory);
                String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
                Path mainLog = logDirectory.resolve("LOG_" + date + ".txt");
                logPrintStream = Log.setLogFile(mainLog, true);
                System.out.println("Logging to: " + mainLog);
            } catch (IOException io) {
                io.printStackTrace();
                Log.error("Failed to initialize logging. %s", io.getMessage());
            }
        }

        Log.info("=== Download and transform tests to %s", testDirectory);
        TestDownloader downloader = new TestDownloader(testDirectory);
        downloader.dry = dry;
        downloader.downloadAndTransform();

        if (dry && reportOption != null) {
            Log.warn("Report file will not be generated due to 'dry' option");
        }

        Log.info("=== Running Tests.%s", reportOption != null ? " Report file: " + reportOption : "");

        java.util.List<Path> scxmlFiles = new ArrayList<>(300);
        try (var dirStream = Files.list(downloader.scxml)) {
            scxmlFiles.addAll(dirStream.filter(path -> "scxml".equalsIgnoreCase(IOTool.getFileExtension(path)))
                    .toList());
        } catch (IOException e) {
            Log.exception("Failed to list scxml files", e);
            System.exit(1);
        }

        if (optionals) {
            try (var dirStream = Files.list(downloader.optionalScxml)) {
                scxmlFiles.addAll(dirStream.filter(path -> "scxml".equalsIgnoreCase(IOTool.getFileExtension(path)))
                        .toList());
            } catch (IOException e) {
                Log.exception("Failed to list optional scxml files", e);
                System.exit(1);
            }
        }

        IncludePaths includePaths = new IncludePaths();
        includePaths.add(downloader.dependenciesScxml);

        final PrintStream log;
        if (logPrintStream != null) {
            logPrintStream.lock();
            log = logPrintStream;
        } else {
            log = Log.getPrintStream();
        }

        try {
            TestSpecification config = Tester.load_test_config(testDirectory.resolve("test_config.json"));

            Stream<Path> s = parallel ? scxmlFiles.parallelStream() : scxmlFiles.stream();

            s.forEach(scxmlFile -> {

                if (stopOnError && failedCount > 0)
                    return;

                ByteArrayOutputStream os = null;
                boolean succeeded = false;
                Path logFile = logDirectory.resolve(scxmlFile.getFileName() + ".log");
                try {
                    os = new ByteArrayOutputStream(10240);
                    Log.setLogStream(new PrintStream(os, false, StandardCharsets.UTF_8));

                    Tester tester = new Tester(config);
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
                    Log.releaseStream();
                    if (os != null) {
                        if (!(logOnlyFailure && succeeded)) {
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
                }
            });

        } catch (Exception e) {
            Log.exception("Failed in Test loop.", e);
            ++failedCount;
        }

        // Ensure console stream are in sync
        System.err.flush();

        log.println("==== Results:");
        log.println(" Tests     " + scxmlFiles.size());
        log.println(" Succeeded " + succeededCount);
        log.println(" Failed    " + failedCount);

        if (logPrintStream != null)
            logPrintStream.close();
        System.exit(failedCount > 0 ? 1 : 0);

    }
}
